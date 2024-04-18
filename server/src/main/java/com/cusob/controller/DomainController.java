package com.cusob.controller;

import com.cusob.result.Result;
import com.cusob.service.DomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/domain")
public class DomainController {

    @Autowired
    private DomainService domainService;

    @PostMapping("verify")
    public Result verify(String email){
        Map<String, Boolean> map = domainService.domainVerify(email);
        return Result.ok(map);
    }
}
