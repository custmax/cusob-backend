package com.cusob.vo;

import com.cusob.dto.ContactGroupDto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * @author 高丁
 */
@Data
@AllArgsConstructor
public class ContactGroupVo {
    String key;

    String groupName;

    String totals;

    String creationTime;

    String latestActivity;

    String latestPerformance;

    public ContactGroupVo(ContactGroupDto contactGroupDto) {
        setKey(contactGroupDto.getGroupId());
        setGroupName(contactGroupDto.getGroupName());
        setTotals(contactGroupDto.getTotals());
        setCreationTime(contactGroupDto.getCreateTime());
        setLatestActivity(contactGroupDto.getUpdateTime());
        setLatestPerformance(contactGroupDto.getOpenRate(), contactGroupDto.getClickRate());
    }

    public void setKey(Integer key) {
        this.key = key.toString();
    }

    public void setTotals(Integer totals) {
        this.totals = (totals == null) ? "0" : totals.toString();
    }

    public void setCreationTime(Date creationTime) {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy/MM/dd");
        this.creationTime = sdf.format(creationTime);
    }

    public void setLatestActivity(Date updateTime) {
        LocalDate before = updateTime.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();

        LocalDate now = LocalDate.now();
        this.latestActivity = ChronoUnit.DAYS.between(before, now) + " days ago";
    }

    public void setLatestPerformance(Double openRate, Double clickRate) {
        openRate = (openRate == null) ? 0 : openRate;
        clickRate = (clickRate == null) ? 0 : clickRate;
        this.latestPerformance = "open rate: " + (openRate * 100) + "%; click rate: " + (clickRate * 100) + "%;";
    }
}
