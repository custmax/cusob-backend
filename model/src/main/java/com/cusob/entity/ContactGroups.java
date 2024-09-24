package com.cusob.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author 高丁
 */
@TableName("contact_groups")
@Data
public class ContactGroups extends BaseEntity{
    @TableField("user_id")
    private Long userId;

    @TableField("group_name")
    private Long groupName;
}
