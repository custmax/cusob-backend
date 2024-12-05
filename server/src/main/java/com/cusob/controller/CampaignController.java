package com.cusob.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cusob.auth.AuthContext;
import com.cusob.dto.CampaignDto;
import com.cusob.dto.CampaignQueryDto;
import com.cusob.entity.Campaign;
import com.cusob.entity.Contact;
import com.cusob.entity.Sender;
import com.cusob.result.Result;
import com.cusob.result.ResultCodeEnum;
import com.cusob.service.CampaignService;
import com.cusob.service.SenderService;
import com.cusob.vo.CampaignListVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/campaign")
public class CampaignController
{

    @Autowired
    private CampaignService campaignService;
    @Autowired
    private SenderService senderService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @ApiOperation("save Campaign Draft")
    @PostMapping("saveDraft")
    public Result saveDraft(@RequestBody CampaignDto campaignDto)
    {
        System.out.println("Received designContent: " + campaignDto.getDesignContent());

        campaignService.saveCampaign(campaignDto, Campaign.DRAFT);
        return Result.ok();
    }

    @ApiOperation("get Campaign By Id")
    @GetMapping("get/{id}")
    public Result getCampaignById(@PathVariable Long id)
    {
        Campaign campaign = campaignService.getCampaignById(id);
        return Result.ok(campaign);
    }

    @ApiOperation("update Campaign")
    @PostMapping("update")
    public Result updateCampaign(@RequestBody CampaignDto campaignDto)
    {
        campaignService.updateCampaign(campaignDto);
        return Result.ok();
    }

    @ApiOperation("get Campaign Page")
    @GetMapping("getPage/{limit}/{page}")
    public Result getCampaignPage(@PathVariable Long limit,
                                  @PathVariable Long page,
                                  CampaignQueryDto campaignQueryDto)
    {
        Page<Campaign> pageParam = new Page<>(page, limit);
        IPage<CampaignListVo> pageVo = campaignService.getCampaignPage(pageParam, campaignQueryDto);
        return Result.ok(pageVo);
    }

    @ApiOperation("send Email")
    @PostMapping("sendEmail")
    public Result sendEmail(@RequestBody CampaignDto campaignDto)
    {
        System.out.println("\n\n\n");
        System.out.println("campaignDto" + campaignDto);
        System.out.println("\n\n\n");
        //获取userId和email，然后拼接成key，从redis中获取
        Long senderId = campaignDto.getSenderId();
        Sender sender = senderService.getById(senderId);
        String email=sender.getEmail();
        Long userId=sender.getUserId();
        String key=userId+"_"+email;
        String value=stringRedisTemplate.opsForValue().get(key);//当前额度
        //若value为空则设置为200
        if(value==null){
            value="200";
        }
        Long groupId=campaignDto.getToGroup();
        int count=campaignService.getSendList(groupId).size();
        //若value为空或value对应的数字大于0则发送邮件，然后value对应的值减去groupid内的人数，然后重新保存至redis
        if(Integer.parseInt(value)>=count) {

            int newValue=Integer.parseInt(value)-count;
            //将新的value保存至redis，设置过期时间为第二天的零点
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            long expireSeconds = (calendar.getTimeInMillis() - System.currentTimeMillis()) / 1000;

            // 设置键值对，并设置过期时间
            stringRedisTemplate.opsForValue().set(
                    key,
                    String.valueOf(newValue),
                    expireSeconds,
                    TimeUnit.SECONDS
            );
            campaignService.sendEmail(campaignDto);
            return Result.ok();
        }
        else
        {
            int restValue = Integer.parseInt(value);
            System.out.println("在fail里返回的restValue: " + restValue);
            return Result.fail(restValue);
        }
    }

    @ApiOperation("Email list")
    @GetMapping("emailList/{groupId}")
    public Result EmailList(@PathVariable long groupId)
    {
        List<Contact> sendList = campaignService.getSendList(groupId);
        return Result.ok(sendList);
    }

    @ApiOperation("Get SenderName By CampaignName")
    @GetMapping("getSenderName/{campaignName}")
    public Result EmailList(@PathVariable String campaignName)
    {
        Long userId= AuthContext.getUserId();
        return Result.ok(campaignService.getCampaignByName(campaignName,userId).getSenderName());
    }

    @ApiOperation("remove Campaign")
    @DeleteMapping("remove/{id}")
    public Result removeCampaign(@PathVariable Long id)
    {
        campaignService.removeCampaign(id);
        return Result.ok();
    }

    @ApiOperation("getCampaign By CampaignName")
    @GetMapping("/getCampaignByName/{name}")
    public Result getCampaignByName(@PathVariable("name") String campaignName) {
        System.out.println("campaignName : " + campaignName);
        Campaign campaign = campaignService.getCampaignByName(campaignName,AuthContext.getUserId());
        if (campaign != null) {
            return Result.fail(ResultCodeEnum.CAMPAIGN_NAME_EXISTS_FAIL.getMessage());
        }
        else {
            return Result.ok();
        }
    }
}
