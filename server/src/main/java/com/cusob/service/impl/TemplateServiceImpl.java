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
import java.util.stream.Collectors;

@Service
public class TemplateServiceImpl extends ServiceImpl<TemplateMapper, Template> implements TemplateService
{

    @Autowired
    private CompanyService companyService;

    /**
     * save customized Template
     *
     * @param templateDto
     */
    @Override
    public void saveCustomizedTemplate(TemplateDto templateDto)
    {
        //        Company company = companyService.getById(AuthContext.getCompanyId());
        //        if (company.getPlanId().equals(PlanPrice.FREE)){
        //            throw new CusobException(ResultCodeEnum.NO_PERMISSION);
        //        }
        System.out.println("修改时传入的folder value : " + templateDto.getFolder());
        // todo 此处使用硬编码,防止用户新增系统所给的folder
        if (templateDto.getFolder().equals("all") ||
                templateDto.getFolder().equals("personal") ||
                templateDto.getFolder().equals("welcome") ||
                templateDto.getFolder().equals("seasons") ||
                templateDto.getFolder().equals("dealsAndOffers"))
        {
            throw new CusobException(ResultCodeEnum.TEMPLATE_FOLDER_ERROR);
        }
        this.paramVerify(templateDto);
        Template template = new Template();
        BeanUtils.copyProperties(templateDto, template);
        template.setIsCustomized(1);
        template.setUserId(AuthContext.getUserId());
        // todo 这里因为前端不传值进来，默认为0
        template.setType(0);
        baseMapper.insert(template);
    }

    /**
     * get template by id
     *
     * @param id
     * @return
     */
    @Override
    public Template getTemplateById(Long id)
    {
        Template template = baseMapper.selectById(id);
        Integer isCustomized = template.getIsCustomized();
        Company company = companyService.getById(AuthContext.getCompanyId());
        if (company.getPlanId().equals(PlanPrice.FREE))
        {
            if (isCustomized.equals(Template.SYSTEM))
            {
                //                throw new CusobException(ResultCodeEnum.NO_PERMISSION);
            }
        }
        return template;
    }

    /**
     * update Template
     *
     * @param templateDto
     */
    @Override
    public void updateTemplate(TemplateDto templateDto)
    {
        Template templateSelect = baseMapper.selectById(templateDto.getId());
        if (templateSelect.getIsCustomized().equals(Template.SYSTEM))
        {
            throw new CusobException(ResultCodeEnum.TEMPLATE_UPDATE_ERROR);
        }
        this.paramVerify(templateDto);
        Template template = new Template();
        BeanUtils.copyProperties(templateDto, template);
        template.setIsCustomized(1);
        template.setUserId(AuthContext.getUserId());
        template.setType(0);
        baseMapper.updateById(template);
    }

    /**
     * filterTemplateByFolderAndKeyword(name)
     */
    private List<Template> filterTemplateByFolderAndKeyword(String folder, String keyword, List<Template> templateList)
    {
        if (StringUtils.hasText(folder))
        {
            // 如果是personal,则按照user id查询,否则按folder查询
            if (folder.equals("personal")) {
                Long userId = AuthContext.getUserId();
                templateList =  templateList.stream().filter(template ->
                {
                    return template.getUserId() != null && template.getUserId().equals(userId);
                }).collect(Collectors.toList());
            } else {
                templateList =  templateList.stream().filter(template ->
                {
                    return template.getFolder().equals(folder);
                }).collect(Collectors.toList());
            }
        }
        if (StringUtils.hasText(keyword))
        {
            templateList = templateList.stream().filter(template ->
            {
                return template.getName().matches(".*" + keyword + ".*");
            }).collect(Collectors.toList());
        }
        return templateList;
    }

    /**
     * get template List Group By Folder
     *
     * @param templateQueryDto
     * @return
     */
    @Override
    public Map<String, List<Template>> getListGroupByFolder(TemplateQueryDto templateQueryDto)
    {
        String keyword = templateQueryDto.getKeyword();
        String folder = templateQueryDto.getFolder();
        Map<String, List<Template>> map = new HashMap<>();
        //List<String> folderList = this.getFolderList();
        // 控制访问权限，只有当user_id is null 或者为其本身user_id时，才允许访问
        Long userId = AuthContext.getUserId();
        System.out.println("此处userId:" + userId);
        List<Template> templateListByUserId = this.getFolderListByUserId(userId);

        // 此处取出所有template, 直接在内存过滤
        if (StringUtils.hasText(folder) || StringUtils.hasText(keyword))
        {
            //List<Template> list = this.getTemplateListByFolder(folder, keyword);
            //map.put(folder, list);
            //return map;
            templateListByUserId = filterTemplateByFolderAndKeyword(folder, keyword, templateListByUserId);
        }
        //for (String item : folderList) {
        //    List<Template> list = this.getTemplateDefault(item);
        //    if (list!=null && list.size()>0){
        //        map.put(item, list);
        //    }
        //}
        for (Template item : templateListByUserId)
        {
            //List<Template> list = this.getTemplateDefault(item);

            //if (list!=null && list.size()>0){
            //    map.put(item, list);
            //}
            List<Template> itemList = map.getOrDefault(item.getFolder(), new ArrayList<Template>());
            itemList.add(item);
            map.put(item.getFolder(), itemList);
        }
        return map;
    }

    /**
     * get Folder List
     *
     * @return
     */
    @Override
    public List<String> getFolderList()
    {
        List<String> folderList = baseMapper.getFolderList();
        return folderList;
    }

    /**
     * get Folder List By UserId
     *
     * @return
     */
    @Override
    public List<Template> getFolderListByUserId(Long userId)
    {
        //List<String> folderList = baseMapper.getFolderList();
        List<Template> folderListByUserId = baseMapper.getFolderListByUserId(userId);
        return folderListByUserId;
    }

    /**
     * get Template List By Folder
     *
     * @param folder
     * @param keyword
     * @return
     */
    @Override
    public List<Template> getTemplateListByFolder(String folder, String keyword)
    {
        List<Template> templateList = baseMapper.selectList(
                new LambdaQueryWrapper<Template>()
                        .eq(Template::getUserId, AuthContext.getUserId())
                        .eq(Template::getFolder, folder)
                        .like(StringUtils.hasText(keyword), Template::getName, keyword)
        );
        return templateList;

    }

    @Override
    public List<Template> getTemplateDefault(String folder)
    {
        List<Template> originalList = baseMapper.selectList(
                new LambdaQueryWrapper<Template>()
                        .eq(Template::getUserId, AuthContext.getUserId())
                        .eq(Template::getFolder, folder)
        );
        if (!folder.equals("public"))
        {
            return originalList;
        }
        List<Template> customizedList = baseMapper.selectList(
                new LambdaQueryWrapper<Template>()
                        .eq(Template::getIsCustomized, 0)
        );

        // 合并两个列表
        List<Template> mergedList = new ArrayList<>(originalList);
        mergedList.addAll(customizedList);
        return mergedList;
    }

    /**
     * remove Customized Template
     *
     * @param id
     */
    @Override
    public void removeCustomizedTemplate(Long id)
    {
        Template template = baseMapper.selectById(id);
        Integer isCustomized = template.getIsCustomized();
        if (isCustomized.equals(Template.SYSTEM))
        {
            throw new CusobException(ResultCodeEnum.TEMPLATE_REMOVE_ERROR);
        }
        baseMapper.deleteById(id);
    }

    private void paramVerify(TemplateDto templateDto)
    {
        if (!StringUtils.hasText(templateDto.getName()))
        {
            throw new CusobException(ResultCodeEnum.TEMPLATE_NAME_EMPTY);
        }
        if (!StringUtils.hasText(templateDto.getSubject()))
        {
            throw new CusobException(ResultCodeEnum.TEMPLATE_SUBJECT_EMPTY);
        }
        if (!StringUtils.hasText(templateDto.getFolder()))
        {
            throw new CusobException(ResultCodeEnum.TEMPLATE_FOLDER_EMPTY);
        }
        // TODO 其他参数校验
    }
}
