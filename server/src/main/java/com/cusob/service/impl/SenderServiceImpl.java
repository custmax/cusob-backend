package com.cusob.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cusob.auth.AuthContext;
import com.cusob.constant.MqConst;
import com.cusob.constant.RedisConst;
import com.cusob.dto.SenderDto;
import com.cusob.entity.Dkim;
import com.cusob.entity.Domain;
import com.cusob.entity.Email;
import com.cusob.entity.EmailSettings;
import com.cusob.entity.Sender;
import com.cusob.exception.CusobException;
import com.cusob.mapper.SenderMapper;
import com.cusob.result.ResultCodeEnum;
import com.cusob.service.DkimService;
import com.cusob.service.DomainService;
import com.cusob.service.MailService;
import com.cusob.service.EmailSettingsService;
import com.cusob.service.SenderService;

import com.cusob.utils.Ports;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SenderServiceImpl extends ServiceImpl<SenderMapper, Sender> implements SenderService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private DomainService domainService;

    @Value("${cusob.domain.dkim.prefix}")
    private String dkimPrefix;

    @Value("${cusob.domain.dkim.selector}")
    private String selectorPrefix;

    @Autowired
    private DkimService dkimService;

    @Autowired
    private EmailSettingsService emailSettingsService;

    /**
     * save Sender
     * @param senderDto
     */
    @Override
    public void saveSender(SenderDto senderDto) {
        // 参数校验
        if (!StringUtils.hasText(senderDto.getEmail())){
            throw new CusobException(ResultCodeEnum.EMAIL_IS_EMPTY);
        }
        if (!StringUtils.hasText(senderDto.getPassword())){
            throw new CusobException(ResultCodeEnum.PASSWORD_IS_EMPTY);
        }
        String email = senderDto.getEmail();
        Sender senderSelect = baseMapper.selectOne(
                new LambdaQueryWrapper<Sender>()
                        .eq(Sender::getEmail, email)
        );

        if (senderSelect!=null ){
            throw new CusobException(ResultCodeEnum.EMAIL_IS_BOUND);
        }
        Sender sender = new Sender();
        BeanUtils.copyProperties(senderDto, sender);


        String suffix = sender.getEmail().split("@")[1];
        EmailSettings settings = emailSettingsService.getSettings(suffix);
        if(settings == null && sender.getSmtpServer()==null){
            throw new CusobException(ResultCodeEnum.EMAIL_CATEGORY_NOEXIST);
        }
        else {
        if(sender.getServerType().equals("IMAP")){
            if(sender.getImapServer()==null){
                sender.setImapServer(settings.getImapServer());
            }
            if(sender.getImapPort()==null){
                if(sender.getImapEncryption()==null){
                    sender.setImapPort(Ports.IMAP_SSL_PORT);
                }else {
                    sender.setImapPort(Ports.IMAP_NOEncryption_PORT);
                }
            }
        }else if(sender.getServerType().equals("POP3")){
            if (sender.getPopServer()==null){
                sender.setPopServer(settings.getPopServer());
            }
            if(sender.getPopPort()==null){
                if(sender.getPopEncryption()==null){
                    sender.setPopPort(Ports.POP_SSL_PORT);
                }else {
                    sender.setPopPort(Ports.POP_NOEncryption_PORT);
                }
            }
        }
        if(sender.getSmtpServer()==null){
            sender.setSmtpServer(settings.getSmtpServer());
        }

        if(sender.getSmtpPort()==null){
            if (sender.getSmtpEncryption().equals("NO")) {
                sender.setSmtpPort(Ports.SMTP_NOEncryption_PORT);
            } else if (sender.getSmtpEncryption().equals("STARTTLS")) {
                sender.setSmtpPort(Ports.SMTP_STARTTLS_PORT);
            } else {
                sender.setSmtpPort(Ports.SMTP_SSL_PORT);
            }
        }
        }
        sender.setUserId(AuthContext.getUserId());
        baseMapper.insert(sender);

        String domain = email.substring(email.lastIndexOf('@') + 1);
        Domain domainSelect = domainService.getByDomain(domain);
        if (domainSelect == null){
            Domain domainSave = new Domain();
            domainSave.setDomain(domain);
            domainSave.setUserId(AuthContext.getUserId());
            domainService.save(domainSave);

            rabbitTemplate.convertAndSend(MqConst.EXCHANGE_DKIM_DIRECT,
                    MqConst.ROUTING_GENERATE_DKIM, domain);
        }

    }

    /**
     * get Sender By UserId
     */
    @Override
    public Sender getSenderByUserId() {
        Long userId = AuthContext.getUserId();
        Sender sender = baseMapper.selectOne(
                new LambdaQueryWrapper<Sender>().eq(Sender::getUserId, userId)
        );
        return sender;
    }

    /**
     * update Sender
     * @param senderDto
     */
    @Override
    public void updateSender(SenderDto senderDto) {
        // 参数校验
        if(!StringUtils.hasText(senderDto.getServerType())){
            throw new CusobException(ResultCodeEnum.SERVER_TYPE_IS_EMPTY);
        }
        if (!StringUtils.hasText(senderDto.getEmail())){
            throw new CusobException(ResultCodeEnum.EMAIL_IS_EMPTY);
        }
        if (!StringUtils.hasText(senderDto.getPassword())){
            throw new CusobException(ResultCodeEnum.PASSWORD_IS_EMPTY);
        }
        Sender sender = new Sender();
        BeanUtils.copyProperties(senderDto, sender);
        sender.setUserId(AuthContext.getUserId());
        baseMapper.updateById(sender);
    }

    /**
     * remove Sender by userId
     */
    @Override
    public void removeSender() {
        baseMapper.delete(
                new LambdaQueryWrapper<Sender>()
                        .eq(Sender::getUserId, AuthContext.getUserId())
        );
    }

    /**
     * send verify code for binding sender
     * @param email
     */
    @Override
    public void sendCodeForSender(String email) {
        if (!StringUtils.hasText(email)){
            throw new CusobException(ResultCodeEnum.EMAIL_IS_EMPTY);
        }
        String code = String.valueOf((int)((Math.random() * 9 + 1) * Math.pow(10,5)));
        String subject = "Cusob Team"; // todo 待优化
        String content = "Hi \n" +
                "You are now in the process of binding a email, and the verification code is as follows \n" +
                code;
        String key = RedisConst.BIND_EMAIL_PREFIX + email;
        // set verify code ttl 10 minutes
        redisTemplate.opsForValue().set(key, code, RedisConst.BIND_EMAIL_TIMEOUT, TimeUnit.MINUTES);
        Email mail = new Email();
        mail.setEmail(email);
        mail.setSubject(subject);
        mail.setContent(content);
        rabbitTemplate.convertAndSend(MqConst.EXCHANGE_SENDER_DIRECT,
                MqConst.ROUTING_BIND_SENDER, mail);
    }

    /**
     * get Sender List
     * @return
     */
    @Override
    public List<Sender> getSenderList() {
        List<Sender> senderList = baseMapper.selectList(
                new LambdaQueryWrapper<Sender>()
                        .eq(Sender::getUserId, AuthContext.getUserId())
        );
        return senderList;
    }


}
