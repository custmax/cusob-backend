package com.cusob.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cusob.entity.Report;
import com.cusob.result.Result;
import com.cusob.result.ResultCodeEnum;
import com.cusob.service.ReportService;
import com.cusob.vo.ReportVo;
import io.swagger.annotations.ApiOperation;
import lombok.val;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static spark.Spark.redirect;


@RestController
@RequestMapping("report")
public class ReportController {

    @Autowired
    private ReportService reportService;


    @ApiOperation("get report")
    @GetMapping("{campaignId}/{url}" )
    public Result getReport(@PathVariable("campaignId") Long campaignId,
                                          @PathVariable("url") String url)  {
        reportService.clicked(campaignId);
        return Result.build(url, ResultCodeEnum.REDRICT_SUCCESS);
    }


    @ApiOperation("get Report Page")
    @GetMapping("getPage/{page}/{limit}")
    public Result getReportPage(@PathVariable Long page,
                                @PathVariable Long limit,
                                String keyword){
        Page<Report> pageParam = new Page<>(page, limit);
        IPage<ReportVo> reportVoList = reportService.selectPage(pageParam, keyword);
        return Result.ok(reportVoList);
    }

    @ApiOperation("remove Report")
    @DeleteMapping("remove/{id}")
    public Result removeReport(@PathVariable Long id){
        reportService.removeReport(id);
        return Result.ok();
    }

}
