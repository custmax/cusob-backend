package com.cusob.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.auth.In;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.util.*;
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

    @Value("${cusob.imghost}")
    private String baseUrl;

    @Autowired
    private PlanPriceService planPriceService;
    @Autowired
    private PriceService priceService;
    @Resource
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UnsubscribeService unsubscribeService;

    @Autowired
    private CampaignContactService campaignContactService;

    @Autowired
    private AccountInfoService accountInfoService;

    @Autowired
    private ReportService reportService;

    @Value("${cusob.url}")
    private String host;

    /**
     * save Campaign Draft
     * @param campaignDto
     */
    @Override
    public Long saveCampaign(CampaignDto campaignDto, Integer status) {//保存活动
        Campaign campaign = new Campaign();
        BeanUtils.copyProperties(campaignDto, campaign);
        if (campaignDto.getDesignContent() != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonContent = objectMapper.writeValueAsString(campaignDto.getDesignContent());
                campaign.setDesignContent(jsonContent);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize designContent to JSON", e);
            }
        }
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

        // 将 designContent 转换为 JSON 字符串
        if (campaignDto.getDesignContent() != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonContent = objectMapper.writeValueAsString(campaignDto.getDesignContent());
                campaign.setDesignContent(jsonContent);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize designContent to JSON", e);
            }
        }

        // 设置其他字段
        campaign.setUserId(AuthContext.getUserId());
        campaign.setStatus(Campaign.DRAFT);

        // 执行更新
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
        //Integer order1 = order == null ? 0 : order;
        //Page<Campaign> pageParam1=new Page<>(pageParam.getCurrent(),pageParam.getSize());
        QueryWrapper<Campaign> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", 0)
                .eq("user_id", userId)
                .like(!StringUtils.isEmpty(name), "campaign_name", name)  // 只在有关键字时加上 LIKE 条件
                .eq(status != null, "status", status)
                .orderByDesc(order == 0 ? "create_time" : "update_time");
        // 执行分页查询，返回 Page<Campaign> 类型
        IPage<Campaign> campaignPage = this.page(pageParam, queryWrapper);

        // 将查询到的 Page<Campaign> 转换为 Page<CampaignListVo>
        IPage<CampaignListVo> pageModel = campaignPage.convert(campaign -> {
            // 在这里将 Campaign 转换为 CampaignListVo
            CampaignListVo vo = new CampaignListVo();
            vo.setId(campaign.getId());
            vo.setCampaignName(campaign.getCampaignName());
            vo.setStatus(campaign.getStatus());
            vo.setUpdateTime(campaign.getUpdateTime());
            // 继续根据需要设置其他字段
            return vo;
        });


        return pageModel;
    }

    /**
     * send Email
     * @param campaignDto
     */
    @Override
    public void sendEmail(CampaignDto campaignDto) {//发送邮件，重要功能
        Company company = companyService.getById(AuthContext.getCompanyId());//获取公司信息
        Price plan = priceService.getPlanById(company.getPlanId());//获取订阅计划信息
        Long groupId = campaignDto.getToGroup();

        List<Contact> contactList = contactService.getListByGroupId(groupId);//获取该campaign要发送的组的联系人列表
        // Whether the limit of e-mails that can be sent is exceeded
        if (company.getEmails() + contactList.size() >= plan.getEmailCapacity()){//超出发送数量
            throw new CusobException(ResultCodeEnum.EMAIL_NUMBER_FULL);//邮件数量已满
        }
        // Parameter validation
        this.paramVerify(campaignDto);//参数验证
        Long campaignId;
        if(campaignDto.getId()==0 && this.getCampaignByName(campaignDto.getCampaignName(),AuthContext.getUserId())!=null){//判断活动名称是否存在
            throw new CusobException(ResultCodeEnum.TITLE_IS_EXISTED);//标题已存在
        }
        Campaign campaign = this.getCampaignById(campaignDto.getId());//获取活动
        if (campaign != null){//活动存在
            campaignId = campaign.getId();
            BeanUtils.copyProperties(campaignDto, campaign);
            campaign.setUserId(AuthContext.getUserId());
            campaign.setStatus(Campaign.ONGOING);
            baseMapper.updateById(campaign);
        }else {//活动不存在
            campaignId = this.saveCampaign(campaignDto, Campaign.ONGOING);//保存活动
            campaign = this.getCampaignById(campaignId);//获取活动
        }

        Report report = new Report();//报告
        report.setUserId(AuthContext.getUserId());//用户id
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
                MqConst.ROUTING_CAMPAIGN_CONTACT, report);//
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
    public void MassMailing(Campaign campaign) {//正式批量发送邮件
        String campaignName = campaign.getCampaignName();
        System.out.println("当前用户id："+AuthContext.getUserId());
        Campaign campaign1 = baseMapper.selectOne(new LambdaQueryWrapper<Campaign>()
                .eq(Campaign::getUserId, AuthContext.getUserId())
                .eq(Campaign::getCampaignName, campaignName)
        );
        if(campaign1 != null){
            campaign.setCampaignName(campaignName + System.currentTimeMillis());
        }
        Long senderId = campaign.getSenderId();
        Sender sender = senderService.getById(senderId);
        String senderName = campaign.getSenderName();
        String subject = campaign.getSubject();
        String content = campaign.getContent();
        Long userId = campaign.getUserId();
        Long groupId = campaign.getToGroup();
        List<Contact> contactList = contactService.getListByUserIdAndGroupId(userId, groupId);
        //List<String> emailList = unsubscribeService.selectEmailList();//获取退订列表

        Random random = new Random();
        long totalTime = 1;
        for (Contact contact : contactList) {
            String email = contact.getEmail();
            if (!contact.getSubscriptionType().equals("Unsubscribed") && contact.getValid() == 1 && contact.getIsAvailable() == 1){ //判断是否退订、邮箱是否有效、是否可用
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

                //String addr = accountInfoService.getAddr(userId);
                String style = "        <style>.footer {\n" +
                        "            padding-top: 0; \n" +
                        "            padding-bottom: 20px; \n" +
                        "            text-align: center;\n" +
                        "            font-size: 14px;\n" +
                        "            color: #999;\n" +
                        "        }\n" +
                        "        .footer p {\n" +
                        "            margin: 5px 0;\n" +
                        "        }</style>";
                String url=baseUrl + "/read/count/" + campaign.getId() + "/" + contact.getId();
                //String img = "<img  src=\""+ "https://gitee.com/jadeinrain/athena-blog-img/raw/f9e39c2309398832ea5083fd03274c8fa13d9552/athena.JPG\" "+ "alt=\"\">";
                String img = "<img  src=\""+ url+ "\" alt=\"\">";
                System.out.println("img: " + img);

//                String encode = Base64.getEncoder().encodeToString(email.getBytes());
                String unsubscribeUrl = host + "/unsubscribe?email=" + email;
                String btnUnsubscribe = "<a href=\"" + unsubscribeUrl +"\">\n" +
                        "    <div style=\"text-align: center; margin-top: 20px;\">\n" +
                        "        <button style=\"border-radius: 45px; height: 30px; color: white; border: none; background-color: #e7e7e7;\">Unsubscribe</button>\n" +
                        "    </div>\n" +
                        "</a>"; //加入退订链接
                AccountInfo accountInfo = accountInfoService.getByUserId(userId);
                StringBuilder downContent = new StringBuilder();

                try {
                    // 检查并拼接公司信息
                    String comp = accountInfo.getCompany();
                    if (comp != null && !comp.isEmpty()) {
                        downContent.append("    <div class=\"footer\">\n")
                                .append("        <p>").append(comp).append("</p>\n")
                                .append("    </div>");
                    }

                    // 检查并拼接地址信息
                    String addressLine1 = accountInfo.getAddressLine1();
                    String addressLine2 = accountInfo.getAddressLine2();
                    String addr = ((addressLine1 != null) ? addressLine1 : "") +
                            ((addressLine2 != null) ? " " + addressLine2 : "");
                    if (!addr.trim().isEmpty()) {
                        downContent.append("    <div class=\"footer\">\n")
                                .append("        <p>").append(addr).append("</p>\n")
                                .append("    </div>");
                    }

                    // 检查并拼接城市和国家信息
                    String city = accountInfo.getCity();
                    String country = accountInfo.getCountry();
                    String cityAndCou = ((city != null) ? city : "") +
                            ((country != null) ? " " + country : "");
                    if (!cityAndCou.trim().isEmpty()) {
                        downContent.append("    <div class=\"footer\">\n")
                                .append("        <p>").append(cityAndCou).append("</p>\n")
                                .append("    </div>");
                    }

                    // 检查并拼接邮政编码
                    String zip = accountInfo.getZipCode();
                    if (zip != null && !zip.isEmpty()) {
                        downContent.append("    <div class=\"footer\">\n")
                                .append("        <p>").append(zip).append("</p>\n")
                                .append("    </div>");
                    }

                    // 检查并拼接电话号码
                    String phon = accountInfo.getPhone();
                    if (phon != null && !phon.isEmpty()) {
                        downContent.append("    <div class=\"footer\">\n")
                                .append("        <p>").append(phon).append("</p>\n")
                                .append("    </div>");
                    }
                } catch (NullPointerException e) {
                    // 如果 accountInfo 或某些字段为 null，则不执行操作
                    System.err.println("AccountInfo contains null fields: " + e.getMessage());
                }

// 最终生成的 HTML 内容
                String finalDownContent = downContent.toString();

                String emailContent = style + replace + btnUnsubscribe + finalDownContent + img ;
                String listUnsubscribe = "<" + unsubscribeUrl + ">, <mailto:" + sender.getEmail() + "?subject=Unsubscribe>";
                ScheduledThreadPoolExecutor executor =
                        new ScheduledThreadPoolExecutor(2, new ThreadPoolExecutor.CallerRunsPolicy());
                executor.schedule(() -> {//定时发送邮件
                    mailService.sendEmail(sender, senderName, email, emailContent, subject,unsubscribeUrl,groupId,campaign.getId());
                    campaignContactService.updateSendStatus(campaign.getId(), contact.getId());//更新campaign_contact表中发送状态,设置为已发送
                    reportService.updateDeliveredCount(campaign.getId());//更新report表中发送数量
                }, totalTime, TimeUnit.MILLISECONDS);//定时发送
                executor.shutdown();
                totalTime += 1000*(random.nextInt(10) + 10);//随机时间间隔

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
    public Campaign getCampaignByName(String campaignName,Long userId) {
        return baseMapper.getCampaignByName(campaignName, userId);
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
