package com.cusob.service.impl;

import com.cusob.entity.Dkim;
import com.cusob.exception.CusobException;
import com.cusob.result.ResultCodeEnum;
import com.cusob.service.DkimService;
import com.cusob.service.DomainService;
import com.cusob.utils.DnsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DomainServiceImpl implements DomainService {

    @Autowired
    private DkimService dkimService;

    @Value("${cusob.domain.spf}")
    private String spf;

    @Value("${cusob.domain.dkim.prefix}")
    private String dkimPrefix;

    /**
     * domain Verify
     * @param email
     * @return
     */
    @Override
    public Map<String, Boolean> domainVerify(String email) {
        if (!StringUtils.hasText(email)){
            throw new CusobException(ResultCodeEnum.EMAIL_IS_EMPTY);
        }
        String domain = email.substring(email.lastIndexOf('@') + 1);
        Map<String, Boolean> map = new HashMap<>();
        // todo 采用异步操作
        Boolean flagSpf = this.spfVerify(domain);
        Boolean flagDkim = this.dkimVerify(domain);
        map.put("spf", flagSpf);
        map.put("dkim", flagDkim);
        return map;
    }

    private Boolean dkimVerify(String domain) {
        List<String> dkimList = DnsUtil.checkDkim(domain);
        Dkim dkimSelect = dkimService.getDkim(domain);
        String dkim = dkimPrefix + dkimSelect.getPublicKey();
        if (dkimList!=null && dkimList.contains(dkim)){
            return true;
        }
        return false;
    }

    /**
     * spf verify
     * @return
     */
    private Boolean spfVerify(String domain) {
        List<String> spfList = DnsUtil.checkSpf(domain);
        if (spfList!=null && spfList.contains(spf)){
            return true;
        }
        return false;
    }
}
