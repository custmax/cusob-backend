package com.cusob.service;

import java.util.Map;

public interface DomainService {

    /**
     * domain Verify
     * @param email
     * @return
     */
    Map<String, Boolean> domainVerify(String email);
}
