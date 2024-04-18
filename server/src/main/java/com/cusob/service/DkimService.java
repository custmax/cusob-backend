package com.cusob.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cusob.entity.Dkim;

public interface DkimService extends IService<Dkim> {

    /**
     * save Dkim
     * @param domain
     */
    void saveDkim(String domain);

    /**
     * get Dkim
     * @param domain
     * @return
     */
    Dkim getDkim(String domain);

    /**
     * get publicKey by domain
     * @param domain
     * @return
     */
    String getPublicKey(String domain);
}
