package com.cusob.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author 高丁
 */
@Data
@AllArgsConstructor
public class ContactGroupDto implements Serializable {
    Integer groupId;

    String groupName;

    Integer totals;

    Date createTime;

    Date updateTime;

    Double openRate;

    Double clickRate;
}


