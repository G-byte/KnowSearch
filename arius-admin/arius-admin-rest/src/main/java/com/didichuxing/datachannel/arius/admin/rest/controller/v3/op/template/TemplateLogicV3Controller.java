package com.didichuxing.datachannel.arius.admin.rest.controller.v3.op.template;

import static com.didichuxing.datachannel.arius.admin.common.constant.ApiVersion.V3_OP;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.didichuxing.datachannel.arius.admin.biz.template.srv.setting.TemplateLogicSettingsManager;
import com.didichuxing.datachannel.arius.admin.client.bean.common.PaginationResult;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.template.TemplateSettingDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.template.TemplateSettingVO;
import com.didichuxing.datachannel.arius.admin.common.exception.AdminOperateException;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.didichuxing.datachannel.arius.admin.biz.template.TemplateLogicManager;
import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.template.TemplateConditionDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.template.ConsoleTemplateVO;
import com.didichuxing.datachannel.arius.admin.common.util.HttpRequestUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Created by linyunan on 2021-07-30
 */
@RestController
@RequestMapping(V3_OP + "/template/logic")
@Api(tags = "逻辑模板接口(REST)")
public class TemplateLogicV3Controller {

    @Autowired
    private TemplateLogicManager templateLogicManager;

    @Autowired
    private TemplateLogicSettingsManager templateLogicSettingsManager;

    @GetMapping("/listNames")
    @ResponseBody
    @ApiOperation(value = "获取逻辑模板名称列表接口")
    public Result<List<String>> listTemplateLogicNames(HttpServletRequest request) {
        return Result.buildSucc(templateLogicManager.getTemplateLogicNames(HttpRequestUtils.getAppId(request)));
    }

    @PostMapping("/page")
    @ResponseBody
    @ApiOperation(value = "模糊查询模板列表")
    public PaginationResult<ConsoleTemplateVO> pageGetConsoleTemplateVOS(HttpServletRequest request,
                                                                         @RequestBody TemplateConditionDTO condition) {
        return templateLogicManager.pageGetConsoleTemplateVOS(condition, HttpRequestUtils.getAppId(request));
    }

    @GetMapping("/{templateName}/nameCheck")
    @ResponseBody
    @ApiOperation(value = "校验模板名称是否合法")
    public Result<Void> checkTemplateValidForCreate(@PathVariable("templateName") String templateName) {
        return templateLogicManager.checkTemplateValidForCreate(templateName);
    }

    @GetMapping("/{templateId}/checkEditMapping/")
    @ResponseBody
    @ApiOperation(value = "校验可否编辑模板mapping")
    public Result<Boolean> checkTemplateEditMapping(@PathVariable Integer templateId) {
        return templateLogicManager.checkTemplateEditMapping(templateId);
    }

    @GetMapping("/{templateId}/{templateSrvId}/checkEditTemplateSrv/")
    @ResponseBody
    @ApiOperation(value = "校验模板是否可以使用指定的索引模板服务，例如是否可以编辑mapping,setting等")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "templateId", dataType = "Integer", value = "逻辑模板id", required = true),
            @ApiImplicitParam(name = "templateSrvId", dataType = "Integer", value = "索引模板服务的id，例如mapping设置", required = true)
    })
    public Result<Boolean> checkTemplateEditService(@PathVariable("templateId") Integer templateId,
                                                    @PathVariable("templateSrvId") Integer templateSrvId) {
        return templateLogicManager.checkTemplateEditService(templateId, templateSrvId);
    }

    @PutMapping("/rollover/switch/{templateLogicId}/{status}")
    @ResponseBody
    @ApiOperation(value = "更改逻辑模版的rollover能力")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "templateLogicId", dataType = "Integer", value = "逻辑模版id", required = true),
            @ApiImplicitParam(name = "status", dataType = "Integer", value = "停启rollover能力（1 启用，0 禁用）", required = true)
    })
    public Result<Void> switchRolloverStatus(@PathVariable Integer templateLogicId, @PathVariable Integer status,
                                               HttpServletRequest request) {
        String operator = HttpRequestUtils.getOperator(request);
        return templateLogicManager.switchRolloverStatus(templateLogicId, status, operator);
    }

    @PutMapping("/setting")
    @ResponseBody
    @ApiOperation(value = "更新索引Setting接口", notes = "")
    @ApiImplicitParams({@ApiImplicitParam(paramType = "header", dataType = "String", name = "X-ARIUS-APP-ID", value = "应用ID", required = true)})
    public Result<Void> customizeSetting(HttpServletRequest request,
                                      @RequestBody TemplateSettingDTO settingDTO) throws AdminOperateException {
        Result<Void> checkAuthResult = templateLogicManager.checkAppAuthOnLogicTemplate(settingDTO.getLogicId(), HttpRequestUtils.getAppId(request));
        if (checkAuthResult.failed()) {
            return checkAuthResult;
        }

        return templateLogicSettingsManager.customizeSetting(settingDTO, HttpRequestUtils.getOperator(request));
    }

    @GetMapping("/setting")
    @ResponseBody
    @ApiOperation(value = "获取索引Setting接口", notes = "")
    @ApiImplicitParams({@ApiImplicitParam(paramType = "query", dataType = "Integer", name = "logicId", value = "索引ID", required = true)})
    public Result<TemplateSettingVO> getTemplateSettings(@RequestParam("logicId") Integer logicId) throws AdminOperateException {
        return templateLogicSettingsManager.buildTemplateSettingVO(logicId);
    }

    @GetMapping("/listTemplates")
    @ResponseBody
    @ApiOperation(value = "根据物理集群名称获取对应全量逻辑模板列表", notes = "")
    public Result<List<ConsoleTemplateVO>> getLogicTemplatesByCluster(HttpServletRequest request,
                                                                      @RequestParam("cluster") String cluster) {
        return templateLogicManager.getTemplateVOByPhyCluster(cluster, HttpRequestUtils.getAppId(request));
    }
}
