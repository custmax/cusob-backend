package com.cusob.service.impl;

import com.cusob.entity.Contact;
import com.cusob.entity.Group;
import com.cusob.mapper.ContactMapper;
import com.cusob.mapper.GroupMapper;
import com.cusob.service.ContactService;
import com.cusob.vo.GroupRequestVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ContactServiceImplTest {

    @Autowired
    private ContactService contactService;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private ContactMapper contactMapper;

    @Test
    public void testAddGroupaddGroupByContactId() {
        GroupRequestVO request = new GroupRequestVO();
        request.setContactIdlist(new Long[]{1L, 2L}); // 根据您的实际数据
        request.setGroupName("Test Group");

        // 调用服务方法
        Long result = contactService.addGroupaddGroupByContactId(request);

        // 检查数据库中是否插入成功
        Group group = groupMapper.selectById(result);
        assertNotNull(group);
        assertEquals("Test Group", group.getGroupName());

        // 如果需要检查联系人是否关联到新组
        for (Long contactId : request.getContactIdlist()) {
            Contact contact = contactMapper.selectById(contactId); // 获取联系人的数据
            assertEquals(result, contact.getGroupId()); // 确认联系人的组ID是否为新组的ID
        }
    }
}
