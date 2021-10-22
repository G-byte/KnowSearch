package com.didichuxing.datachannel.arius.admin.biz.workorder.handler;

import com.alibaba.fastjson.JSON;
import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.constant.result.ResultType;
import com.didichuxing.datachannel.arius.admin.client.constant.workorder.WorkOrderTypeEnum;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.arius.AriusUserInfo;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.template.IndexTemplateLogic;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.workorder.WorkOrder;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.workorder.detail.AbstractOrderDetail;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.workorder.detail.TemplateIndecreaseOrderDetail;
import com.didichuxing.datachannel.arius.admin.common.bean.po.order.WorkOrderPO;
import com.didichuxing.datachannel.arius.admin.common.exception.AdminOperateException;
import com.didichuxing.datachannel.arius.admin.common.util.AriusObjUtils;
import com.didichuxing.datachannel.arius.admin.common.util.ConvertUtil;
import com.didichuxing.datachannel.arius.admin.biz.template.TemplateAction;
import com.didichuxing.datachannel.arius.admin.core.service.template.logic.TemplateLogicService;
import com.didichuxing.datachannel.arius.admin.biz.template.srv.quota.TemplateQuotaManager;
import com.didichuxing.datachannel.arius.admin.biz.workorder.BaseWorkOrderHandler;
import com.didichuxing.datachannel.arius.admin.biz.workorder.content.TemplateIndecreaseContent;
import com.didichuxing.datachannel.arius.admin.biz.workorder.notify.TemplateIndecencyNotify;
import com.didichuxing.datachannel.arius.admin.metadata.service.TemplateLabelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import static com.didichuxing.datachannel.arius.admin.core.notify.NotifyTaskTypeEnum.WORK_ORDER_TEMPLATE_INDECREASE;

/**
 * @author d06679
 * @date 2019/4/29
 */
@Service("templateIndecreaseHandler")
public class TemplateIndecreaseHandler extends BaseWorkOrderHandler {

    private static final Logger        LOGGER = LoggerFactory.getLogger(TemplateIndecreaseHandler.class);

    @Autowired
    private TemplateLabelService        templateLabelService;

    @Autowired
    private TemplateLogicService       templateLogicService;

    @Autowired
    private TemplateAction             templateAction;

    @Autowired
    private TemplateQuotaManager templateQuotaManager;

    /**
     * 工单是否自动审批
     *
     * @param workOrder 工单类型
     * @return result
     */
    @Override
    public boolean canAutoReview(WorkOrder workOrder) {
        return true;
    }

    @Override
    public AbstractOrderDetail getOrderDetail(String extensions) {
        TemplateIndecreaseContent content = JSON.parseObject(extensions, TemplateIndecreaseContent.class);

        return ConvertUtil.obj2Obj(content, TemplateIndecreaseOrderDetail.class);
    }

    @Override
    public List<AriusUserInfo> getApproverList(AbstractOrderDetail detail) {
        return getRDOrOPList();
    }

    @Override
    public Result checkAuthority(WorkOrderPO orderPO, String userName) {
        if (isRDOrOP(userName)) {
            return Result.buildSucc(true);
        }
        return Result.buildFail(ResultType.OPERATE_FORBIDDEN_ERROR.getMessage());
    }

    /**************************************** protected method ******************************************/

    /**
     * 验证用户提供的参数
     *
     * @param workOrder 工单
     * @return result
     */
    @Override
    protected Result validateConsoleParam(WorkOrder workOrder) {
        TemplateIndecreaseContent content = ConvertUtil.obj2ObjByJSON(workOrder.getContentObj(),
            TemplateIndecreaseContent.class);

        if (AriusObjUtils.isNull(content.getId())) {
            return Result.buildParamIllegal("索引id为空");
        }

        if (AriusObjUtils.isNull(content.getName())) {
            return Result.buildParamIllegal("索引名字为空");
        }

        if (AriusObjUtils.isNull(content.getExpectQuota())) {
            return Result.buildParamIllegal("索引Quota为空");
        }

        return Result.buildSucc();
    }

    @Override
    protected String getTitle(WorkOrder workOrder) {
        TemplateIndecreaseContent content = ConvertUtil.obj2ObjByJSON(workOrder.getContentObj(),
            TemplateIndecreaseContent.class);

        WorkOrderTypeEnum workOrderTypeEnum = WorkOrderTypeEnum.valueOfName(workOrder.getType());
        if (workOrderTypeEnum == null) {
            return "";
        }
        return content.getName() + workOrderTypeEnum.getMessage();
    }

    /**
     * 验证用户是否有该工单权限
     *
     * @param workOrder 工单内容
     * @return result
     */
    @Override
    protected Result validateConsoleAuth(WorkOrder workOrder) {
        TemplateIndecreaseContent content = ConvertUtil.obj2ObjByJSON(workOrder.getContentObj(),
            TemplateIndecreaseContent.class);

        if (templateLabelService.isImportantIndex(content.getId())) {
            return Result.buildOpForBidden("禁止操作重要索引，请联系Arius服务号处理");
        }

        IndexTemplateLogic templateLogic = templateLogicService.getLogicTemplateById(content.getId());
        if (!templateLogic.getAppId().equals(workOrder.getSubmitorAppid())) {
            return Result.buildOpForBidden("您无权对该索引进行扩缩容操作");
        }

        return Result.buildSucc();
    }

    /**
     * 验证平台参数
     *
     * @param workOrder 工单内容
     * @return result
     */
    @Override
    protected Result validateParam(WorkOrder workOrder) {
        return Result.buildSucc();
    }

    /**
     * 处理工单
     *
     * @param workOrder 工单
     * @return result
     */
    @Override
    protected Result doProcessAgree(WorkOrder workOrder, String approver) throws AdminOperateException {
        TemplateIndecreaseContent content = ConvertUtil.obj2ObjByJSON(workOrder.getContentObj(),
            TemplateIndecreaseContent.class);

        // 修改模板quota及保存时长信息
        Result result = templateAction.indecreaseWithAutoDistributeResource(content.getId(),
            content.getExpectExpireTime(), content.getExpectQuota(), workOrder.getSubmitor());

        //Quota更新
        if (!templateQuotaManager.controlAndPublish(content.getId())) {
            LOGGER.warn(
                "class=TemplateIndecreaseHandler||method=doProcessAgree||templateLogicId={}||msg=template quota publish failed!",
                content.getId());
        }

        sendNotify(WORK_ORDER_TEMPLATE_INDECREASE,
            new TemplateIndecencyNotify(workOrder.getSubmitorAppid(), content.getName()),
            Arrays.asList(workOrder.getSubmitor()));

        return result;
    }
}
