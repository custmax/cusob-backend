package com.cusob.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cusob.auth.AuthContext;
import com.cusob.dto.ContactDto;
import com.cusob.dto.ContactQueryDto;
import com.cusob.dto.GroupDto;
import com.cusob.entity.*;
import com.cusob.exception.CusobException;
import com.cusob.mapper.ContactMapper;
import com.cusob.result.ResultCodeEnum;
import com.cusob.service.*;
import com.cusob.vo.ContactVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class ContactServiceImpl extends ServiceImpl<ContactMapper, Contact> implements ContactService {

    @Autowired
    private GroupService groupService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private PlanPriceService planPriceService;

    @Autowired
    private UserService userService;

    /**
     * add Contact
     * @param contactDto
     */
    @Override
    @Transactional
    public void addContact(ContactDto contactDto) {
        // contacts count
        Long companyId = AuthContext.getCompanyId();
        Integer count = this.selectCountByCompanyId(companyId);
        Company company = companyService.getById(companyId);
        PlanPrice plan = planPriceService.getPlanById(company.getPlanId());
        if (count >= plan.getContactCapacity()){
            throw new CusobException(ResultCodeEnum.CONTACT_NUMBER_FULL);
        }

        // 参数校验
        this.paramVerify(contactDto);
        Contact contact = new Contact();
        String groupName = contactDto.getGroupName();
        // The group name is not empty
        if (StringUtils.hasText(groupName)){
            Group group = groupService.getGroupByName(groupName);
            // The group doesn't exist，create group
            if (group==null){
                GroupDto groupDto = new GroupDto();
                groupDto.setGroupName(groupName);
                Long groupId = groupService.addGroup(groupDto);
                contact.setGroupId(groupId);
            }else {
                // The group already exists
                contact.setGroupId(group.getId());
            }
        }

        BeanUtils.copyProperties(contactDto, contact);
        contact.setUserId(AuthContext.getUserId());
        contact.setCompanyId(AuthContext.getCompanyId());
        baseMapper.insert(contact);
    }

    private void paramVerify(ContactDto contactDto) {
        // todo maybe need other params to verify
        if (!StringUtils.hasText(contactDto.getFirstName())){
            throw new CusobException(ResultCodeEnum.FIRST_NAME_IS_EMPTY);
        }
        if (!StringUtils.hasText(contactDto.getLastName())){
            throw new CusobException(ResultCodeEnum.LAST_NAME_IS_EMPTY);
        }
        if (!StringUtils.hasText(contactDto.getEmail())){
            throw new CusobException(ResultCodeEnum.EMAIL_IS_EMPTY);
        }
    }

    /**
     * get contact count by group
     * @param groupId
     * @return
     */
    @Override
    public int getCountByGroup(Long groupId) {
        Long userId = AuthContext.getUserId();
        Integer count = baseMapper.selectCount(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUserId, userId)
                        .eq(Contact::getGroupId, groupId)
        );
        return count;
    }

    /**
     * get contact information By Id
     * @param contactId
     * @return
     */
    @Override
    public ContactDto getContactById(Long contactId) {
        Contact contact = baseMapper.selectById(contactId);
        Long groupId = contact.getGroupId();
        Group group = groupService.getGroupById(groupId);
        ContactDto contactDto = new ContactDto();
        BeanUtils.copyProperties(contact, contactDto);
        contactDto.setGroupName(group.getGroupName());
        return contactDto;
    }

    /**
     * batch remove contacts
     * @param idList
     */
    @Override
    public void batchRemove(List<Long> idList) {
        baseMapper.deleteBatchIds(idList);
    }

    /**
     * update Contact
     * @param contactDto
     */
    @Override
    public void updateContact(ContactDto contactDto) {
        Contact select = baseMapper.selectById(contactDto.getId());
        if (!select.getUserId().equals(AuthContext.getUserId())){
            throw new CusobException(ResultCodeEnum.UPDATE_CONTACT_FAIL);
        }
        this.paramVerify(contactDto);

        Group oldGroup = groupService.getGroupById(select.getGroupId());
        BeanUtils.copyProperties(contactDto, select);
        String newGroup = contactDto.getGroupName();
        if (!oldGroup.getGroupName().equals(newGroup)){
            select.setGroupId(groupService.getGroupIdByName(newGroup));
        }
        baseMapper.updateById(select);
    }

    /**
     * Contact List Pagination condition query
     * @param contactQueryDto
     * @return
     */
    @Override
    public IPage<ContactVo> getContactList(Page<Contact> pageParam, ContactQueryDto contactQueryDto) {
        String keyword = contactQueryDto.getKeyword();
        Long groupId = contactQueryDto.getGroupId();
        Long userId = AuthContext.getUserId();
        Long companyId = AuthContext.getCompanyId();
        User user = userService.getById(userId);
        IPage<ContactVo> contactVoPage;
        if (user.getPermission().equals(User.USER)){
            // user
            contactVoPage = baseMapper.pageQuery(pageParam, userId, keyword, groupId);
        }else {
            // admin
            contactVoPage = baseMapper.pageQueryByCompanyId(pageParam, companyId, keyword, groupId);
        }
        return contactVoPage;
    }

    /**
     * batch import contacts
     * @param file
     */
    @Override
    public void batchImport(MultipartFile file, String groupName) {
        if (file==null){
            throw new CusobException(ResultCodeEnum.FILE_IS_EMPTY);
        }

        Long userId = AuthContext.getUserId();
        Long companyId = AuthContext.getCompanyId();
        Integer count = this.selectCountByCompanyId(companyId);
        Long groupId = groupService.getGroupIdByName(groupName);
        Company company = companyService.getById(companyId);
        PlanPrice plan = planPriceService.getPlanById(company.getPlanId());

        try {
            InputStream inputStream = file.getInputStream();
            EasyExcel.read(inputStream, Contact.class, new PageReadListener<Contact>(dataList ->{
                if (count + dataList.size() >= plan.getContactCapacity()){
                    throw new CusobException(ResultCodeEnum.CONTACT_NUMBER_FULL);
                }
                for (Contact contact : dataList) {
                    contact.setUserId(userId);
                    contact.setCompanyId(companyId);
                    contact.setGroupId(groupId);
                    baseMapper.insert(contact);  // todo 待优化
                }
            })).sheet().doRead();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * get Contact Count By Group id
     * @param groupId
     * @return
     */
    @Override
    public Integer getContactCountByGroup(Long groupId) {
        Integer count = baseMapper.selectCount(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUserId, AuthContext.getUserId())
                        .eq(Contact::getGroupId, groupId)
        );
        return count;
    }

    /**
     * get Count By User id
     * @return
     */
    @Override
    public Integer getCountByUserId() {
        Long userId = AuthContext.getUserId();
        Integer count = baseMapper.selectCount(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUserId, userId)
        );
        return count;
    }

    /**
     * get contact List By Group id
     * @param groupId
     * @return
     */
    @Override
    public List<Contact> getListByGroupId(Long groupId) {
        Long userId = AuthContext.getUserId();
        List<Contact> contactList = baseMapper.selectList(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUserId, userId)
                        .eq(Contact::getGroupId, groupId)
        );
        return contactList;
    }

    /**
     * select Contact Count By CompanyId
     * @param companyId
     * @return
     */
    @Override
    public Integer selectCountByCompanyId(Long companyId) {
        Integer count = baseMapper.selectCount(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getCompanyId, companyId)
        );
        return count;
    }

    /**
     * get List By UserId And GroupId
     * @param userId
     * @param groupId
     * @return
     */
    @Override
    public List<Contact> getListByUserIdAndGroupId(Long userId, Long groupId) {
        List<Contact> contactList = baseMapper.selectList(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUserId, userId)
                        .eq(Contact::getGroupId, groupId)
        );
        return contactList;
    }

}
