package com.cusob.receiver;

import com.cusob.constant.MqConst;
import com.cusob.dto.CampaignDto;
import com.cusob.entity.Campaign;
import com.cusob.entity.Report;
import com.cusob.service.CampaignContactService;
import com.cusob.service.CampaignService;
import com.cusob.service.CompanyService;
import com.cusob.service.ReportService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class CampaignReceiver {

    @Autowired
    private ReportService reportService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CampaignContactService campaignContactService;

    @Autowired
    private CampaignService campaignService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SAVE_REPORT, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_REPORT_DIRECT),
            key = {MqConst.ROUTING_SAVE_REPORT}
    ))
    public void generateReports(Report report, Message message, Channel channel) throws IOException {
        if (report!=null){
//            reportService.saveReport(report);
            companyService.updateEmails(report.getCompanyId(), report.getDeliverCount());
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_CAMPAIGN_CONTACT, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_CAMPAIGN_DIRECT),
            key = {MqConst.ROUTING_CAMPAIGN_CONTACT}
    ))
    public void saveCampaignContact(Report report, Message message, Channel channel) throws IOException {
        if (report!=null){
            System.out.println("save Campaign Contact");
            campaignContactService.batchSaveContact(report.getUserId(), report.getCampaignId(), report.getGroupId());
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_MASS_MAILING, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_MAIL_DIRECT),
            key = {MqConst.ROUTING_MASS_MAILING}
    ))
    public void massMailing(Campaign campaign, Message message, Channel channel) throws IOException {
        try {
            if (campaign!=null){
                Date sendTime = campaign.getSendTime();
                Date now = new Date();
                if (now.after(sendTime)){
                    campaignService.MassMailing(campaign);
                    campaignService.updateStatus(campaign.getId(), Campaign.COMPLETED);
                }else {
                    long delay = sendTime.getTime() - now.getTime();
                    ScheduledThreadPoolExecutor executor =
                            new ScheduledThreadPoolExecutor(2, new ThreadPoolExecutor.CallerRunsPolicy());
                    executor.schedule(() -> {
                        campaignService.MassMailing(campaign);
                        campaignService.updateStatus(campaign.getId(), Campaign.COMPLETED);
                    }, delay, TimeUnit.MILLISECONDS);
                    executor.shutdown();
                }

            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }

    }


}
