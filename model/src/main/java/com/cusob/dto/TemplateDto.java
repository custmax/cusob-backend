package com.cusob.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class TemplateDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String subject;

    private String folder;

    private Integer type;

    private String content;

    @TableField("designContent")
    private String designContent;

}
