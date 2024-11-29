package com.cusob.controller;

import com.cusob.dto.TemplateDto;
import com.cusob.dto.TemplateQueryDto;
import com.cusob.entity.Template;
import com.cusob.result.Result;
import com.cusob.service.TemplateService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/template")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @ApiOperation("save customized Template")
    @PostMapping("save/customized")
    public Result saveCustomizedTemplate(@RequestBody TemplateDto templateDto){
        templateService.saveCustomizedTemplate(templateDto);
        return Result.ok();
    }

    @ApiOperation("get template by id")
    @GetMapping("get/{id}")
    public Result getTemplateById(@PathVariable Long id){
        Template template = templateService.getTemplateById(id);
        return Result.ok(template);
    }

    @ApiOperation("update Template")
    @PostMapping("update")
    public Result updateTemplate(@RequestBody TemplateDto templateDto){
        templateService.updateTemplate(templateDto);
        return Result.ok();
    }

    @ApiOperation("get template List Group By Folder")
    @GetMapping("getList")
    public Result getListGroupByFolder(TemplateQueryDto templateQueryDto){
        Map<String, List<Template>> map = templateService.getListGroupByFolder(templateQueryDto);
        return Result.ok(map);
    }

    @ApiOperation("get Folder List")
    @GetMapping("getFolderList")
    public Result getFolderList(){
        List<String> folderList = templateService.getFolderList();
        return Result.ok(folderList);
    }

    @ApiOperation("get Template List By Folder")
    @GetMapping("getTemplateListByFolder")
    public Result getTemplateListByFolder(String folder ,String keyword){
        List<Template> templateList = templateService.getTemplateListByFolder(folder, keyword);
        return Result.ok(templateList);
    }

    @ApiOperation("remove Customized Template")
    @DeleteMapping("remove/{id}")
    public Result removeCustomizedTemplate(@PathVariable Long id){
        templateService.removeCustomizedTemplate(id);
        return Result.ok();
    }
}
