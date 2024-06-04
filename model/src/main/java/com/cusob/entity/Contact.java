package com.cusob.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@TableName("contact")
@Data
public class Contact extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("company_id")
    private Long companyId;

    @TableField("group_id")
    private Long groupId;

    @TableField("valid")
    private int valid;

    @ExcelProperty(index = 0)
    @TableField("first_name")
    private String firstName;

    @ExcelProperty(index = 1)
    @TableField("last_name")
    private String lastName;

    @ExcelProperty(index = 2)
    @TableField("email")
    private String email;

    @ExcelProperty(index = 3)
    @TableField("mobile")
    private String mobile;

    @ExcelProperty(index = 4)
    @TableField("phone")
    private String phone;

    @ExcelProperty(index = 5)
    @TableField("country")
    private String country;

    @ExcelProperty(index = 6)
    @TableField("company")
    private String company;

    @ExcelProperty(index = 7)
    @TableField("dept")
    private String dept;

    @ExcelProperty(index = 8)
    @TableField("title")
    private String title;

    @TableField("avatar")
    private String avatar;

    @ExcelProperty(index = 9)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("birthdate")
    private Date birthDate;

    @ExcelProperty(index = 10)
    @TableField("note")
    private String note;
}
