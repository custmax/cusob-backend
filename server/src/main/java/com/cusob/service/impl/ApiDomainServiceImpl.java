package com.cusob.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cusob.entity.ApiDomain;
import com.cusob.mapper.ApiDomainMapper;
import com.cusob.service.ApiDomainService;
import org.springframework.stereotype.Service;

@Service
public class ApiDomainServiceImpl extends ServiceImpl<ApiDomainMapper, ApiDomain> implements ApiDomainService {
}
