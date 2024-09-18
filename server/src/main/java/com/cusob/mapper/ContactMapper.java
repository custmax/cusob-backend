package com.cusob.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cusob.dto.ContactDto;
import com.cusob.dto.ContactGroupDto;
import com.cusob.entity.Contact;
import com.cusob.vo.ContactVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContactMapper extends BaseMapper<Contact> {

    /**
     * page query by user id
     * @return
     */
    IPage<ContactVo> pageQuery(Page<Contact> pageParam,
                               @Param("userId") Long userId,
                               @Param("keyword") String keyword,
                               @Param("groupId") Long GroupId,
                               @Param("subscriptionType") String subscriptionType);

    /**
     * page Query By Company id
     *
     * @param pageParam
     * @param companyId
     * @param keyword
     * @param groupId
     * @param subscriptionType
     * @return
     */
    IPage<ContactVo> pageQueryByCompanyId(Page<Contact> pageParam,
                                          @Param("companyId") Long companyId,
                                          @Param("keyword") String keyword,
                                          @Param("groupId") Long groupId,
                                          @Param("subscriptionType") String subscriptionType);

    ContactVo selectByEmail(@Param("email") String email,
                            @Param("groupId") Long groupId
                            );
    List<ContactGroupDto> selectGroups(@Param("userId") Long userId,
                                       @Param("selectType") String selectType,
                                       @Param("selectOption") String selectOption);

    List<ContactDto> selectContacts(@Param("userId") Long userId, @Param("searchInfo") String searchInfo);


    void deleteGroupsFromContact(@Param("indexs")Integer[] indexs, @Param("userId") Long userId);
    void deleteGroupsFromContactGroups(@Param("indexs")Integer[] indexs, @Param("userId") Long userId);
    void deleteGroupsFromReport(@Param("indexs")Integer[] indexs, @Param("userId") Long userId);

    void deleteContactsFromContact(Integer[] indexs, Long userId);
}
