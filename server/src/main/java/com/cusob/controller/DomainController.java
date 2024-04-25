package com.cusob.controller;

import com.cusob.result.Result;
import com.cusob.service.DomainService;
import com.cusob.vo.DomainListVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/domain")
public class DomainController {

    @Autowired
    private DomainService domainService;

    @ApiOperation("verify domain")
    @PostMapping("verify")
    public Result verify(String domain){
        Map<String, Boolean> map = domainService.domainVerify(domain);
        return Result.ok(map);
    }

    @ApiOperation("get DomainList")
    @GetMapping("getList")
    public Result getDomainList(){
        List<DomainListVo> list = domainService.getDomainList();
        return Result.ok(list);
    }

    @ApiOperation("remove Domain")
    @DeleteMapping("remove")
    public Result removeDomain(Long id){
        domainService.removeById(id);
        return Result.ok();
    }
}
