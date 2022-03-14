package com.didichuxing.datachannel.arius.admin.rest.controller.v3.op.task;

import static com.didichuxing.datachannel.arius.admin.common.constant.ApiVersion.V3_OP;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.didichuxing.datachannel.arius.admin.biz.template.srv.dcdr.TemplateDcdrManager;
import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.template.DCDRMasterSlaveSwitchDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.task.WorkTaskVO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.template.DCDRSingleTemplateMasterSlaveSwitchDetailVO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.template.DCDRTasksDetailVO;
import com.didichuxing.datachannel.arius.admin.common.exception.ESOperateException;
import com.didichuxing.datachannel.arius.admin.common.util.HttpRequestUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping(V3_OP + "/dcdr/work-order/task")
@Api(tags = "DCDR任务相关接口(REST)")
public class OpTaskDCDRController {

    @Autowired
    private TemplateDcdrManager templateDcdrManager;

    @GetMapping("/{taskId}/detail")
    @ResponseBody
    @ApiOperation(value = "获取dcdr主从切换任务详情")
    public Result<DCDRTasksDetailVO> getDCDRMasterSlaveSwitchDetailVO(@PathVariable("taskId") Integer taskId) {
        return templateDcdrManager.getDCDRMasterSlaveSwitchDetailVO(taskId);
    }

    @GetMapping("/{taskId}/{templateId}/detail")
    @ResponseBody
    @ApiOperation(value = "获取模板dcdr主从切换任务详情")
    public Result<DCDRSingleTemplateMasterSlaveSwitchDetailVO> getDCDRSingleTemplateMasterSlaveSwitchDetailVO(@PathVariable("taskId") Integer taskId,
                                                                                                              @PathVariable("templateId") Long templateId) {
        return templateDcdrManager.getDCDRSingleTemplateMasterSlaveSwitchDetailVO(taskId, templateId);
    }

    @PostMapping("/switchMasterSlave")
    @ResponseBody
    @ApiOperation(value = "DCDR主从切换接口", notes = "")
    public Result<WorkTaskVO> dcdrSwitchMasterSlave(HttpServletRequest request,
                                                    @RequestBody DCDRMasterSlaveSwitchDTO dcdrMasterSlaveSwitchDTO) {
        return templateDcdrManager.batchDcdrSwitchMaster2Slave(dcdrMasterSlaveSwitchDTO,
            HttpRequestUtils.getOperator(request));
    }

    @DeleteMapping("/{taskId}/{templateIds}/cancel")
    @ResponseBody
    @ApiOperation(value = "根据任务id和模板id取消单个DCDR主从切换任务", notes = "")
    public Result<Void> cancelDcdrSwitchMasterSlaveByTaskIdAndTemplateIds(HttpServletRequest request,
                                                                          @PathVariable("taskId") Integer taskId,
                                                                          @PathVariable("templateIds") List<Long> templateIds) throws ESOperateException {
        return templateDcdrManager.cancelDcdrSwitchMasterSlaveByTaskIdAndTemplateIds(taskId, templateIds, false,
            HttpRequestUtils.getOperator(request));
    }

    @DeleteMapping("/{taskId}/cancel")
    @ResponseBody
    @ApiOperation(value = "根据任务id取消全量DCDR主从切换任务")
    public Result<Void> cancelDcdrSwitchMasterSlaveByTaskId(HttpServletRequest request,
                                                            @PathVariable("taskId") Integer taskId) throws ESOperateException {
        return templateDcdrManager.cancelDcdrSwitchMasterSlaveByTaskId(taskId, HttpRequestUtils.getOperator(request));
    }

    @PostMapping("/{taskId}/{templateId}/refresh")
    @ResponseBody
    @ApiOperation(value = "刷新dcdr链路任务")
    public Result<Void> refreshDcdrChannelState(@PathVariable("taskId") Integer taskId,
                                                @PathVariable("templateId") Integer templateId) {
        return templateDcdrManager.refreshDcdrChannelState(taskId, templateId, null);
    }

    @PutMapping("/{taskId}/{templateId}/forceSwitch")
    @ResponseBody
    @ApiOperation(value = "dcdr主从强制切换接口")
    public Result<Void> forceSwitchMasterSlave(HttpServletRequest request,
                                               @PathVariable("taskId") Integer taskId,
                                               @PathVariable ("templateId") Integer templateId) {
        return templateDcdrManager.forceSwitchMasterSlave(taskId, templateId, HttpRequestUtils.getOperator(request));
    }
}
