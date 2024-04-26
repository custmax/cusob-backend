package com.cusob.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cusob.auth.AuthContext;
import com.cusob.constant.MqConst;
import com.cusob.constant.RedisConst;
import com.cusob.dto.SenderDto;
import com.cusob.entity.Email;
import com.cusob.entity.EmailSettings;
import com.cusob.entity.Sender;
import com.cusob.exception.CusobException;
import com.cusob.mapper.SenderMapper;
import com.cusob.result.ResultCodeEnum;
import com.cusob.service.EmailSettingsService;
import com.cusob.service.SenderService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SenderServiceImpl extends ServiceImpl<SenderMapper, Sender> implements SenderService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private EmailSettingsService emailSettingsService;
    /**
     * save Sender
     * @param senderDto
     */
    @Override
    public void saveSender(SenderDto senderDto) {
        // 参数校验
//        if(!StringUtils.hasText(senderDto.getServerType())){
//            throw new CusobException(ResultCodeEnum.SERVER_TYPE_IS_EMPTY);
//        }
        if (!StringUtils.hasText(senderDto.getEmail())){
            throw new CusobException(ResultCodeEnum.EMAIL_IS_EMPTY);
        }
        if (!StringUtils.hasText(senderDto.getPassword())){
            throw new CusobException(ResultCodeEnum.PASSWORD_IS_EMPTY);
        }
        // todo 其他参数校验
        Sender sender = new Sender();

        BeanUtils.copyProperties(senderDto, sender);

        String suffix = sender.getEmail().split("@")[1];
        EmailSettings settings = emailSettingsService.getSettings(suffix);


        if(sender.getImapServer()==null){
            sender.setImapServer(settings.getImapServer());
        }
        if(sender.getImapPort()==null){
            sender.setImapPort(settings.getImapPort());
        }
        if(sender.getSmtpServer()==null){
            sender.setSmtpServer(settings.getSmtpServer());
        }
        if(sender.getSmtpPort()==null){
            sender.setSmtpPort(settings.getSmtpPort());
        }
        sender.setUserId(AuthContext.getUserId());
        baseMapper.insert(sender);
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
