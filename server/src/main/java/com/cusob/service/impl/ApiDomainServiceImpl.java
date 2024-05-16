package com.cusob.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cusob.auth.AuthContext;
import com.cusob.dto.ApiDomainDto;
import com.cusob.entity.ApiDomain;
import com.cusob.exception.CusobException;
import com.cusob.mapper.ApiDomainMapper;
import com.cusob.result.ResultCodeEnum;
import com.cusob.service.ApiDomainService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class ApiDomainServiceImpl extends ServiceImpl<ApiDomainMapper, ApiDomain> implements ApiDomainService {

    @Override
    public ApiDomain selectByDomainName(String domain,Long userId) {
        return baseMapper.selectDomainByname(domain,userId);
    }

    @Override
    public void saveDomain(ApiDomainDto apiDomainDto) {
        ApiDomain apiDomain = new ApiDomain();
        BeanUtils.copyProperties(apiDomainDto,apiDomain);
        apiDomain.setUserId(AuthContext.getUserId());
        if(this.selectByDomainName(apiDomain.getDomain(),apiDomain.getUserId())!=null){
            throw new CusobException(ResultCodeEnum.DOMAIN_IS_EXISTED);
        }
        if((apiDomainDto.getVerify() & 1) != 0){
            apiDomain.setDkimVerify(1);
        }
        if((apiDomainDto.getVerify() & 2) != 0){
            apiDomain.setSpfVerify(1);
        }
        if((apiDomainDto.getVerify() & 4) != 0){
            apiDomain.setMxVerify(1);
        }
        if((apiDomainDto.getVerify() & 16) != 0){
            apiDomain.setDmarcVerify(1);
        }
        if(apiDomain.getSpfVerify()==1 && apiDomain.getDkimVerify()==1 && apiDomain.getMxVerify()==1){
            if(apiDomain.getDmarcVerify()==1){apiDomain.setStatus(2);}
            else {
                apiDomain.setStatus(1);
            }

        };

        save(apiDomain);
    }
}
