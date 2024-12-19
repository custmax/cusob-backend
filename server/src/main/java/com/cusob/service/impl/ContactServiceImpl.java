package com.cusob.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cusob.auth.AuthContext;
import com.cusob.constant.MqConst;
import com.cusob.dto.ContactDto;
import com.cusob.dto.ContactQueryDto;
import com.cusob.dto.GroupDto;
import com.cusob.entity.*;
import com.cusob.exception.CusobException;
import com.cusob.mapper.ContactMapper;
import com.cusob.result.ResultCodeEnum;
import com.cusob.service.*;
import com.cusob.vo.ContactVo;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.cusob.utils.EmailUtil.readResponse;
import static com.cusob.utils.EmailUtil.sendCommand;

@Service
public class ContactServiceImpl extends ServiceImpl<ContactMapper, Contact> implements ContactService {

    @Autowired
    private GroupService groupService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private PriceService priceService;

    @Autowired
    private UserService userService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * add Contact
     *
     * @param contactDto
     */
    @Override
    @Transactional
    public void addContact(ContactDto contactDto) {
        // contacts count
        Long companyId = AuthContext.getCompanyId();//获取当前用户的公司id
        Integer count = this.selectCountByCompanyId(companyId);//获取当前公司的联系人数量
        Company company = companyService.getById(companyId);//获取当前公司
        Price plan = priceService.getPlanById(company.getPlanId());//获取当前公司的套餐
        if (count >= plan.getContactCapacity()) {
            throw new CusobException(ResultCodeEnum.CONTACT_NUMBER_FULL);//联系人数量已满
        }
        // 参数校验
        //this.paramVerify(contactDto);
        Contact contact = new Contact();//创建联系人对象

        String groupName = contactDto.getGroupName();//获取联系人的组名
        // The group name is not empty
        if (StringUtils.hasText(groupName)) {//如果组名不为空
            Group group = groupService.getGroupByName(groupName);//获取组名
            // The group doesn't exist，create group
            if (group == null) {//如果组名不存在
                GroupDto groupDto = new GroupDto();
                groupDto.setGroupName(groupName);
                Long groupId = groupService.addGroup(groupDto);//添加组
                contact.setGroupId(groupId);//设置组id
            }
            else {
                // The group already exists
                contact.setGroupId(group.getId());
            }
        }
        contact.setUserId(AuthContext.getUserId());//设置用户id
        BeanUtils.copyProperties(contactDto, contact);//将contactDto的属性拷贝到contact中
        //        rabbitTemplate.convertAndSend(MqConst.EXCHANGE_CHECK_DIRECT,//发送消息到交换机
        //                MqConst.ROUTING_CHECK_EMAIL, contact); //验证该邮箱是否真实存在（充分条件）
        try {
            rabbitTemplate.convertAndSend(MqConst.EXCHANGE_CHECK_DIRECT, // 发送消息到交换机
                    MqConst.ROUTING_CHECK_EMAIL, contact); // 验证该邮箱是否真实存在（充分条件）
        } catch (Exception e) {
            // 记录异常信息或执行其他处理
            System.err.println("Failed to send message to RabbitMQ: " + e.getMessage());
            e.printStackTrace(); // 打印完整的堆栈跟踪信息
            // 你可以选择重新抛出异常或者执行其他的异常处理逻辑
            //throw new CusobException(e); // 自定义异常处理
        }

        if (baseMapper.selectByEmail(contact.getEmail(), contact.getGroupId()) == null) {
            contact.setUserId(AuthContext.getUserId());
            contact.setCompanyId(AuthContext.getCompanyId());
            baseMapper.insert(contact);
        }
        else {
            throw new CusobException(ResultCodeEnum.CONTACT_IS_EXISTED);
        }

    }

    @Override
    public void updateByEmail(String email, Long groupId, Long userId, int valid) {
        //        Contact contact = baseMapper.selectOne(new LambdaQueryWrapper<Contact>()
        //                .eq(Contact::getEmail, email)
        //                .eq(Contact::getUserId,userId)
        //                .eq(Contact::getGroupId,groupId)
        //        );
        //        contact.setValid(valid); //设置是否有效
        //        baseMapper.updateById(contact);
        List<Contact> contacts = baseMapper.selectList(new LambdaQueryWrapper<Contact>()//查询多条联系人
                .eq(Contact::getEmail, email)
                .eq(Contact::getUserId, userId)
                .eq(Contact::getGroupId, groupId)
        );
        for (Contact contact : contacts) {
            contact.setValid(valid); //设置是否有效
            baseMapper.updateById(contact);
        }

    }

    private void paramVerify(ContactDto contactDto) {
        if (!StringUtils.hasText(contactDto.getFirstName())) {
            throw new CusobException(ResultCodeEnum.FIRST_NAME_IS_EMPTY);
        }
        if (!StringUtils.hasText(contactDto.getLastName())) {
            throw new CusobException(ResultCodeEnum.LAST_NAME_IS_EMPTY);
        }
        if (!StringUtils.hasText(contactDto.getEmail())) {
            throw new CusobException(ResultCodeEnum.EMAIL_IS_EMPTY);
        }
        if (!StringUtils.hasText(contactDto.getGroupName())) {
            throw new CusobException(ResultCodeEnum.GROUP_NAME_EMPTY);
        }
    }

    /**
     * get contact count by group
     *
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
     *
     * @param contactId
     * @return
     */
    @Override
    public ContactDto getContactById(Long contactId) {
        Contact contact = baseMapper.selectById(contactId);
        if (contact != null) {
            Long groupId = contact.getGroupId();
            Group group = groupService.getGroupById(groupId);
            ContactDto contactDto = new ContactDto();
            BeanUtils.copyProperties(contact, contactDto);
            contactDto.setGroupName(group.getGroupName());
            return contactDto;
        }
        return null;
    }

    /**
     * batch remove contacts
     *
     * @param idList
     */
    @Override
    public void batchRemove(List<Long> idList) {
        baseMapper.deleteBatchIds(idList);
    }

    /**
     * update Contact
     *
     * @param contactDto
     */
    @Override
    public void updateContact(ContactDto contactDto) {
        Contact select = baseMapper.selectById(contactDto.getId());
        if (!select.getUserId().equals(AuthContext.getUserId())) {
            throw new CusobException(ResultCodeEnum.UPDATE_CONTACT_FAIL);
        }
        this.paramVerify(contactDto);

        Group oldGroup = groupService.getGroupById(select.getGroupId());
        BeanUtils.copyProperties(contactDto, select);

        String newGroup = contactDto.getGroupName();
        if (!oldGroup.getGroupName().equals(newGroup)) {
            select.setGroupId(groupService.getGroupIdByName(newGroup));
        }
        rabbitTemplate.convertAndSend(MqConst.EXCHANGE_CHECK_DIRECT,
                MqConst.ROUTING_CHECK_EMAIL,
                select);
        baseMapper.updateById(select);
    }


    /**
     * Contact List Pagination condition query
     *
     * @param contactQueryDto
     * @return
     */
    @Override
    public IPage<ContactVo> getContactList(Page<Contact> pageParam, ContactQueryDto contactQueryDto) {
        IPage<ContactVo> contactVoPage;

        Long userId = AuthContext.getUserId();
        long current = pageParam.getCurrent();
        long pageSize = pageParam.getSize();
        Long groupId = contactQueryDto.getGroupId();
        String groupName = groupId == null ? "" : groupService.getGroupById(groupId).getGroupName();
        // 拼接redis Key
        String rekey =
                "contact_list_" + current + "_" + pageSize + "_" + userId + "_" + groupName + contactQueryDto.hashCode();
        String countKey = rekey + "_totalCount";
        // 获取对应的zset
        BoundZSetOperations<String, Object> boundZSetOps = redisTemplate.boundZSetOps(rekey);
        Integer totalCount = (Integer) redisTemplate.opsForValue().get(countKey);
        // 没有缓存
        if (totalCount == null || totalCount == 0) {
            // this
            String keyword = contactQueryDto.getKeyword();
            Long companyId = AuthContext.getCompanyId();
            User user = userService.getById(userId);
            String SubscriptionType = contactQueryDto.getSubscriptionType();

            // 用户与管理员逻辑
            if (user.getPermission().equals(User.USER)) {
                // user
                contactVoPage = baseMapper.pageQuery(pageParam, userId, keyword, groupId, SubscriptionType);
            }
            else {
                // admin
                contactVoPage = baseMapper.pageQueryByCompanyId(pageParam, companyId, keyword, groupId,
                        SubscriptionType);
            }

            totalCount = (int) contactVoPage.getTotal();
            //将查询结果缓存到Redis中，并设置过期时间
            redisTemplate.opsForValue().set(countKey, totalCount);
            List<ContactVo> EventsList = contactVoPage.getRecords();
            for (int i = 0; i < EventsList.size(); i++) {
                boundZSetOps.add(EventsList.get(i), i);
            }
            //设置两个key的过期时间
            redisTemplate.expire(rekey, 5, TimeUnit.MINUTES);
            redisTemplate.expire(countKey, 5, TimeUnit.MINUTES);
        }
        else {
            Set<Object> eventSet = boundZSetOps.range(0, pageSize);
            List<Object> eventsList = new ArrayList<>(eventSet.size());
            for (Object event1 : eventSet) {
                eventsList.add((Object) event1);
            }
            contactVoPage = new Page(current, pageSize, totalCount).setRecords(eventsList);
        }

        return contactVoPage;
    }

    /**
     * batch import contacts
     *
     * @param file
     */
    @Override
    public CampaignReturn batchImport(MultipartFile file, String groupName, String subscriptionType) {
        if (file == null) {
            throw new CusobException(ResultCodeEnum.FILE_IS_EMPTY);
        }
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);
        Long userId = AuthContext.getUserId();
        Long companyId = AuthContext.getCompanyId();
        Integer count = this.selectCountByCompanyId(companyId);//获取当前公司的联系人数量
        Long groupId = groupService.getGroupIdByName(groupName);
        Company company = companyService.getById(companyId);
        Price plan = priceService.getPlanById(company.getPlanId());

        try {
            InputStream inputStream = file.getInputStream();
            //使用 EasyExcel 库读取文件中的数据。Contact.class 指定了数据模型，PageReadListener<Contact> 是批量处理的监听器
            EasyExcel.read(inputStream, Contact.class, new PageReadListener<Contact>(dataList -> {
                if (count + dataList.size() >= plan.getContactCapacity()) {
                    throw new CusobException(ResultCodeEnum.CONTACT_NUMBER_FULL);
                }
                for (Contact contact : dataList) {
                    contact.setUserId(userId);
                    contact.setCompanyId(companyId);
                    contact.setGroupId(groupId);
                    contact.setSubscriptionType(subscriptionType);
                    contact.setIsAvailable(1);
                    try {
                        //baseMapper.insert(contact);
                        //                        rabbitTemplate.convertAndSend(MqConst.EXCHANGE_CHECK_DIRECT, //
                        //                        发送消息到交换机
                        //                                MqConst.ROUTING_CHECK_EMAIL, contact); // 验证该邮箱是否真实存在（充分条件）
                        boolean check = checkEmail(contact);
                        contact.setValid(check == true ? 1 : 0);
                    } catch (Exception e) {
                        // 记录异常信息或执行其他处理
                        System.err.println("Failed to send message to RabbitMQ: " + e.getMessage());
                        e.printStackTrace(); // 打印完整的堆栈跟踪信息
                        // 你可以选择重新抛出异常或者执行其他的异常处理逻辑
                        //throw new CusobException(e); // 自定义异常处理
                    }
                    if (contact.getValid() == 1) {
                        baseMapper.insert(contact);  // todo 待优化
                        success.getAndIncrement();
                    }
                    else {
                        //baseMapper.deleteById(contact);
                        fail.getAndIncrement();
                    }
                }
            })).sheet().doRead();

        } catch (IOException e) {
            e.printStackTrace();
        }
        CampaignReturn campaignReturn = new CampaignReturn();
        campaignReturn.setSuccessCount(success.get());
        campaignReturn.setFailCount(fail.get());
        return campaignReturn;
    }

    @Override
    public Map<String, Object> parseFields(MultipartFile file) {
        if (file == null) {
            throw new CusobException(ResultCodeEnum.FILE_IS_EMPTY);
        }
        Map<String, Object> response = new HashMap<>();
        try (InputStream inputStream = file.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            // 使用 BufferedReader 读取 CSV 文件的第一行（头部行）
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new CusobException(ResultCodeEnum.FILE_IS_EMPTY);
            }

            // 将头部行拆分成字段列表
            List<String> headers = Arrays.asList(headerLine.split(","));

            // 返回字段列表
            response.put("fields", headers);
            System.out.println("fields: " + headers);
            return response;

        } catch (IOException e) {
            e.printStackTrace();
            response.put("error", "Failed to parse file");
            return response;
        }
    }

    /**
     * get Contact Count By Group id
     *
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
     *
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
     *
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
     *
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
     *
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

    /**
     * get All Contacts email By GroupId
     *
     * @param groupId
     * @return
     */
    @Override
    public List<String> getAllContactsByGroupId(Long groupId) {
        List<Contact> contactList = this.getListByUserIdAndGroupId(AuthContext.getUserId(), groupId);
        List<String> emailList = contactList.stream().map(Contact::getEmail).collect(Collectors.toList());
        return emailList;
    }

    @Override
    public List<Contact> getContactsByEmail(String email) {
        List<Contact> contacts = baseMapper.selectList(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getEmail, email)
        );
        return contacts;
    }

    @Override
    public void saveUnsubsribedEmail(String email) {
        List<Contact> contacts = baseMapper.selectList(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getEmail, email)
        );
        for (Contact contact : contacts) {
            contact.setSubscriptionType("Unsubscribed");
            baseMapper.updateById(contact);
        }

    }

    public boolean checkEmail(Contact contact) throws IOException {
        String email = contact.getEmail();
        Long groupId = contact.getGroupId();
        boolean check = true;

        try {
            if (email == null || !email.contains("@")) {
                // 如果 email 为空或者不包含 '@' 符号，直接设置 check 为 false
                check = false;
            }
            else {
                String domain = email.split("@")[1]; // 获取域名
                // 检查MX记录
                Record[] records = new Lookup(domain, Type.MX).run(); // 查询MX记录
                if (!(records != null && records.length > 0)) { // 如果没有MX记录
                    check = false;
                }
                else {
                    for (Record record : records) { // 遍历MX记录
                        MXRecord mxRecord = (MXRecord) record; // 获取MX记录
                        // 连接
                        try (Socket socket = new Socket(mxRecord.getTarget().toString(), 25);
                             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

                            readResponse(reader);
                            // 握手
                            sendCommand(writer, "HELO " + domain);
                            readResponse(reader);
                            // 身份
                            sendCommand(writer, "MAIL FROM:<verify@" + domain + ">");
                            readResponse(reader);
                            // 验证
                            sendCommand(writer, "RCPT TO:<" + email + ">");
                            String response = readResponse(reader);

                            // 断开
                            if (response.startsWith("250")) {
                                sendCommand(writer, "QUIT");
                                break;
                            }
                            else if (response.startsWith("550")) {
                                sendCommand(writer, "QUIT");
                                check = false;
                            }
                        }
                    }
                }
            }
            //            contactService.updateByEmail(email, groupId, contact.getUserId(), check ? 1 : 0); // 更新状态
        } catch (Exception e) {
            //            contactService.updateByEmail(email, groupId, contact.getUserId(), 1);
            // 如果抛异常，可能是无法连接到相应的SMTP服务器，无法用这种方式判断存不存在，则先按存在处理
            e.printStackTrace(); // 打印异常信息
        } finally {
            //            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return check;
        }
    }


}
