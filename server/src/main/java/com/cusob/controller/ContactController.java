package com.cusob.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cusob.dto.ContactDto;
import com.cusob.dto.ContactQueryDto;
import com.cusob.entity.Contact;
import com.cusob.entity.Minio;
import com.cusob.result.Result;
import com.cusob.service.ContactService;
import com.cusob.service.MinioService;
import com.cusob.vo.ContactVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/contact")
@RestController
public class ContactController {

    @Autowired
    private Minio minio;

    @Autowired
    private MinioService minioService;

    @Autowired
    private ContactService contactService;

    @ApiOperation("add Contact")
    @PostMapping("add")
    public Result addContact(@RequestBody ContactDto contactDto){
        contactService.addContact(contactDto);
        return Result.ok();
    }

    @ApiOperation("get contact count by group")
    @GetMapping("getCountByGroup/{groupId}")
    public Result getCountByGroup(@PathVariable Long groupId){
        int count = contactService.getCountByGroup(groupId);
        return Result.ok(count);
    }

    @ApiOperation("get contact information By Id")
    @GetMapping("getById/{contactId}")
    public Result getById(@PathVariable Long contactId){
        ContactDto contactDto = contactService.getContactById(contactId);
        return Result.ok(contactDto);
    }

    @ApiOperation("batch remove contacts")
    @DeleteMapping("batchRemove")
    public Result batchRemove(@RequestBody List<Long> idList){
        contactService.batchRemove(idList);
        return Result.ok();
    }

    @ApiOperation("update Contact")
    @PostMapping("update")
    public Result updateContact(@RequestBody ContactDto contactDto){
        contactService.updateContact(contactDto);
        return Result.ok();
    }

    @ApiOperation("Contact List Pagination condition query")
    @GetMapping("getList/{page}/{limit}")
    public Result getContactList(@PathVariable Long page,
                                 @PathVariable Long limit,
                                 ContactQueryDto contactQueryDto){
        Page<Contact> pageParam = new Page<>(page, limit);
        IPage<ContactVo> contactVoPage = contactService.getContactList(pageParam, contactQueryDto);
        return Result.ok(contactVoPage);
    }

    @ApiOperation("upload Avatar")
    @PostMapping("uploadAvatar")
    public Result uploadAvatar(@RequestPart("file") MultipartFile file){
        String url = minioService.uploadAvatar(minio.getBucketName(), file);
        return Result.ok(url);
    }

    @ApiOperation("batch import contacts")
    @PostMapping("batchImport")
    public Result batchImport(@RequestPart("file") MultipartFile file, String groupName){
        contactService.batchImport(file, groupName);
        return Result.ok();
    }

    @ApiOperation("get All Contacts email By GroupId")
    @GetMapping("getAll/{groupId}")
    public Result getAllContactsByGroupId(@PathVariable Long groupId){
        List<String> emailList = contactService.getAllContactsByGroupId(groupId);
        return Result.ok(emailList);
    }
}
