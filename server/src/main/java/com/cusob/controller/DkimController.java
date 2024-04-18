package com.cusob.controller;

import com.cusob.result.Result;
import com.cusob.service.DkimService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dkim")
public class DkimController {

    @Autowired
    private DkimService dkimService;

    @ApiOperation("save Dkim")
    @PostMapping("save")
    public Result save(String domain){
        dkimService.saveDkim(domain);
        return Result.ok();
    }

    @ApiOperation("get publicKey by domain")
    @GetMapping("getPublicKey")
    public Result getPublicKey(String domain){
        String publicKey = dkimService.getPublicKey(domain);
        return Result.ok(publicKey);
    }

}
