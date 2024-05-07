package com.cusob.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cusob.entity.CampaignContact;
import com.cusob.entity.Contact;
import com.cusob.mapper.CampaignContactMapper;
import com.cusob.service.CampaignContactService;
import com.cusob.service.ContactService;
import com.cusob.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CampaignContactServiceImpl
        extends ServiceImpl<CampaignContactMapper, CampaignContact>
        implements CampaignContactService {

    @Autowired
    private ContactService contactService;

    @Autowired
    private ReportService reportService;

    /**
     * Read Count
     * @param campaignId
     * @param contactId
     */
    @Transactional
    @Override
    public void opened(Long campaignId, Long contactId) {
        CampaignContact contact = baseMapper.selectOne(
                new LambdaQueryWrapper<CampaignContact>()
                        .eq(CampaignContact::getCampaignId, campaignId)
                        .eq(CampaignContact::getContactId, contactId)
        );
        if (contact != null){
            if (!contact.getIsOpened().equals(CampaignContact.OPENED)){
                contact.setIsOpened(CampaignContact.OPENED);
                baseMapper.updateById(contact);
                reportService.opened(campaignId);
            }
        }

    }

    /**
     * batch Save Contact
     * @param campaignId
     * @param groupId
     */
    @Override
    public void batchSaveContact(Long userId, Long campaignId, Long groupId) {
        List<Contact> contactList = contactService.getListByUserIdAndGroupId(userId, groupId);
        List<CampaignContact> campaignContactList = new ArrayList<>();
        for (Contact contact : contactList) {
            CampaignContact campaignContact = new CampaignContact();
            campaignContact.setCampaignId(campaignId);
            campaignContact.setContactId(contact.getId());
            campaignContactList.add(campaignContact);
        }
        // todo 待优化
        campaignContactList.forEach(campaignContact -> baseMapper.insert(campaignContact));
    }

    /**
     * update Send Status
     * @param campaignId
     * @param contactId
     */
    @Override
    public void updateSendStatus(Long campaignId, Long contactId) {
        CampaignContact campaignContact = baseMapper.selectOne(
                new LambdaQueryWrapper<CampaignContact>()
                        .eq(CampaignContact::getCampaignId, campaignId)
                        .eq(CampaignContact::getContactId, contactId)
        );
        if (campaignContact != null){
            campaignContact.setIsDelivered(CampaignContact.DELIVERED);
            baseMapper.updateById(campaignContact);
        }
    }
}
