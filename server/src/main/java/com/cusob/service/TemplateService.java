package com.cusob.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cusob.dto.TemplateDto;
import com.cusob.dto.TemplateQueryDto;
import com.cusob.entity.Template;

import java.util.List;
import java.util.Map;

public interface TemplateService extends IService<Template> {

    /**
     * save customized Template
     * @param templateDto
     */
    void saveCustomizedTemplate(TemplateDto templateDto);

    /**
     * get template by id
     * @param id
     * @return
     */
    Template getTemplateById(Long id);

    /**
     * update Template
     * @param templateDto
     */
    void updateTemplate(TemplateDto templateDto);

    /**
     * get template List Group By Folder
     * @param templateQueryDto
     * @return
     */
    Map<String, List<Template>> getListGroupByFolder(TemplateQueryDto templateQueryDto);

    /**
     * get Folder List
     * @return
     */
    List<String> getFolderList();

    /**
     * get Template List By Folder
     * @param folder
     * @param keyword
     * @return
     */
    List<Template> getTemplateListByFolder(String folder, String keyword);

    List<Template> getTemplateDefault(String folder);

    /**
     * remove Customized Template
     * @param id
     */
    void removeCustomizedTemplate(Long id);
}
