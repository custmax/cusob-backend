package com.cusob.controller;

import com.cusob.result.Result;
import com.cusob.service.CampaignContactService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/read")
public class ReadCountController {

    @Autowired
    private CampaignContactService campaignContactService;

    @ApiOperation("Read Count")
    @GetMapping("count/{campaignId}/{contactId}")
    public Result ReadCount(@PathVariable Long campaignId,
                            @PathVariable Long contactId){
        campaignContactService.opened(campaignId, contactId);
        return Result.ok();
    }
}
