package com.cusob.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cusob.entity.Dkim;
import com.cusob.exception.CusobException;
import com.cusob.mapper.DkimMapper;
import com.cusob.result.ResultCodeEnum;
import com.cusob.service.DkimService;
import com.cusob.utils.DkimGeneratorUtil;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DkimServiceImpl extends ServiceImpl<DkimMapper, Dkim> implements DkimService {

    /**
     * save Dkim
     * @param domain
     */
    @Override
    public void saveDkim(String domain) {
        Dkim dkim = new Dkim();
        dkim.setDomain(domain);
        Map<String, String> map = DkimGeneratorUtil.generator();
        if (map==null){
            throw new CusobException(ResultCodeEnum.KEY_GENERATE_FAIL);
        }
        dkim.setPrivateKey(map.get(DkimGeneratorUtil.PRIVATE_KEY));
        dkim.setPublicKey(map.get(DkimGeneratorUtil.PUBLIC_KEY));
        baseMapper.insert(dkim);
    }

    /**
     * get Dkim
     * @param domain
     * @return
     */
    @Override
    public Dkim getDkim(String domain) {
        Dkim dkim = baseMapper.selectOne(
                new LambdaQueryWrapper<Dkim>()
                        .eq(Dkim::getDomain, domain)
        );
        return dkim;
    }

    /**
     * get publicKey by domain
     * @param domain
     * @return
     */
    @Override
    public String getPublicKey(String domain) {
        Dkim dkim = baseMapper.selectOne(
                new LambdaQueryWrapper<Dkim>()
                        .eq(Dkim::getDomain, domain)
        );
        return dkim.getPublicKey();
    }
}
