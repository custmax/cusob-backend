package com.cusob.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("email_settings")
@Data
public class EmailSettings {
    @TableField("name_suffix")
    private String nameSuffix;
    @TableField("smtp_server")
    private String smtpServer;
    @TableField("smtp_port")
    private int smtpPort;
    @TableField("imap_server")
    private String imapServer;
    @TableField("imap_port")
    private int imapPort;

}
