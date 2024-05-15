package com.cusob.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cusob.auth.AuthContext;
import com.cusob.dto.TemplateDto;
import com.cusob.dto.TemplateQueryDto;
import com.cusob.entity.Company;
import com.cusob.entity.PlanPrice;
import com.cusob.entity.Template;
import com.cusob.exception.CusobException;
import com.cusob.mapper.TemplateMapper;
import com.cusob.result.ResultCodeEnum;
import com.cusob.service.CompanyService;
import com.cusob.service.TemplateService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TemplateServiceImpl extends ServiceImpl<TemplateMapper, Template> implements TemplateService {

    @Autowired
    private CompanyService companyService;

    /**
     * save customized Template
     * @param templateDto
     */
    @Override
    public void saveCustomizedTemplate(TemplateDto templateDto) {
//        Company company = companyService.getById(AuthContext.getCompanyId());
//        if (company.getPlanId().equals(PlanPrice.FREE)){
//            throw new CusobException(ResultCodeEnum.NO_PERMISSION);
//        }
        this.paramVerify(templateDto);
        Template template = new Template();
        BeanUtils.copyProperties(templateDto, template);
        template.setIsCustomized(1);
        template.setUserId(AuthContext.getUserId());
        baseMapper.insert(template);
    }

    /**
     * get template by id
     * @param id
     * @return
     */
    @Override
    public Template getTemplateById(Long id) {
        Template template = baseMapper.selectById(id);
        Integer isCustomized = template.getIsCustomized();
        Company company = companyService.getById(AuthContext.getCompanyId());
        if (company.getPlanId().equals(PlanPrice.FREE)){
            if (isCustomized.equals(Template.SYSTEM)){
                throw new CusobException(ResultCodeEnum.NO_PERMISSION);
            }
        }
        return template;
    }

    /**
     * update Template
     * @param templateDto
     */
    @Override
    public void updateTemplate(TemplateDto templateDto) {
        Template templateSelect = baseMapper.selectById(templateDto.getId());
        if (templateSelect.getIsCustomized().equals(Template.SYSTEM)){
            throw new CusobException(ResultCodeEnum.TEMPLATE_UPDATE_ERROR);
        }
        this.paramVerify(templateDto);
        Template template = new Template();
        BeanUtils.copyProperties(templateDto, template);
        template.setIsCustomized(1);
        template.setUserId(AuthContext.getUserId());
        baseMapper.updateById(template);
    }

    /**
     * get template List Group By Folder
     * @param templateQueryDto
     * @return
     */
    @Override
    public Map<String, List<Template>> getListGroupByFolder(TemplateQueryDto templateQueryDto) {
        String keyword = templateQueryDto.getKeyword();
        String folder = templateQueryDto.getFolder();
        Map<String, List<Template>> map = new HashMap<>();
        List<String> folderList = this.getFolderList();
        if (StringUtils.hasText(folder)){
            List<Template> list = this.getTemplateListByFolder(folder, keyword);
            map.put(folder, list);
            return map;
        }
        for (String item : folderList) {
            List<Template> list = this.getTemplateListByFolder(item, keyword);
            if (list!=null && list.size()>0){
                map.put(item, list);
            }
        }
        return map;
    }

    /**
     * get Folder List
     * @return
     */
    @Override
    public List<String> getFolderList() {
        List<String> folderList = baseMapper.getFolderList();
        return folderList;
    }

    /**
     * get Template List By Folder
     * @param folder
     * @param keyword
     * @return
     */
    @Override
    public List<Template> getTemplateListByFolder(String folder, String keyword) {
        List<Template> templateList = baseMapper.selectList(
                new LambdaQueryWrapper<Template>()
                        .eq(Template::getUserId, AuthContext.getUserId())
                        .eq(Template::getFolder, folder)
                        .like(StringUtils.hasText(keyword), Template::getName, keyword)
        );
        return templateList;
    }

    /**
     * remove Customized Template
     * @param id
     */
    @Override
    public void removeCustomizedTemplate(Long id) {
        Template template = baseMapper.selectById(id);
        Integer isCustomized = template.getIsCustomized();
        if (isCustomized.equals(Template.SYSTEM)){
            throw new CusobException(ResultCodeEnum.TEMPLATE_REMOVE_ERROR);
        }
        baseMapper.deleteById(id);
    }

    private void paramVerify(TemplateDto templateDto) {
        if (!StringUtils.hasText(templateDto.getName())){
            throw new CusobException(ResultCodeEnum.TEMPLATE_NAME_EMPTY);
        }
        if (!StringUtils.hasText(templateDto.getSubject())){
            throw new CusobException(ResultCodeEnum.TEMPLATE_SUBJECT_EMPTY);
        }
        if (!StringUtils.hasText(templateDto.getFolder())){
            throw new CusobException(ResultCodeEnum.TEMPLATE_FOLDER_EMPTY);
        }
        // TODO 其他参数校验
    }
}
