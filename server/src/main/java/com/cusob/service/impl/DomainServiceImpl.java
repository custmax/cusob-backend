package com.cusob.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cusob.auth.AuthContext;
import com.cusob.entity.Dkim;
import com.cusob.entity.Domain;
import com.cusob.exception.CusobException;
import com.cusob.mapper.DomainMapper;
import com.cusob.result.ResultCodeEnum;
import com.cusob.service.DkimService;
import com.cusob.service.DomainService;
import com.cusob.utils.DnsUtil;
import com.cusob.vo.DomainListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DomainServiceImpl extends ServiceImpl<DomainMapper, Domain> implements DomainService {

    @Autowired
    private DkimService dkimService;

    @Value("${cusob.domain.spf}")
    private String spf;

    /**
     * domain Verify
     * @param domain
     * @return
     */
    @Override
    public Map<String, Boolean> domainVerify(String domain) {
        if (!StringUtils.hasText(domain)){
            throw new CusobException(ResultCodeEnum.DOMAIN_IS_EMPTY);
        }
        Map<String, Boolean> map = new HashMap<>();
        // todo 采用异步操作
        Boolean flagSpf = this.spfVerify(domain);
        Boolean flagDkim = this.dkimVerify(domain);
        map.put("spf", flagSpf);
        map.put("dkim", flagDkim);
        Domain domainSelect = this.getByDomain(domain);
        domainSelect.setSpf(flagSpf);
        domainSelect.setDkim(flagDkim);
        baseMapper.updateById(domainSelect);
        return map;
    }

    /**
     * get DomainList
     * @return
     */
    @Override
    public List<DomainListVo> getDomainList() {
        Long userId = AuthContext.getUserId();
        List<DomainListVo> list = baseMapper.getDomainList(userId);
        return list;
    }

    /**
     * get Domain
     * @param domain
     */
    @Override
    public Domain getByDomain(String domain) {
        Domain domainSelect = baseMapper.selectOne(
                new LambdaQueryWrapper<Domain>()
                        .eq(Domain::getDomain, domain)
        );
        return domainSelect;
    }

    private Boolean dkimVerify(String domain) {
        Dkim dkimSelect = dkimService.getDkim(domain);
        String selector = dkimSelect.getSelector();
        List<String> dkimList = DnsUtil.checkDkim(selector ,domain);

        String publicKey = dkimSelect.getPublicKey();
        if (dkimList!=null && dkimList.contains(publicKey)){
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
