package com.cusob.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ContactDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String mobile;

    private String phone;

    private String country;

    private String company;

    private String dept;

    private String title;

    private String avatar;

    private int isAvailable;

    private Date birthDate;

    private String groupName;

    private String note;

    private String subscriptionType;
    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = (subscriptionType == null) ? "未设置" : subscriptionType;
    }

    public void setPhone(String phone) {
        this.phone = (phone == null) ? "未填写" : phone;
    }
}
