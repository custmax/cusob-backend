package com.cusob.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dkim")
public class Dkim extends BaseEntity{

    @TableField("domain")
    private String domain;

    @TableField("selector")
    private String selector;

    @TableField("private_key")
    private String privateKey;

    @TableField("public_key")
    private String publicKey;
}
