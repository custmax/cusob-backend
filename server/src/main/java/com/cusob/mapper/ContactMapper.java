package com.cusob.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cusob.entity.Contact;
import com.cusob.vo.ContactVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ContactMapper extends BaseMapper<Contact> {

    /**
     * page query by user id
     * @return
     */
    IPage<ContactVo> pageQuery(Page<Contact> pageParam,
                               @Param("userId") Long userId,
                               @Param("keyword") String keyword,
                               @Param("groupId") Long GroupId);

    /**
     * page Query By Company id
     * @param pageParam
     * @param companyId
     * @param keyword
     * @param groupId
     * @return
     */
    IPage<ContactVo> pageQueryByCompanyId(Page<Contact> pageParam,
                                          @Param("companyId") Long companyId,
                                          @Param("keyword") String keyword,
                                          @Param("groupId") Long groupId);
}
