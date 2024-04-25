package com.cusob.controller;

import com.cusob.dto.SenderDto;
import com.cusob.entity.Sender;
import com.cusob.result.Result;
import com.cusob.service.EmailSettingsService;
import com.cusob.service.SenderService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sender")
public class SenderController {

    @Autowired
    private SenderService senderService;


    @ApiOperation("save Sender")
    @PostMapping("save")
    public Result saveSender(@RequestBody SenderDto senderDto){
        senderService.saveSender(senderDto);
        return Result.ok();
    }

//    @ApiOperation("get Sender By UserId")
//    @GetMapping("getByUserId")
//    public Result getSenderByUserId(){
//        Sender sender = senderService.getSenderByUserId();
//        return Result.ok(sender);
//    }

    @ApiOperation("update Sender")
    @PostMapping("update")
    public Result updateSender(@RequestBody SenderDto senderDto){
        senderService.updateSender(senderDto);
        return Result.ok();
    }

    @ApiOperation("remove Sender")
    @DeleteMapping("remove")
    public Result removeSender(){
        senderService.removeSender();
        return Result.ok();
    }

    @ApiOperation("send verify code for binding sender")
    @PostMapping("sendCodeForSender")
    public Result sendCodeForSender(String email){
        senderService.sendCodeForSender(email);
        return Result.ok();
    }

    @ApiOperation("get Sender List")
    @GetMapping("getList")
    public Result getSenderList(){
        List<Sender> senderList = senderService.getSenderList();
        return Result.ok(senderList);
    }
}
