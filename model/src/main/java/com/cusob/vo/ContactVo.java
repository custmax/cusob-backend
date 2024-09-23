package com.cusob.vo;

import com.cusob.dto.ContactDto;
import lombok.Data;

import java.io.Serializable;

@Data
public class ContactVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private int isAvailable;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private String company;

    private String groupName;

    private String subscriptionType;

    public ContactVo(ContactDto contactDto) {
        setId(contactDto.getId());
        setEmail(contactDto.getEmail());
        setCompany(contactDto.getCompany());
        setPhone(contactDto.getPhone());
        setFirstName(contactDto.getFirstName());
        setLastName(contactDto.getLastName());
        setGroupName(contactDto.getGroupName());
        setSubscriptionType(contactDto.getSubscriptionType());
    }

}
