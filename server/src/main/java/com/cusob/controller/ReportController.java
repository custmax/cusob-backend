package com.cusob.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cusob.entity.Report;
import com.cusob.result.Result;
import com.cusob.service.ReportService;
import com.cusob.vo.ReportVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("report")
public class ReportController {

    @Autowired
    private ReportService reportService;

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
