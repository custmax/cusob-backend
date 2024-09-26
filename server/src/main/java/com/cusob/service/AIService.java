package com.cusob.service;

import com.cusob.dto.PromptDto;

import java.util.Map;

public interface AIService{


    String generateByGroup(PromptDto promptDto);

    Map<Long, String> generate(Long[] groupId);
}
