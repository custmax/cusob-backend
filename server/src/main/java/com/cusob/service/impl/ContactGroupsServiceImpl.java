package com.cusob.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cusob.entity.Campaign;
import com.cusob.entity.ContactGroups;
import com.cusob.mapper.CampaignMapper;
import com.cusob.mapper.ContactGroupsMapper;
import com.cusob.service.ContactGroupsService;
import org.springframework.stereotype.Service;

/**
 * @author 高丁
 */
@Service
public class ContactGroupsServiceImpl extends ServiceImpl<ContactGroupsMapper, ContactGroups> implements ContactGroupsService {
}
