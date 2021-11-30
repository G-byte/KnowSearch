package com.didichuxing.datachannel.arius.admin.biz.workorder.handler.clusterReStart;

import com.alibaba.fastjson.JSON;
import com.didichuxing.datachannel.arius.admin.biz.workorder.content.clusterOpRestart.ClusterOpRestartContent;
import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.constant.result.ResultType;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.arius.AriusUserInfo;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.workorder.WorkOrder;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.workorder.detail.AbstractOrderDetail;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.workorder.detail.clusterOpRestart.ClusterOpRestartOrderDetail;
import com.didichuxing.datachannel.arius.admin.common.bean.po.order.WorkOrderPO;
import com.didichuxing.datachannel.arius.admin.common.util.ConvertUtil;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.ecm.EcmHandleService;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.physic.ClusterPhyService;
import com.didichuxing.datachannel.arius.admin.biz.workorder.BaseWorkOrderHandler;
import com.didichuxing.datachannel.arius.admin.biz.worktask.WorkTaskManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public abstract class ClusterOpRestartHandler extends BaseWorkOrderHandler {

    @Autowired
    protected ClusterPhyService esClusterPhyService;

    @Autowired
    protected EcmHandleService ecmHandleService;

    @Autowired
    protected WorkTaskManager workTaskManager;

    @Override
    protected Result validateConsoleAuth(WorkOrder workOrder) {
        if (!isOP(workOrder.getSubmitor())) {
            return Result.buildOpForBidden("非运维人员不能操作集群扩缩容！");
        }

        return Result.buildSucc();
    }

    @Override
    protected Result<Void> validateParam(WorkOrder workOrder) {
        return Result.buildSucc();
    }

    @Override
    public boolean canAutoReview(WorkOrder workOrder) {
        return false;
    }

    @Override
    public AbstractOrderDetail getOrderDetail(String extensions) {
        ClusterOpRestartContent content = JSON.parseObject(extensions, ClusterOpRestartContent.class);

        return ConvertUtil.obj2Obj(content, ClusterOpRestartOrderDetail.class);
    }

    @Override
    public List<AriusUserInfo> getApproverList(AbstractOrderDetail detail) {
        return getOPList();
    }

    @Override
    public Result checkAuthority(WorkOrderPO orderPO, String userName) {
        if (isOP(userName)) {
            return Result.buildSucc(true);
        }
        return Result.buildFail(ResultType.OPERATE_FORBIDDEN_ERROR.getMessage());
    }
}
