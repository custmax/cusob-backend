package com.cusob.controller;

import com.cusob.entity.Contact;
import com.cusob.result.Result;
import com.cusob.service.ContactService;
import com.cusob.service.UnsubscribeService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/unsubscribe")
public class UnsubscribeController {

    @Autowired
    private ContactService contactService;

    @ApiOperation("Unsubscribe")
    @GetMapping("campaign")
    public Result Unsubscribe(String email){
        contactService.saveUnsubsribedEmail(email);
        return Result.ok();
    }
    @ApiOperation("Subscribe again")
    @GetMapping("subscribe")
    public Result subscribe(String email){
        contactService.saveSubsribedEmail(email);
        return Result.ok();
    }
}
