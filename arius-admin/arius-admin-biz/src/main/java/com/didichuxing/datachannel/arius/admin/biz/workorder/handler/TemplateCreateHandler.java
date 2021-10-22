package com.didichuxing.datachannel.arius.admin.biz.workorder.handler;

import com.alibaba.fastjson.JSON;
import com.didichuxing.datachannel.arius.admin.client.bean.common.LogicResourceConfig;
import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.template.IndexTemplateLogicDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.template.IndexTemplatePhysicalDTO;
import com.didichuxing.datachannel.arius.admin.client.constant.operaterecord.OperationEnum;
import com.didichuxing.datachannel.arius.admin.client.constant.quota.NodeSpecifyEnum;
import com.didichuxing.datachannel.arius.admin.client.constant.resource.ResourceLogicTypeEnum;
import com.didichuxing.datachannel.arius.admin.client.constant.result.ResultType;
import com.didichuxing.datachannel.arius.admin.client.constant.template.TemplateDeployRoleEnum;
import com.didichuxing.datachannel.arius.admin.client.constant.workorder.WorkOrderTypeEnum;
import com.didichuxing.datachannel.arius.admin.client.mapping.AriusTypeProperty;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.arius.AriusUserInfo;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ESClusterLogic;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.workorder.WorkOrder;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.workorder.detail.AbstractOrderDetail;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.workorder.detail.TemplateCreateOrderDetail;
import com.didichuxing.datachannel.arius.admin.common.bean.po.order.WorkOrderPO;
import com.didichuxing.datachannel.arius.admin.common.constant.AdminConstant;
import com.didichuxing.datachannel.arius.admin.common.exception.AdminOperateException;
import com.didichuxing.datachannel.arius.admin.common.util.AriusObjUtils;
import com.didichuxing.datachannel.arius.admin.common.util.AriusOptional;
import com.didichuxing.datachannel.arius.admin.common.util.ConvertUtil;
import com.didichuxing.datachannel.arius.admin.common.util.TemplateUtils;
import com.didichuxing.datachannel.arius.admin.biz.template.TemplateAction;
import com.didichuxing.datachannel.arius.admin.core.component.QuotaTool;
import com.didichuxing.datachannel.arius.admin.core.service.app.AppLogicClusterAuthService;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.logic.ESClusterLogicService;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.physic.ESClusterPhyService;
import com.didichuxing.datachannel.arius.admin.core.service.common.AriusConfigInfoService;
import com.didichuxing.datachannel.arius.admin.core.service.template.logic.TemplateLogicService;
import com.didichuxing.datachannel.arius.admin.biz.template.srv.mapping.TemplateLogicMappingManager;
import com.didichuxing.datachannel.arius.admin.biz.template.srv.mapping.TemplatePhyMappingManager;
import com.didichuxing.datachannel.arius.admin.biz.workorder.BaseWorkOrderHandler;
import com.didichuxing.datachannel.arius.admin.biz.workorder.content.TemplateCreateContent;
import com.didichuxing.datachannel.arius.admin.biz.workorder.notify.TemplateCreateNotify;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.didichuxing.datachannel.arius.admin.common.constant.AdminConstant.DEFAULT_TYPE;
import static com.didichuxing.datachannel.arius.admin.common.constant.AdminConstant.G_PER_SHARD;
import static com.didichuxing.datachannel.arius.admin.common.constant.AriusConfigConstant.ARIUS_COMMON_GROUP;
import static com.didichuxing.datachannel.arius.admin.core.component.QuotaTool.TEMPLATE_QUOTA_MIN;
import static com.didichuxing.datachannel.arius.admin.core.notify.NotifyTaskTypeEnum.WORK_ORDER_TEMPLATE_CREATE;
import static com.didichuxing.datachannel.arius.admin.core.service.template.physic.impl.TemplatePhyServiceImpl.NOT_CHECK;

/**
 * @author d06679
 * @date 2019/4/29
 */
@Service("templateCreateHandler")
public class TemplateCreateHandler extends BaseWorkOrderHandler {

    @Autowired
    private TemplateLogicService        templateLogicService;

    @Autowired
    private QuotaTool                   quotaTool;

    @Autowired
    private TemplatePhyMappingManager   templatePhyMappingManager;

    @Autowired
    private TemplateLogicMappingManager templateLogicMappingManager;

    @Autowired
    private TemplateAction              templateAction;

    @Autowired
    private ESClusterLogicService       esClusterLogicService;

    @Autowired
    private AriusConfigInfoService      ariusConfigInfoService;

    @Autowired
    private AppLogicClusterAuthService  logicClusterAuthService;

    /**
     * 工单是否自动审批
     *
     * @param workOrder 工单类型
     * @return result
     */
    @Override
    public boolean canAutoReview(WorkOrder workOrder) {
        TemplateCreateContent content = ConvertUtil.obj2ObjByJSON(workOrder.getContentObj(),
            TemplateCreateContent.class);
        ESClusterLogic esClusterLogic = esClusterLogicService.getLogicClusterById(content.getResourceId());

        if (!esClusterLogic.getType().equals(ResourceLogicTypeEnum.PUBLIC.getCode())) {
            return false;
        }

        LogicResourceConfig resourceConfig = esClusterLogicService
            .genLogicClusterConfig(esClusterLogic.getConfigJson());
        if (!resourceConfig.getTemplateCreateWorkOrderAutoProcess()) {
            return false;
        }

        Double autoProcessDiskMaxG = ariusConfigInfoService.doubleSetting(ARIUS_COMMON_GROUP,
            "arius.wo.auto.process.create.template.disk.maxG", 10.0);

        return content.getDiskQuota() < autoProcessDiskMaxG;
    }

    @Override
    public AbstractOrderDetail getOrderDetail(String extensions) {
        TemplateCreateContent content = JSON.parseObject(extensions, TemplateCreateContent.class);

        return ConvertUtil.obj2Obj(content, TemplateCreateOrderDetail.class);
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
        TemplateCreateContent content = ConvertUtil.obj2ObjByJSON(workOrder.getContentObj(),
            TemplateCreateContent.class);

        if (AriusObjUtils.isNull(content.getResponsible())) {
            return Result.buildParamIllegal("责任人为空");
        }

        if (AriusObjUtils.isNull(content.getCyclicalRoll())) {
            return Result.buildParamIllegal("索引分区设置为空");
        }

        if (AriusObjUtils.isNull(content.getDiskQuota())) {
            return Result.buildParamIllegal("索引数据总量为空");
        }

        ESClusterLogic esClusterLogic = esClusterLogicService.getLogicClusterById(content.getResourceId());
        if (esClusterLogic == null) {
            return Result.buildParamIllegal("集群不存在");
        }

        if (content.getCyclicalRoll()) {
            if (AriusObjUtils.isNull(content.getDateField())) {
                return Result.buildParamIllegal("分区字段为空");
            }
        }

        // 集群权限检查
        if (!logicClusterAuthService.canCreateLogicTemplate(workOrder.getSubmitorAppid(), content.getResourceId())) {
            return Result.buildFail(
                String.format("APP[%s]没有在逻辑集群[%s]下创建模板的权限", workOrder.getSubmitorAppid(), content.getResourceId()));
        }

        Result checkBaseInfoResult = templateLogicService
            .validateTemplate(buildTemplateLogicDTO(content, workOrder.getSubmitorAppid()), OperationEnum.ADD);
        if (checkBaseInfoResult.failed()) {
            return checkBaseInfoResult;
        }

        if (content.getMapping() != null) {
            Result checkMapping = templatePhyMappingManager.checkMappingForNew(content.getName(),
                genTypeProperty(content.getMapping()));
            if (checkMapping.failed()) {
                return checkMapping;
            }
        }

        // 校验数据中心是否匹配
        if (!content.getDataCenter().equals(esClusterLogic.getDataCenter())) {
            return Result.buildParamIllegal("集群数据中心不符");
        }

        return Result.buildSucc();
    }

    @Override
    protected String getTitle(WorkOrder workOrder) {
        TemplateCreateContent content = ConvertUtil.obj2ObjByJSON(workOrder.getContentObj(),
            TemplateCreateContent.class);

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
        if (!isOP(workOrder.getSubmitor())) {
            return Result.buildOpForBidden("非运维人员不能操作集群扩缩容！");
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
     * 处理工单 该工单需要运维人员在运维控制台处理好后,再到处理;所以这里不用执行任何逻辑
     *
     * @param workOrder 工单
     * @return result
     */
    @Override
    protected Result doProcessAgree(WorkOrder workOrder, String approver) throws AdminOperateException {
        TemplateCreateContent content = ConvertUtil.obj2ObjByJSON(workOrder.getContentObj(),
            TemplateCreateContent.class);

        IndexTemplateLogicDTO logicDTO = buildTemplateLogicDTO(content, workOrder.getSubmitorAppid());

        logicDTO.setPhysicalInfos(Lists.newArrayList(buildTemplatePhysicalDTO(content, logicDTO)));
        logicDTO.setShardNum(fetchMaxShardNum(logicDTO.getPhysicalInfos()));

        Result<Integer> result = templateAction.createWithAutoDistributeResource(logicDTO, workOrder.getSubmitor());

        if (result.success()) {
            // 模板创建成功，如果设置了mapping，更新mapping
            if (StringUtils.isNoneBlank(content.getMapping()) && templateLogicMappingManager
                .updateMappingForNew(result.getData(), genTypeProperty(content.getMapping())).failed()) {
                throw new AdminOperateException("设置mapping失败");
            }

            sendNotify(WORK_ORDER_TEMPLATE_CREATE,
                new TemplateCreateNotify(workOrder.getSubmitorAppid(), content.getName()),
                Arrays.asList(workOrder.getSubmitor()));
        }

        return result;
    }

    /**************************************** private method ****************************************************/
    /**
     * 获取逻辑模板下所有物理模板最大shard值
     * @param physicals 物理模板列表
     * @return
     */
    private int fetchMaxShardNum(List<IndexTemplatePhysicalDTO> physicals) {
        int maxShardNum = -1;
        if (CollectionUtils.isNotEmpty(physicals)) {
            for (IndexTemplatePhysicalDTO physical : physicals) {
                if (physical.getShard() != null && physical.getShard() > maxShardNum) {
                    maxShardNum = physical.getShard();
                }
            }
        }
        return maxShardNum;
    }

    /**
     * 根据工单内容构建逻辑模板DTO
     * @param content 工单内容
     * @param submitorAppid appid
     * @return dto
     */
    private IndexTemplateLogicDTO buildTemplateLogicDTO(TemplateCreateContent content, Integer submitorAppid) {
        IndexTemplateLogicDTO logicDTO = ConvertUtil.obj2Obj(content, IndexTemplateLogicDTO.class);

        if (!content.getCyclicalRoll()) {
            // 不周期滚动
            logicDTO.setExpression(logicDTO.getName());
            logicDTO.setDateFormat("");
            logicDTO.setExpireTime(-1);
            logicDTO.setDateField("");
        } else {
            // 周期滚动
            logicDTO.setExpression(logicDTO.getName() + "*");

            // 数据不会过期，必须按月滚动
            if (content.getExpireTime() < 0) {
                logicDTO.setDateFormat(AdminConstant.YY_MM_DATE_FORMAT);
            } else {
                //每天的数据增量大于200G或者保存时长小于30天 按天存储
                double incrementPerDay = content.getDiskQuota() / content.getExpireTime();
                if (incrementPerDay >= 200.0 || content.getExpireTime() <= 30) {
                    if (StringUtils.isNotBlank(logicDTO.getDateField())
                        && !AdminConstant.MM_DD_DATE_FORMAT.equals(logicDTO.getDateField())) {
                        logicDTO.setDateFormat(AdminConstant.YY_MM_DD_DATE_FORMAT);
                    }
                } else {
                    logicDTO.setDateFormat(AdminConstant.YY_MM_DATE_FORMAT);
                }
            }
        }

        if (content.getDiskQuota() < 0) {
            content.setDiskQuota(1024.0);
        }

        if (content.getPreCreateFlags() == null) {
            content.setPreCreateFlags(AdminConstant.DEFAULT_PRE_CREATE_FLAGS);
        }

        if (content.getDisableSourceFlags() == null) {
            content.setDisableSourceFlags(AdminConstant.DISABLE_SOURCE_FLAGS);
        }

        if (content.getShardNum() == null) {
            content.setShardNum(AdminConstant.DEFAULT_SHARD_NUM);
        }

        logicDTO.setQuota(quotaTool.getQuotaCountByDisk(NodeSpecifyEnum.DOCKER.getCode(), content.getDiskQuota() * 1.2,
            TEMPLATE_QUOTA_MIN));
        logicDTO.setAppId(submitorAppid);
        return logicDTO;
    }

    /**
     * 根据工单内容构建物理模板DTO
     * 情融说：当前用户控制台不支持主从部署，所以这里每个逻辑模板知会对应一个物理模板角色是为master
     * @param content 工单内容
     * @param logicDTO 逻辑模板DTO
     * @return dto
     */
    private IndexTemplatePhysicalDTO buildTemplatePhysicalDTO(TemplateCreateContent content,
                                                              IndexTemplateLogicDTO logicDTO) {
        IndexTemplatePhysicalDTO physicalDTO = new IndexTemplatePhysicalDTO();

        physicalDTO.setLogicId(NOT_CHECK);
        physicalDTO.setName(logicDTO.getName());
        physicalDTO.setExpression(logicDTO.getExpression());
        physicalDTO.setGroupId(UUID.randomUUID().toString());
        physicalDTO.setRole(TemplateDeployRoleEnum.MASTER.getCode());
        physicalDTO.setResourceId(content.getResourceId());
        physicalDTO.setCluster(content.getCluster());
        physicalDTO.setRack(content.getRack());

        setTemplateShard(physicalDTO, content, logicDTO);

        return physicalDTO;
    }

    private Integer genShardNumBySize(Double size) {
        double shardNumCeil = Math.ceil(size / G_PER_SHARD);
        return (int) shardNumCeil;
    }

    private AriusTypeProperty genTypeProperty(String mapping) {
        AriusTypeProperty typeProperty = new AriusTypeProperty();
        typeProperty.setTypeName(DEFAULT_TYPE);
        // 未指定mapping，设置为{}以符合json格式
        if (StringUtils.isBlank(mapping)) {
            mapping = "{}";
        }
        typeProperty.setProperties(JSON.parseObject(mapping));
        return typeProperty;
    }

    private void setTemplateShard(IndexTemplatePhysicalDTO physicalDTO, TemplateCreateContent content,
                                  IndexTemplateLogicDTO logicDTO) {
        if (content.getCyclicalRoll()) {
            int expireTime = content.getExpireTime();
            if (expireTime < 0) {
                // 如果数据永不过期，平台会按着180天来计算每日数据增量，最终用于生成模板shard
                expireTime = 180;
            }

            if (TemplateUtils.isSaveByDay(logicDTO.getDateFormat())) {
                // 按天滚动
                physicalDTO.setShard(genShardNumBySize(content.getDiskQuota() / expireTime));
            } else {
                // 按月滚动
                physicalDTO.setShard(genShardNumBySize((content.getDiskQuota() / expireTime) * 30));
            }
        } else {
            physicalDTO.setShard(genShardNumBySize(content.getDiskQuota()));
        }
    }

}
