package com.cusob.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cusob.entity.Report;
import com.cusob.vo.ReportVo;


public interface ReportService extends IService<Report> {

    /**
     * save Report
     */
    void saveReport(Report report);

    /**
     * get Report Page
     * @param pageParam
     * @param keyword
     * @return
     */
    IPage<ReportVo> selectPage(Page<Report> pageParam, String keyword);

    /**
     * Read Count
     * @param campaignId
     */
    void opened(Long campaignId);

    /**
     * update DeliveredCount
     * @param campaignId
     */
    void updateDeliveredCount(Long campaignId);
}
