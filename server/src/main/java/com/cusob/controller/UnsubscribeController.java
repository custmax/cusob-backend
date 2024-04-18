package com.cusob.controller;

import com.cusob.result.Result;
import com.cusob.service.UnsubscribeService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.util.Base64;

@RestController
@RequestMapping("/unsubscribe")
public class UnsubscribeController {

    @Autowired
    private UnsubscribeService unsubscribeService;

    @ApiOperation("Unsubscribe")
    @GetMapping("campaign")
    public Result Unsubscribe(String email){
        byte[] decode = Base64.getDecoder().decode(URLDecoder.decode(email));
        String emailUnsubscribe = new String(decode);
        unsubscribeService.saveEmail(emailUnsubscribe);
        return Result.ok();
    }
}
