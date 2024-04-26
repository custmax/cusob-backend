package com.cusob.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CampaignQueryDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private Integer status;
}
