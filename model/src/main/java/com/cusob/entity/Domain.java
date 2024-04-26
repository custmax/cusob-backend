package com.cusob.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("domain")
public class Domain extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("domain")
    private String domain;

    @TableField("spf")
    private Boolean spf;

    @TableField("dkim")
    private Boolean dkim;
}
