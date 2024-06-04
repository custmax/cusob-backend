package com.cusob.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cusob.auth.AuthContext;
import com.cusob.constant.MqConst;
import com.cusob.dto.CampaignDto;
import com.cusob.dto.CampaignQueryDto;
import com.cusob.entity.*;
import com.cusob.exception.CusobException;
import com.cusob.mapper.CampaignMapper;
import com.cusob.result.ResultCodeEnum;
import com.cusob.service.*;
import com.cusob.vo.CampaignListVo;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class CampaignServiceImpl extends ServiceImpl<CampaignMapper, Campaign> implements CampaignService {

    @Autowired
    private ContactService contactService;

    @Autowired
    private MailService mailService;

    @Autowired
    private SenderService senderService;

    @Autowired
    private CompanyService companyService;

    @Value("${cusob.host}")
    private String baseUrl;

    @Autowired
    private PlanPriceService planPriceService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UnsubscribeService unsubscribeService;

    @Autowired
    private CampaignContactService campaignContactService;

    @Autowired
    private ReportService reportService;

    @Value("${cusob.url}")
    private String host;

    /**
     * save Campaign Draft
     * @param campaignDto
     */
    @Override
    public Long saveCampaign(CampaignDto campaignDto, Integer status) {
        Campaign campaign = new Campaign();
        BeanUtils.copyProperties(campaignDto, campaign);

        campaign.setUserId(AuthContext.getUserId());
        campaign.setStatus(status);
        baseMapper.insert(campaign);
        return campaign.getId();
    }

    /**
     * get Campaign By Id
     * @param id
     * @return
     */
    @Override
    public Campaign getCampaignById(Long id) {
        Campaign campaign = baseMapper.selectById(id);
        return campaign;
    }

    /**
     * update Campaign
     * @param campaignDto
     */
    @Override
    public void updateCampaign(CampaignDto campaignDto) {
        Campaign campaign = new Campaign();
        BeanUtils.copyProperties(campaignDto, campaign);
        campaign.setUserId(AuthContext.getUserId());
        campaign.setStatus(Campaign.DRAFT);
        baseMapper.updateById(campaign);
    }

    /**
     * get Campaign Page
     * @param pageParam
     * @param campaignQueryDto
     * @return
     */
    @Override
    public IPage<CampaignListVo> getCampaignPage(Page<Campaign> pageParam, CampaignQueryDto campaignQueryDto) {
        String name = campaignQueryDto.getName();
        Integer status = campaignQueryDto.getStatus();
        Integer order = campaignQueryDto.getOrder();
        Long userId = AuthContext.getUserId();
        IPage<CampaignListVo> pageModel = baseMapper.getCampaignPage(pageParam, userId, name, status, order);
        return pageModel;
    }

    /**
     * send Email
     * @param campaignDto
     */
    @Override
    public void sendEmail(CampaignDto campaignDto) {
        Company company = companyService.getById(AuthContext.getCompanyId());
        PlanPrice plan = planPriceService.getPlanById(company.getPlanId());
        Long groupId = campaignDto.getToGroup();
        List<Contact> contactList = contactService.getListByGroupId(groupId);
        // Whether the limit of e-mails that can be sent is exceeded
        if (company.getEmails() + contactList.size() >= plan.getEmailCapacity()){
            throw new CusobException(ResultCodeEnum.EMAIL_NUMBER_FULL);
        }
        // Parameter validation
        this.paramVerify(campaignDto);
        Long campaignId;
        if(campaignDto.getId()==0 && this.getCampaignByname(campaignDto.getCampaignName())!=null){
            throw new CusobException(ResultCodeEnum.TITLE_IS_EXISTED);
        }
        Campaign campaign = this.getCampaignById(campaignDto.getId());
        if (campaign != null){
            campaignId = campaign.getId();
        }else {
            campaignId = this.saveCampaign(campaignDto, Campaign.ONGOING);
            campaign = this.getCampaignById(campaignId);
        }

        Report report = new Report();
        report.setUserId(AuthContext.getUserId());
        report.setCompanyId(AuthContext.getCompanyId());
        report.setCampaignId(campaignId);
        report.setGroupId(groupId);
        report.setDeliverCount(contactList.size());
        // Generate reports
        reportService.saveReport(report);

        rabbitTemplate.convertAndSend(MqConst.EXCHANGE_REPORT_DIRECT,
                MqConst.ROUTING_SAVE_REPORT, report);
        // save campaign contact
        rabbitTemplate.convertAndSend(MqConst.EXCHANGE_CAMPAIGN_DIRECT,
                MqConst.ROUTING_CAMPAIGN_CONTACT, report);
        // Email contacts
        rabbitTemplate.convertAndSend(MqConst.EXCHANGE_MAIL_DIRECT,
                MqConst.ROUTING_MASS_MAILING, campaign);
    }

    @Override
    public List<Contact> getSendList(Long groupId) {
        return contactService.getListByGroupId(groupId);
    }

    /**
     * remove Campaign
     * @param id
     */
    @Override
    public void removeCampaign(Long id) {
        baseMapper.deleteById(id);
    }

    /**
     * Mass Mailing
     * @param campaign
     */


    @Override
    public void MassMailing(Campaign campaign) {
        Long senderId = campaign.getSenderId();
        Sender sender = senderService.getById(senderId);
        String senderName = campaign.getSenderName();
        String subject = campaign.getSubject();
        String content = campaign.getContent();
        Long userId = campaign.getUserId();
        Long groupId = campaign.getToGroup();
        List<Contact> contactList = contactService.getListByUserIdAndGroupId(userId, groupId);
        List<String> emailList = unsubscribeService.selectEmailList();

        Random random = new Random();
        long totalTime = 1;
        for (Contact contact : contactList) {
            String email = contact.getEmail();
            if (!emailList.contains(email)){
                String replace = content.replace("#{FIRSTNAME}", contact.getFirstName()==null ? "#{FIRSTNAME}":contact.getFirstName())
                        .replace("#{LASTNAME}", contact.getLastName()==null ? "#{LASTNAME}":contact.getLastName())
                        .replace("#{COMPANY}",contact.getCompany()==null ? "#{COMPANY}":contact.getCompany())
                        .replace("#{EMAIL}",contact.getEmail()==null ? "#{EMAIL}":contact.getEmail())
                        .replace("#{TITLE}",contact.getTitle()==null ? "#{TITLE}":contact.getTitle())
                        .replace("#{BIRTHDATE}",contact.getBirthDate()==null ? "#{BIRTHDATE}":contact.getBirthDate().toString())
                        .replace("#{PHONE}",contact.getBirthDate()==null ? "#{PHONE}":contact.getPhone())
                        .replace("#{DEPT}",contact.getBirthDate()==null ? "#{DEPT}":contact.getDept())
                        //动态替换收件人信息
                        ;
                String img = "<img style=\"display: none;\" src=\"" + baseUrl + "/read/count/"
                        + campaign.getId() + "/" + contact.getId() + "\">";

                String encode = Base64.getEncoder().encodeToString(email.getBytes());
                String unsubscribeUrl = host + "/unsubscribe?email=" + URLEncoder.encode(encode);
                String btnUnsubscribe = "<a href=\"" + unsubscribeUrl +"\">\n" +
                        "    <div style=\"text-align: center; margin-top: 20px;\">\n" +
                        "        <button style=\"border-radius: 4px; height: 30px; color: white; border: none; background-color: #e7e7e7;\">Unsubscribe</button>\n" +
                        "    </div>\n" +
                        "</a>";
                String emailContent = replace + btnUnsubscribe + img;
                ScheduledThreadPoolExecutor executor =
                        new ScheduledThreadPoolExecutor(2, new ThreadPoolExecutor.CallerRunsPolicy());
                executor.schedule(() -> {
                    mailService.sendEmail(sender, senderName, email, emailContent, subject);
                    campaignContactService.updateSendStatus(campaign.getId(), contact.getId());
                    reportService.updateDeliveredCount(campaign.getId());
                }, totalTime, TimeUnit.MILLISECONDS);
                executor.shutdown();
                totalTime += 1000*(random.nextInt(10) + 10);
            }
        }

    }


    /**
     * update Status
     */
    @Override
    public void updateStatus(Long campaignId, Integer status) {
        Campaign campaign = baseMapper.selectById(campaignId);
        if (campaign != null){
            campaign.setStatus(status);
            baseMapper.updateById(campaign);
        }
    }

    @Override
    public Campaign getCampaignByname(String campaignName) {
        return baseMapper.getCampaignByname(campaignName);
    }


    private void paramVerify(CampaignDto campaignDto) {
        if (!StringUtils.hasText(campaignDto.getSenderName())){
            throw new CusobException(ResultCodeEnum.SENDER_NAME_EMPTY);
        }
        if (campaignDto.getSenderId() == null){
            throw new CusobException(ResultCodeEnum.SENDER_IS_EMPTY);
        }
        if (campaignDto.getToGroup()==null){
            throw new CusobException(ResultCodeEnum.RECIPIENT_IS_EMPTY);
        }
        if (!StringUtils.hasText(campaignDto.getSubject())){
            throw new CusobException(ResultCodeEnum.SUBJECT_IS_EMPTY);
        }
        if (!StringUtils.hasText(campaignDto.getContent())){
            throw new CusobException(ResultCodeEnum.CONTENT_IS_EMPTY);
        }
        if (campaignDto.getSendTime() == null){
            throw new CusobException(ResultCodeEnum.SEND_TIME_EMPTY);
        }

    }

}
