package com.didichuxing.datachannel.arius.admin.biz.template.impl;

import static com.didichuxing.datachannel.arius.admin.client.constant.operaterecord.ModuleEnum.TEMPLATE;
import static com.didichuxing.datachannel.arius.admin.client.constant.operaterecord.OperationEnum.*;
import static com.didichuxing.datachannel.arius.admin.client.constant.template.TemplateDeployRoleEnum.MASTER;
import static com.didichuxing.datachannel.arius.admin.client.constant.template.TemplateDeployRoleEnum.SLAVE;
import static com.didichuxing.datachannel.arius.admin.common.constant.AdminConstant.MILLIS_PER_DAY;
import static com.didichuxing.datachannel.arius.admin.common.constant.AriusConfigConstant.ARIUS_COMMON_GROUP;
import static com.didichuxing.datachannel.arius.admin.common.constant.cache.CacheGlobalNamesContent.CACHE_GLOBAL_NAME;
import static com.didichuxing.datachannel.arius.admin.common.util.IndexNameFactory.genIndexNameClear;
import static com.didichuxing.datachannel.arius.admin.persistence.constant.ESOperateContant.INDEX_SHARD_NUM;
import static com.didichuxing.datachannel.arius.admin.persistence.constant.ESOperateContant.TEMPLATE_INDEX_INCLUDE_RACK;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.didichuxing.datachannel.arius.admin.biz.template.TemplatePhyManager;
import com.didichuxing.datachannel.arius.admin.biz.template.srv.precreate.TemplatePreCreateManager;
import com.didichuxing.datachannel.arius.admin.biz.template.srv.shard.TemplateShardManager;
import com.didichuxing.datachannel.arius.admin.client.bean.common.IndexTemplatePhysicalConfig;
import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.template.IndexTemplateLogicDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.template.IndexTemplatePhysicalDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.template.TemplatePhysicalCopyDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.template.TemplatePhysicalUpgradeDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.template.ConsoleTemplatePhyVO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.template.IndexTemplatePhysicalVO;
import com.didichuxing.datachannel.arius.admin.client.constant.app.AppTemplateAuthEnum;
import com.didichuxing.datachannel.arius.admin.client.constant.operaterecord.OperationEnum;
import com.didichuxing.datachannel.arius.admin.client.constant.result.ResultType;
import com.didichuxing.datachannel.arius.admin.client.constant.template.TemplateDeployRoleEnum;
import com.didichuxing.datachannel.arius.admin.client.constant.template.TemplatePhysicalStatusEnum;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ClusterPhy;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.template.IndexTemplateLogic;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.template.IndexTemplateLogicWithPhyTemplates;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.template.IndexTemplatePhy;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.template.IndexTemplatePhyWithLogic;
import com.didichuxing.datachannel.arius.admin.common.component.SpringTool;
import com.didichuxing.datachannel.arius.admin.common.event.template.PhysicalTemplateAddEvent;
import com.didichuxing.datachannel.arius.admin.common.event.template.PhysicalTemplateModifyEvent;
import com.didichuxing.datachannel.arius.admin.common.exception.AdminOperateException;
import com.didichuxing.datachannel.arius.admin.common.exception.ESOperateException;
import com.didichuxing.datachannel.arius.admin.common.util.*;
import com.didichuxing.datachannel.arius.admin.core.notify.NotifyTaskTypeEnum;
import com.didichuxing.datachannel.arius.admin.core.notify.info.cluster.ClusterTemplatePhysicalMetaErrorNotifyInfo;
import com.didichuxing.datachannel.arius.admin.core.notify.info.template.TemplatePhysicalMetaErrorNotifyInfo;
import com.didichuxing.datachannel.arius.admin.core.notify.service.NotifyService;
import com.didichuxing.datachannel.arius.admin.core.service.app.AppLogicTemplateAuthService;
import com.didichuxing.datachannel.arius.admin.core.service.app.AppService;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.physic.ClusterPhyService;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.region.RegionRackService;
import com.didichuxing.datachannel.arius.admin.core.service.common.AriusConfigInfoService;
import com.didichuxing.datachannel.arius.admin.core.service.common.OperateRecordService;
import com.didichuxing.datachannel.arius.admin.core.service.es.ESTemplateService;
import com.didichuxing.datachannel.arius.admin.core.service.template.logic.TemplateLogicService;
import com.didichuxing.datachannel.arius.admin.core.service.template.physic.TemplatePhyService;
import com.didichuxing.datachannel.arius.admin.core.service.template.physic.impl.TemplatePhyServiceImpl;
import com.didichuxing.datachannel.arius.admin.metadata.service.TemplateLabelService;
import com.didiglobal.logi.elasticsearch.client.response.setting.template.TemplateConfig;
import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

@Component
public class TemplatePhyManagerImpl implements TemplatePhyManager {

    private static final ILog           LOGGER                    = LogFactory.getLog(TemplatePhyServiceImpl.class);

    public static final Integer         NOT_CHECK                 = -100;
    private static final Integer        INDEX_OP_OK               = 0;
    private static final Integer        TOMORROW_INDEX_NOT_CREATE = 1;
    private static final Integer        EXPIRE_INDEX_NOT_DELETE   = 2;
    private static final Integer        INDEX_ALL_ERR             = TOMORROW_INDEX_NOT_CREATE + EXPIRE_INDEX_NOT_DELETE;

    private static final String TEMPLATE_PHYSICAL_ID_IS_NULL = "物理模板id为空";

    private static final String TEMPLATE_PHYSICAL_NOT_EXISTS = "物理模板不存在";

    private static final String CHECK_FAIL_MSG = "check fail||msg={}";

    @Autowired
    private OperateRecordService        operateRecordService;

    @Autowired
    private ClusterPhyService           clusterPhyService;

    @Autowired
    private TemplateLabelService        templateLabelService;

    @Autowired
    private ESTemplateService           esTemplateService;

    @Autowired
    private TemplatePreCreateManager    templatePreCreateManager;

    @Autowired
    private TemplateShardManager        templateShardManager;

    @Autowired
    private RegionRackService           regionRackService;

    @Autowired
    private TemplateLogicService        templateLogicService;

    @Autowired
    private TemplatePhyService          templatePhyService;

    @Autowired
    private NotifyService               notifyService;

    @Autowired
    private AriusConfigInfoService      ariusConfigInfoService;

    @Autowired
    private AppLogicTemplateAuthService appLogicTemplateAuthService;

    @Autowired
    private AppService                  appService;

    @Override
    public boolean checkMeta() {
        List<IndexTemplatePhy> templatePhysicals = templatePhyService.listTemplate();

        List<IndexTemplateLogic> templateLogics = templateLogicService.getAllLogicTemplates();
        Map<Integer, IndexTemplateLogic> logicId2IndexTemplateLogicMap = ConvertUtil.list2Map(templateLogics,
                IndexTemplateLogic::getId);

        Multimap<String, IndexTemplatePhy> cluster2IndexTemplatePhysicalMultiMap = ConvertUtil
                .list2MulMap(templatePhysicals, IndexTemplatePhy::getCluster);

        Set<String> esClusters = clusterPhyService.listAllClusters().stream().map( ClusterPhy::getCluster)
                .collect( Collectors.toSet());

        for (String cluster : cluster2IndexTemplatePhysicalMultiMap.keySet()) {
            int tomorrowIndexNotCreateCount = 0;
            int expireIndexNotDeleteCount = 0;

            Collection<IndexTemplatePhy> clusterTemplates = cluster2IndexTemplatePhysicalMultiMap.get(cluster);

            for (IndexTemplatePhy templatePhysical : clusterTemplates) {
                try {
                    Result<Void> result = checkMetaInner(templatePhysical, logicId2IndexTemplateLogicMap, esClusters);
                    if (result.success()) {
                        LOGGER.info("class=TemplatePhyManagerImpl||method=metaCheck||msg=succ||physicalId={}", templatePhysical.getId());
                    } else {
                        LOGGER.warn("class=TemplatePhyManagerImpl||method=metaCheck||msg=fail||physicalId={}||failMsg={}", templatePhysical.getId(),
                                result.getMessage());
                        notifyService.send( NotifyTaskTypeEnum.TEMPLATE_PHYSICAL_META_ERROR,
                                new TemplatePhysicalMetaErrorNotifyInfo(templatePhysical, result.getMessage()),
                                Arrays.asList());
                    }
                    int indexOpResult = checkIndexCreateAndExpire(templatePhysical, logicId2IndexTemplateLogicMap);
                    if (indexOpResult == TOMORROW_INDEX_NOT_CREATE || indexOpResult == INDEX_ALL_ERR) {
                        tomorrowIndexNotCreateCount++;
                    }
                    if (indexOpResult == EXPIRE_INDEX_NOT_DELETE || indexOpResult == INDEX_ALL_ERR) {
                        expireIndexNotDeleteCount++;
                    }

                } catch (Exception e) {
                    LOGGER.error("class=TemplatePhyServiceImpl||method=metaCheck||errMsg={}||physicalId={}||",
                            e.getMessage(), templatePhysical.getId(), e);
                }
            }

            List<String> errMsgs = Lists.newArrayList();
            if (tomorrowIndexNotCreateCount * 1.0 / clusterTemplates.size() > 0.7) {
                errMsgs.add("有" + tomorrowIndexNotCreateCount + "个索引模板创建明天索引失败");
            }
            if (expireIndexNotDeleteCount * 1.0 / clusterTemplates.size() > 0.7) {
                errMsgs.add("有" + expireIndexNotDeleteCount + "个索引模板删除过期索引失败");
            }

            if (CollectionUtils.isNotEmpty(errMsgs)) {
                notifyService.send(NotifyTaskTypeEnum.CLUSTER_TEMPLATE_PHYSICAL_META_ERROR,
                        new ClusterTemplatePhysicalMetaErrorNotifyInfo(cluster, String.join(",", errMsgs)),
                        Arrays.asList());
            }

        }

        return true;
    }

    @Override
    public void syncMeta(Long physicalId, int retryCount) throws ESOperateException {

        // 从数据库获取物理模板
        IndexTemplatePhy indexTemplatePhy = templatePhyService.getTemplateById(physicalId);
        if (indexTemplatePhy == null) {
            return;
        }

        // 从ES集群获取模板配置
        TemplateConfig templateConfig = esTemplateService.syncGetTemplateConfig(indexTemplatePhy.getCluster(),
                indexTemplatePhy.getName());

        if (templateConfig == null) {
            // es集群中还没有模板，创建
            esTemplateService.syncCreate(indexTemplatePhy.getCluster(), indexTemplatePhy.getName(), indexTemplatePhy.getExpression(),
                    indexTemplatePhy.getRack(), indexTemplatePhy.getShard(), indexTemplatePhy.getShardRouting(), retryCount);

        } else {
            // 校验表达式
            if (
                    !indexTemplatePhy.getExpression().equals(templateConfig.getTemplate()) &&
                            esTemplateService.syncUpdateExpression(indexTemplatePhy.getCluster(), indexTemplatePhy.getName(),
                                    indexTemplatePhy.getExpression(), retryCount)
            ) {
                // 表达式不同（表达式发生变化），同步到ES集群
                LOGGER.info("class=TemplatePhyManagerImpl||method=syncMeta||msg=syncUpdateExpression succ||template={}||srcExp={}||tgtExp={}",
                        indexTemplatePhy.getName(), templateConfig.getTemplate(), indexTemplatePhy.getExpression());
            }

            // 标志shard或rack是否需要修改
            boolean editShardOrRack = false;
            Map<String, String> settings = templateConfig.getSetttings();
            String rack = settings.get(TEMPLATE_INDEX_INCLUDE_RACK);
            String shardNum = settings.get(INDEX_SHARD_NUM);

            // 校验shard个数
            if (!String.valueOf(indexTemplatePhy.getShard()).equals(shardNum)) {
                editShardOrRack = true;
                shardNum = String.valueOf(indexTemplatePhy.getShard());
            }

            // 校验rack
            if (
                    StringUtils.isNotBlank(indexTemplatePhy.getRack()) &&
                            (!settings.containsKey(TEMPLATE_INDEX_INCLUDE_RACK)
                                    || !indexTemplatePhy.getRack().equals(settings.get(TEMPLATE_INDEX_INCLUDE_RACK)))
            ) {
                editShardOrRack = true;
                rack = indexTemplatePhy.getRack();
            }

            if (editShardOrRack && esTemplateService.syncUpdateRackAndShard(indexTemplatePhy.getCluster(), indexTemplatePhy.getName(), rack,
                    Integer.valueOf(shardNum), indexTemplatePhy.getShardRouting(), retryCount)) {
                // 同步变化到ES集群
                    LOGGER.info(
                            "class=TemplatePhyManagerImpl||method=syncMeta||msg=syncUpdateRackAndShard succ||template={}||srcRack={}||srcShard={}||tgtRack={}||tgtShard={}",
                            indexTemplatePhy.getName(), settings.get(TEMPLATE_INDEX_INCLUDE_RACK), settings.get(INDEX_SHARD_NUM),
                            rack, shardNum);
            }
        }
    }

    @Override
    public Result<Void> delTemplate(Long physicalId, String operator) throws ESOperateException {
        return templatePhyService.delTemplate(physicalId, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delTemplateByLogicId(Integer logicId, String operator) throws ESOperateException {
        List<IndexTemplatePhy> indexTemplatePhys = templatePhyService.getTemplateByLogicId(logicId);

        boolean succ = true;
        if (CollectionUtils.isEmpty(indexTemplatePhys)) {
            LOGGER.info("class=TemplatePhyManagerImpl||method=delTemplateByLogicId||logicId={}||msg=template no physical info!", logicId);
        } else {
            LOGGER.info("class=TemplatePhyManagerImpl||method=delTemplateByLogicId||logicId={}||physicalSize={}||msg=template has physical info!",
                    logicId, indexTemplatePhys.size());
            for (IndexTemplatePhy indexTemplatePhy : indexTemplatePhys) {
                if (delTemplate(indexTemplatePhy.getId(), operator).failed()) {
                    succ = false;
                }

            }
        }

        return Result.build(succ);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> upgradeTemplate(TemplatePhysicalUpgradeDTO param, String operator) throws ESOperateException {
        Result<Void> checkResult = checkUpgradeParam(param);
        if (checkResult.failed()) {
            LOGGER.warn("class=TemplatePhyManagerImpl||method=upgradeTemplate||msg={}", CHECK_FAIL_MSG + checkResult.getMessage());
            return checkResult;
        }

        return upgradeTemplateWithCheck(param, operator, 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> upgradeMultipleTemplate(List<TemplatePhysicalUpgradeDTO> params,
                                                   String operator) throws ESOperateException {
        if (CollectionUtils.isEmpty(params)) {
            Result.buildFail("参数为空");
        }

        for (TemplatePhysicalUpgradeDTO param : params) {
            Result<Void> ret = upgradeTemplate(param, operator);
            if (ret.failed()) {
                throw new ESOperateException("升级失败");
            }
        }
        return Result.buildSucc(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> copyTemplate(TemplatePhysicalCopyDTO param, String operator) throws AdminOperateException {
        Result<Void> checkResult = checkCopyParam(param);
        if (checkResult.failed()) {
            LOGGER.warn("class=TemplatePhyManagerImpl||method=copyTemplate||msg={}", CHECK_FAIL_MSG + checkResult.getMessage());
            return checkResult;
        }

        IndexTemplatePhy indexTemplatePhy = templatePhyService.getTemplateById(param.getPhysicalId());
        IndexTemplatePhysicalDTO tgtTemplateParam = ConvertUtil.obj2Obj(indexTemplatePhy, IndexTemplatePhysicalDTO.class);
        tgtTemplateParam.setCluster(param.getCluster());
        tgtTemplateParam.setRack(param.getRack());
        tgtTemplateParam.setRole(SLAVE.getCode());
        tgtTemplateParam.setShard(param.getShard());
        tgtTemplateParam.setVersion(indexTemplatePhy.getVersion());

        Result<Long> addResult = addTemplateWithoutCheck(tgtTemplateParam);
        if (addResult.failed()) {
            return Result.buildFrom(addResult);
        }

        // 记录操作记录
        operateRecordService.save(TEMPLATE, COPY, indexTemplatePhy.getLogicId(),
                "复制" + indexTemplatePhy.getCluster() + "物理模板至" + param.getCluster(), operator);

        if (esTemplateService.syncCopyMappingAndAlias(indexTemplatePhy.getCluster(), indexTemplatePhy.getName(),
                tgtTemplateParam.getCluster(), tgtTemplateParam.getName(), 0)) {
            LOGGER.info("class=TemplatePhyManagerImpl||methood=copyTemplate||TemplatePhysicalCopyDTO={}||msg=syncCopyMappingAndAlias succ", param);
        } else {
            LOGGER.warn("class=TemplatePhyManagerImpl||methood=copyTemplate||TemplatePhysicalCopyDTO={}||msg=syncCopyMappingAndAlias fail", param);
        }

        return Result.buildSucWithTips("模板部署集群变更!请注意模板APP是否可以使用修改后的集群rack\n模板复制后请确认逻辑模板quota是否充足");
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> editTemplate(IndexTemplatePhysicalDTO param, String operator) throws ESOperateException {
        Result<Void> checkResult = validateTemplate(param, EDIT);
        if (checkResult.failed()) {
            LOGGER.warn("class=TemplatePhyManagerImpl||method=editTemplate||msg={}", CHECK_FAIL_MSG + checkResult.getMessage());
            return checkResult;
        }

        return editTemplateWithoutCheck(param, operator, 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> editMultipleTemplate(List<IndexTemplatePhysicalDTO> params,
                                                String operator) throws ESOperateException {
        if (CollectionUtils.isEmpty(params)) {
            Result.buildFail("参数为空");
        }

        for (IndexTemplatePhysicalDTO param : params) {
            Result<Void> ret = editTemplate(param, operator);
            if (ret.failed()) {
                throw new ESOperateException(String.format("编辑模板:%s失败", param.getName()));
            }
        }

        return Result.buildSucc(true);
    }

    @Override
    public Result<Void> validateTemplate(IndexTemplatePhysicalDTO param, OperationEnum operation) {
        if (AriusObjUtils.isNull(param)) {
            return Result.buildParamIllegal("物理模板参数为空");
        }
        if (operation == OperationEnum.ADD) {
            Result<Void> result = handleValidateTemplateAdd(param);
            if (result.failed()) {return result;}
        } else if (operation == EDIT) {
            Result<Void> result = handleValidateTemplateEdit(param);
            if (result.failed()) {return result;}
        }

        Result<Void> result = handleValidateTemplate(param);
        if (result.failed()) {return result;}

        return Result.buildSucc();
    }

    @Override
    public Result<Void> validateTemplates(List<IndexTemplatePhysicalDTO> params, OperationEnum operation) {
        if (AriusObjUtils.isNull(params)) {
            return Result.buildParamIllegal("物理模板信息为空");
        }

        Set<String> deployClusterSet = Sets.newTreeSet();
        for (IndexTemplatePhysicalDTO param : params) {
            Result<Void> checkResult = validateTemplate(param, operation);
            if (checkResult.failed()) {
                LOGGER.warn("class=TemplatePhyManagerImpl||method=validateTemplates||msg={}", CHECK_FAIL_MSG + checkResult.getMessage());
                checkResult
                        .setMessage(checkResult.getMessage() + "; 集群:" + param.getCluster() + ",模板:" + param.getName());
                return checkResult;
            }

            if (deployClusterSet.contains(param.getCluster())) {
                return Result.buildParamIllegal("部署集群重复");
            } else {
                deployClusterSet.add(param.getCluster());
            }

        }

        return Result.buildSucc();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> addTemplatesWithoutCheck(Integer logicId,
                                           List<IndexTemplatePhysicalDTO> physicalInfos) throws ESOperateException {
        for (IndexTemplatePhysicalDTO param : physicalInfos) {
            param.setLogicId(logicId);
            param.setPhysicalInfos(physicalInfos);
            Result<Long> result = addTemplateWithoutCheck(param);
            if (result.failed()) {
                result.setMessage(result.getMessage() + "; 集群:" + param.getCluster() + ",模板:" + param.getName());
                return Result.buildFrom(result);
            }
        }
        return Result.buildSucc();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> addTemplateWithoutCheck(IndexTemplatePhysicalDTO param) throws ESOperateException {
        if (null != templatePhyService.getTemplateByClusterAndName(param.getCluster(), param.getName())) {
            return Result.buildParamIllegal("索引已经存在");
        }

        initParamWhenAdd(param);

        // 为了解决写入的长尾问题，引擎增加了逻辑shard的概念，这里需要计算逻辑shard的值，并调整源shard个数
        templateShardManager.initShardRoutingAndAdjustShard(param);
        Result<Long> result = templatePhyService.insert(param);
        Long physicalId = result.getData();
        if (result.success()) {
            //删除数据库中历史的脏数据
            templatePhyService.deleteDirtyByClusterAndName(param.getCluster(), param.getName());
            IndexTemplateLogic logic = templateLogicService.getLogicTemplateById(param.getLogicId());

            esTemplateService.syncCreate(param.getCluster(), param.getName(), logic.getExpression(), param.getRack(),
                    param.getShard(), param.getShardRouting(), 0);

            SpringTool.publish(new PhysicalTemplateAddEvent(this, templatePhyService.getTemplateById(physicalId),
                    buildIndexTemplateLogicWithPhysicalForNew(param)));
        }

        return Result.buildSucc(physicalId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> editTemplateFromLogic(IndexTemplateLogicDTO param, String operator) throws ESOperateException {
        List<IndexTemplatePhy> indexTemplatePhys = templatePhyService.getTemplateByLogicId(param.getId());
        if (CollectionUtils.isEmpty(indexTemplatePhys)) {
            return Result.buildSucc();
        }

        for (IndexTemplatePhy indexTemplatePhy : indexTemplatePhys) {
            if (AriusObjUtils.isChanged(param.getExpression(), indexTemplatePhy.getExpression())) {
                Result<Void> result = templatePhyService.updateTemplateExpression(indexTemplatePhy, param.getExpression(), operator);
                if (result.failed()) {
                    return result;
                }
            }

            if (isValidShardNum(param.getShardNum())
                    && AriusObjUtils.isChanged(param.getShardNum(), indexTemplatePhy.getShard())) {
                Result<Void> result = templatePhyService.updateTemplateShardNum(indexTemplatePhy, param.getShardNum(), operator);
                if (result.failed()) {
                    return result;
                }
            }
        }

        return Result.buildSucc();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> switchMasterSlave(Integer logicId, Long expectMasterPhysicalId, String operator) {
        List<IndexTemplatePhy> indexTemplatePhys = templatePhyService.getTemplateByLogicId(logicId);
        if (CollectionUtils.isEmpty(indexTemplatePhys)) {
            return Result.buildNotExist("模板不存在");
        }

        IndexTemplatePhy oldMaster = null;
        IndexTemplatePhy newMaster = null;

        for (IndexTemplatePhy indexTemplatePhy : indexTemplatePhys) {
            if (indexTemplatePhy.getRole().equals(MASTER.getCode())) {
                if (oldMaster != null) {
                    LOGGER.error(
                            "class=TemplatePhyServiceImpl||method=switchMasterSlave||errMsg=no master||logicId={}",
                            logicId);
                }
                oldMaster = indexTemplatePhy;
            } else {
                if (expectMasterPhysicalId == null && newMaster == null) {
                    newMaster = indexTemplatePhy;
                }

                if (indexTemplatePhy.getId().equals(expectMasterPhysicalId)) {
                    newMaster = indexTemplatePhy;
                }
            }
        }

        if (newMaster == null) {
            return Result.buildNotExist("无法确定新的主");
        }

        boolean succ = true;

        if (oldMaster == null) {
            LOGGER.error("class=TemplatePhyServiceImpl||method=switchMasterSlave||errMsg=no master||logicId={}",
                    logicId);
        } else {
            succ = templatePhyService.updateTemplateRole(oldMaster,SLAVE,operator).success();
        }

        succ = succ && (templatePhyService.updateTemplateRole(newMaster,MASTER,operator).success());

        if (succ) {
            operateRecordService.save(TEMPLATE, SWITCH_MASTER_SLAVE, logicId,
                    "src_master:" + (oldMaster != null ? oldMaster.getId() : "") + ", tgt_master:" + newMaster.getId(),
                    operator);
        }

        return Result.build(succ);
    }

    @Override
    public Result<Void> editTemplateRackWithoutCheck(Long physicalId, String tgtRack, String operator,
                                               int retryCount) throws ESOperateException {
        IndexTemplatePhysicalDTO updateParam = new IndexTemplatePhysicalDTO();
        updateParam.setId(physicalId);
        updateParam.setRack(tgtRack);
        return editTemplateWithoutCheck(updateParam, operator, retryCount);
    }

    @Override
    public Result<Void> upgradeTemplateVersion(Long physicalId, String operator, int retryCount) throws ESOperateException {
        IndexTemplatePhy indexTemplatePhy = templatePhyService.getTemplateById(physicalId);
        if (indexTemplatePhy == null) {
            return Result.buildNotExist("模板不存在");
        }

        int version = indexTemplatePhy.getVersion() + 1;
        if (version > 9) {
            version = 0;
        }

        IndexTemplatePhysicalDTO updateParam = new IndexTemplatePhysicalDTO();
        updateParam.setId(indexTemplatePhy.getId());
        updateParam.setVersion(version);
        return editTemplateWithoutCheck(updateParam, operator, retryCount);
    }

    @Override
    public Result<Void> editTemplateWithoutCheck(IndexTemplatePhysicalDTO param, String operator,
                                           int retryCount) throws ESOperateException {
        IndexTemplatePhy oldIndexTemplatePhy = templatePhyService.getTemplateById(param.getId());

        if (param.getShard() != null && !oldIndexTemplatePhy.getShard().equals(param.getShard())) {
            templateShardManager.initShardRoutingAndAdjustShard(param);
        }

        boolean succ = templatePhyService.update(param).success();
        String tips = "";
        if (succ) {
            if (AriusObjUtils.isChanged(param.getRack(), oldIndexTemplatePhy.getRack())
                    || AriusObjUtils.isChanged(param.getShard(), oldIndexTemplatePhy.getShard())) {
                esTemplateService.syncUpdateRackAndShard(oldIndexTemplatePhy.getCluster(), oldIndexTemplatePhy.getName(), param.getRack(),
                        param.getShard(), param.getShardRouting(), retryCount);
                if (AriusObjUtils.isChanged(param.getRack(), oldIndexTemplatePhy.getRack())) {
                    tips = "模板部署rack变更!请注意模板APP是否可以使用修改后的rack";
                }
            }
            // 记录操作记录
            String editContent = AriusObjUtils.findChanged(oldIndexTemplatePhy, param);
            if (StringUtils.isNotBlank(editContent)) {
                operateRecordService.save(TEMPLATE, EDIT, oldIndexTemplatePhy.getLogicId(),
                        "修改" + oldIndexTemplatePhy.getCluster() + "物理模板:" + editContent, operator);
            }

            SpringTool.publish(new PhysicalTemplateModifyEvent(this, ConvertUtil.obj2Obj(oldIndexTemplatePhy, IndexTemplatePhy.class),
                    templatePhyService.getTemplateById(oldIndexTemplatePhy.getId()),
                    templateLogicService.getLogicTemplateWithPhysicalsById(oldIndexTemplatePhy.getLogicId())));
        }

        return Result.buildWithTips(succ, tips);
    }

    @Override
    public Set<String> getIndexByBeforeDay(IndexTemplatePhyWithLogic physicalWithLogic, int days) {
        try {
            IndexTemplateLogic logicTemplate = physicalWithLogic.getLogicTemplate();

            if (!physicalWithLogic.getExpression().endsWith("*")) {
                return Sets.newHashSet();
            }

            if (!TemplateUtils.isSaveByDay(logicTemplate.getDateFormat())
                    && !TemplateUtils.isSaveByMonth(logicTemplate.getDateFormat())) {
                return Sets.newHashSet();
            }

            List<String> indices = templatePhyService.getMatchIndexNames(physicalWithLogic.getId());
            if (CollectionUtils.isEmpty(indices)) {
                LOGGER.info("class=TemplatePhyManagerImpl||method=getIndexByBeforeDay||template={}||msg=no match indices", logicTemplate.getName());
                return Sets.newHashSet();
            }

            return getFinalIndexSet(physicalWithLogic, days, logicTemplate, indices);
        } catch (Exception e) {
            LOGGER.warn("class=TemplatePhyManagerImpl||method=getIndexByBeforeDay||templateName={}||errMsg={}", physicalWithLogic.getName(),
                    e.getMessage(), e);
        }

        return Sets.newHashSet();
    }

    @Override
    @Cacheable(cacheNames = CACHE_GLOBAL_NAME, key = "#appId + '@' + #param.cluster + '@' + 'getConsoleTemplatePhyVOS'")
    public List<ConsoleTemplatePhyVO> getConsoleTemplatePhyVOS(IndexTemplatePhysicalDTO param, Integer appId) {
        List<ConsoleTemplatePhyVO> consoleTemplatePhyVOS = ConvertUtil.list2List(templatePhyService.getByCondt(param),
            ConsoleTemplatePhyVO.class);

        buildConsoleTemplatePhyVO(consoleTemplatePhyVOS, appId);

        return consoleTemplatePhyVOS;
    }

    @Override
    public List<String> getTemplatePhyNames(Integer appId) {
        return getConsoleTemplatePhyVOS(null, appId).parallelStream().map(ConsoleTemplatePhyVO::getName)
            .collect(Collectors.toList());
    }

    @Override
    public List<String> getCanCopyTemplatePhyClusterPhyNames(Long templatePhyId) {
        List<String> canCopyClusterPhyNames = Lists.newArrayList();
        IndexTemplatePhy templatePhy = templatePhyService.getTemplateById(templatePhyId);
        if (null != templatePhy && null != templatePhy.getCluster()) {
            clusterPhyService.listAllClusters()
                    .stream()
                    .filter(clusterPhy -> !templatePhy.getCluster().equals(clusterPhy.getCluster()))
                    .forEach(clusterPhy -> canCopyClusterPhyNames.add(clusterPhy.getCluster()));
        }

        return canCopyClusterPhyNames;
    }

    @Override
    public Result<List<IndexTemplatePhysicalVO>> getTemplatePhies(Integer logicId) {
        if (!templateLogicService.exist(logicId)) {
            return Result.buildFail("模板Id不存在");
        }
        return Result.buildSucc(
            ConvertUtil.list2List(templatePhyService.getTemplateByLogicId(logicId), IndexTemplatePhysicalVO.class));
    }

    /**************************************** private method ****************************************************/
    private void initParamWhenAdd(IndexTemplatePhysicalDTO param) {
        IndexTemplateLogic logic = templateLogicService.getLogicTemplateById(param.getLogicId());

        if (param.getName() == null) {
            param.setName(logic.getName());
        }
        if (param.getExpression() == null) {
            param.setExpression(logic.getExpression());
        }
        if (param.getStatus() == null) {
            param.setStatus(TemplatePhysicalStatusEnum.NORMAL.getCode());
        }

        if (param.getRack() == null) {
            param.setRack("");
        }

        if (param.getVersion() == null) {
            param.setVersion(0);
        }

        if (param.getConfig() == null) {
            param.setConfig("");
        }

        IndexTemplatePhysicalConfig indexTemplatePhysicalConfig = new IndexTemplatePhysicalConfig();
        if (StringUtils.isNotBlank(param.getConfig())) {
            indexTemplatePhysicalConfig = JSON.parseObject(param.getConfig(), IndexTemplatePhysicalConfig.class);
        }

        indexTemplatePhysicalConfig.setGroupId(param.getGroupId());
        indexTemplatePhysicalConfig.setDefaultWriterFlags(param.getDefaultWriterFlags());

        param.setConfig(JSON.toJSONString(indexTemplatePhysicalConfig));
    }

    private Result<Void> checkUpgradeParam(TemplatePhysicalUpgradeDTO param) {
        if (AriusObjUtils.isNull(param)) {
            return Result.buildParamIllegal("模板升版本信息为空");
        }
        if (AriusObjUtils.isNull(param.getPhysicalId())) {
            return Result.buildParamIllegal(TEMPLATE_PHYSICAL_ID_IS_NULL);
        }
        if (AriusObjUtils.isNull(param.getVersion())) {
            return Result.buildParamIllegal("物理模板版本为空");
        }

        IndexTemplatePhy oldIndexTemplatePhy = templatePhyService.getTemplateById(param.getPhysicalId());
        if (oldIndexTemplatePhy == null) {
            return Result.buildNotExist(TEMPLATE_PHYSICAL_NOT_EXISTS);
        }
        if (Objects.equals(param.getVersion(), oldIndexTemplatePhy.getVersion())
                || (param.getVersion() > 0 && param.getVersion() < oldIndexTemplatePhy.getVersion())) {
            return Result.buildParamIllegal("物理模板版本非法");
        }
        if (param.getRack() != null && !clusterPhyService.isRacksExists(oldIndexTemplatePhy.getCluster(), param.getRack())) {
            return Result.buildParamIllegal("物理模板rack非法");
        }
        if (param.getShard() != null) {
            if (param.getShard() >= 1) {
                return Result.buildSucc();
            }
            return Result.buildParamIllegal("shard个数非法");
        }

        return Result.buildSucc();
    }

    private Result<Void> upgradeTemplateWithCheck(TemplatePhysicalUpgradeDTO param, String operator,
                                            int retryCount) throws ESOperateException {
        IndexTemplatePhy indexTemplatePhy = templatePhyService.getTemplateById(param.getPhysicalId());
        if (templateLabelService.hasDeleteDoc(indexTemplatePhy.getLogicId())) {
            return Result.buildParamIllegal("模板有删除操作,禁止升版本");
        }

        IndexTemplateLogic logic = templateLogicService.getLogicTemplateById(indexTemplatePhy.getLogicId());

        if (TemplateUtils.isOnly1Index(logic.getExpression())) {
            return Result.buildParamIllegal("不是分区创建的索引，不能升版本");
        }

        LOGGER.info("class=TemplatePhyManagerImpl||method=upgradeTemplateWithCheck||name={}||rack={}||shard={}||version={}", logic.getName(), param.getRack(),
                param.getShard(), param.getVersion());

        IndexTemplatePhysicalDTO updateParam = new IndexTemplatePhysicalDTO();
        updateParam.setId(indexTemplatePhy.getId());
        updateParam.setRack(param.getRack());
        updateParam.setShard(param.getShard());
        updateParam.setVersion(param.getVersion());
        Result<Void> editResult = editTemplateWithoutCheck(updateParam, operator, retryCount);

        if (editResult.failed()) {
            return editResult;
        }

        templatePreCreateManager.asyncCreateTodayAndTomorrowIndexByPhysicalId(indexTemplatePhy.getId(), 3);

        return Result.buildSucc();
    }

    private Result<Void> checkCopyParam(TemplatePhysicalCopyDTO param) {
        if (AriusObjUtils.isNull(param)) {
            return Result.buildParamIllegal("复制参数为空");
        }
        if (AriusObjUtils.isNull(param.getPhysicalId())) {
            return Result.buildParamIllegal(TEMPLATE_PHYSICAL_ID_IS_NULL);
        }
        if (AriusObjUtils.isNull(param.getCluster())) {
            return Result.buildParamIllegal("目标集群为空");
        }
        if (AriusObjUtils.isNull(param.getShard())) {
            return Result.buildParamIllegal("shard为空");
        }

        IndexTemplatePhy oldIndexTemplatePhy = templatePhyService.getTemplateById(param.getPhysicalId());
        if (oldIndexTemplatePhy == null) {
            return Result.buildNotExist(TEMPLATE_PHYSICAL_NOT_EXISTS);
        }

        if (!clusterPhyService.isClusterExists(param.getCluster())) {
            return Result.buildNotExist("目标集群不存在");
        }

        if (oldIndexTemplatePhy.getCluster().equals(param.getCluster())) {
            return Result.buildParamIllegal("目标集群不能与源集群相同");
        }

        if (StringUtils.isNotEmpty(param.getRack())
                && !clusterPhyService.isRacksExists(param.getCluster(), param.getRack())) {
            return Result.buildNotExist("rack不存在");
        }

        if (param.getShard() < 1) {
            return Result.buildParamIllegal("shard非法");
        }

        return Result.buildSucc();
    }

    private boolean needOperateAhead(IndexTemplatePhyWithLogic physicalWithLogic) {
        Set<String> clusterSet = ariusConfigInfoService.stringSettingSplit2Set(ARIUS_COMMON_GROUP,
                "delete.expire.index.ahead.clusters", "", ",");
        return clusterSet.contains(physicalWithLogic.getCluster());
    }

    private Result<Void> checkMetaInner(IndexTemplatePhy templatePhysical,
                                  Map<Integer, IndexTemplateLogic> logicId2IndexTemplateLogicMap,
                                  Set<String> esClusters) {
        List<String> errMsgs = Lists.newArrayList();

        if (!esClusters.contains(templatePhysical.getCluster())) {
            errMsgs.add("物理集群不存在：" + templatePhysical.getName() + "(" + templatePhysical.getId() + ")");
        }

        if (!logicId2IndexTemplateLogicMap.containsKey(templatePhysical.getLogicId())) {
            errMsgs.add("逻辑模板不存在：" + templatePhysical.getName() + "(" + templatePhysical.getId() + ")");
        }

        TemplateConfig templateConfig = esTemplateService.syncGetTemplateConfig(templatePhysical.getCluster(),
                templatePhysical.getName());

        if (templateConfig == null) {
            errMsgs.add("es模板不存在：" + templatePhysical.getName() + "(" + templatePhysical.getId() + ")");
        }

        if (CollectionUtils.isEmpty(errMsgs)) {
            return Result.buildSucc();
        }

        return Result.build( ResultType.ADMIN_META_ERROR.getCode(), String.join(",", errMsgs));

    }

    private int checkIndexCreateAndExpire(IndexTemplatePhy templatePhysical,
                                          Map<Integer, IndexTemplateLogic> logicId2IndexTemplateLogicMap) {
        int result = INDEX_OP_OK;
        if (templatePhysical.getCreateTime().before(AriusDateUtils.getZeroDate())) {
            Set<String> indices = Sets.newHashSet( templatePhyService.getMatchNoVersionIndexNames(templatePhysical.getId()));

            IndexTemplateLogic templateLogic = logicId2IndexTemplateLogicMap.get(templatePhysical.getLogicId());
            String tomorrowIndexName = IndexNameFactory.getNoVersion(templateLogic.getExpression(),
                    templateLogic.getDateFormat(), 1);
            String expireIndexName = IndexNameFactory.getNoVersion(templateLogic.getExpression(),
                    templateLogic.getDateFormat(), -1 * templateLogic.getExpireTime());

            if (!indices.contains(tomorrowIndexName)) {
                LOGGER.warn("class=TemplatePhyManagerImpl||method=checkIndexCreateAndExpire||cluster={}||template={}||msg=TOMORROW_INDEX_NOT_CREATE",
                        templatePhysical.getCluster(), templatePhysical.getName());
                result = result + TOMORROW_INDEX_NOT_CREATE;
            }

            if (TemplateUtils.isSaveByDay(templateLogic.getDateFormat()) && indices.contains(expireIndexName)) {
                LOGGER.warn("class=TemplatePhyManagerImpl||method=checkIndexCreateAndExpire||cluster={}||template={}||msg=EXPIRE_INDEX_NOT_DELETE",
                        templatePhysical.getCluster(), templatePhysical.getName());
                result = result + EXPIRE_INDEX_NOT_DELETE;
            }
        }
        return result;
    }

    private IndexTemplateLogicWithPhyTemplates buildIndexTemplateLogicWithPhysicalForNew(IndexTemplatePhysicalDTO param) {
        IndexTemplateLogicWithPhyTemplates logicWithPhysical = templateLogicService
                .getLogicTemplateWithPhysicalsById(param.getLogicId());
        if (CollectionUtils.isNotEmpty(param.getPhysicalInfos())) {
            List<IndexTemplatePhy> physicals = ConvertUtil.list2List(param.getPhysicalInfos(), IndexTemplatePhy.class);
            logicWithPhysical.setPhysicals(physicals);
        }
        return logicWithPhysical;
    }

    /**
     * 判定是否是合法的shard number.
     *
     * @param shardNum
     * @return
     */
    private boolean isValidShardNum(Integer shardNum) {
        return  (shardNum != null && shardNum > 0);
    }

    private void buildConsoleTemplatePhyVO(List<ConsoleTemplatePhyVO> params, Integer currentAppId) {
        
        Map<Integer, String> appId2AppNameMap = Maps.newHashMap();

        for (ConsoleTemplatePhyVO consoleTemplatePhyVO : params) {

            IndexTemplateLogic logicTemplate = templateLogicService.getLogicTemplateById(consoleTemplatePhyVO.getLogicId());
            if (AriusObjUtils.isNull(logicTemplate)) {
                LOGGER.error(
                        "class=TemplatePhyServiceImpl||method=buildConsoleTemplatePhyVO||errMsg=IndexTemplateLogic is empty||logicId={}",
                        consoleTemplatePhyVO.getLogicId());
                continue;
            }

            handleIndexTemplateLogic(currentAppId, appId2AppNameMap, consoleTemplatePhyVO, logicTemplate);

        }
    }

    private void handleIndexTemplateLogic(Integer currentAppId, Map<Integer, String> appId2AppNameMap, ConsoleTemplatePhyVO consoleTemplatePhyVO, IndexTemplateLogic logicTemplate) {
        //设置归属项目信息
        Integer appIdFromLogicTemplate = logicTemplate.getAppId();
        if (!AriusObjUtils.isNull(appIdFromLogicTemplate)) {
            consoleTemplatePhyVO.setAppId(appIdFromLogicTemplate);

            if (appId2AppNameMap.containsKey(appIdFromLogicTemplate)) {
                consoleTemplatePhyVO.setAppName(appId2AppNameMap.get(logicTemplate.getAppId()));
            } else {
                String appName = appService.getAppName(logicTemplate.getAppId());
                if (!AriusObjUtils.isNull(appName)) {
                    consoleTemplatePhyVO.setAppName(appName);
                    appId2AppNameMap.put(appIdFromLogicTemplate, appName);
                }
            }
        }

        //设置逻辑模板名称
        consoleTemplatePhyVO.setLogicName(logicTemplate.getName());

        //设置描述信息, 是否要加一列描述信息
        consoleTemplatePhyVO.setMemo(logicTemplate.getDesc());

        //设置权限
        if (AriusObjUtils.isNull(currentAppId)) {
            consoleTemplatePhyVO.setAuthType(AppTemplateAuthEnum.NO_PERMISSION.getCode());
            return;
        }
        if (currentAppId.equals(appIdFromLogicTemplate)) {
            consoleTemplatePhyVO.setAuthType(AppTemplateAuthEnum.OWN.getCode());
        } else {
            AppTemplateAuthEnum authEnum = appLogicTemplateAuthService.getAuthEnumByAppIdAndLogicId(currentAppId,
                    appIdFromLogicTemplate);
            consoleTemplatePhyVO.setAuthType(authEnum.getCode());
        }
    }

    private Result<Void> handleValidateTemplate(IndexTemplatePhysicalDTO param) {
        if (param.getCluster() != null && !clusterPhyService.isClusterExists(param.getCluster())) {
            return Result.buildParamIllegal("集群不存在");
        }
        if (StringUtils.isNotEmpty(param.getRack())) {
            if (!clusterPhyService.isRacksExists(param.getCluster(), param.getRack())) {
                return Result.buildParamIllegal("集群rack不存在");
            }
           /* // 校验rack匹配且只匹配到一个region
            if (regionRackService.countRackMatchedRegion(param.getCluster(), param.getRack()) != 1) {
                return Result.buildParamIllegal("集群rack不符合逻辑集群规划");
            }*/
        }
        if (param.getShard() != null && param.getShard() < 1) {
            return Result.buildParamIllegal("shard个数非法");
        }
        if (param.getRole() != null
                && TemplateDeployRoleEnum.UNKNOWN.equals(TemplateDeployRoleEnum.valueOf(param.getRole()))) {
            return Result.buildParamIllegal("模板角色非法");
        }
        if (param.getLogicId() != null && !Objects.equals(param.getLogicId(), NOT_CHECK)) {
            IndexTemplateLogic logic = templateLogicService.getLogicTemplateById(param.getLogicId());
            if (logic == null) {
                return Result.buildNotExist("逻辑模板不存在");
            }
        }
        return Result.buildSucc();
    }

    private Result<Void> handleValidateTemplateEdit(IndexTemplatePhysicalDTO param) {
        if (AriusObjUtils.isNull(param.getId())) {
            return Result.buildParamIllegal(TEMPLATE_PHYSICAL_ID_IS_NULL);
        }
        IndexTemplatePhy indexTemplatePhy = templatePhyService.getTemplateById(param.getId());
        if (indexTemplatePhy == null) {
            return Result.buildNotExist(TEMPLATE_PHYSICAL_NOT_EXISTS);
        }
        return Result.buildSucc();
    }

    private Result<Void> handleValidateTemplateAdd(IndexTemplatePhysicalDTO param) {
        if (AriusObjUtils.isNull(param.getLogicId())) {
            return Result.buildParamIllegal("逻辑模板id为空");
        }
        if (AriusObjUtils.isNull(param.getCluster())) {
            return Result.buildParamIllegal("集群为空");
        }

        if (AriusObjUtils.isNull(param.getShard())) {
            return Result.buildParamIllegal("shard为空");
        }
        if (AriusObjUtils.isNull(param.getRole())) {
            return Result.buildParamIllegal("模板角色为空");
        }

        IndexTemplatePhy indexTemplatePhy = templatePhyService.getTemplateByClusterAndName(param.getCluster(), param.getName());
        if (indexTemplatePhy != null) {
            return Result.buildDuplicate("物理模板已经存在");
        }
        return Result.buildSucc();
    }

    private Set<String> getFinalIndexSet(IndexTemplatePhyWithLogic physicalWithLogic, int days, IndexTemplateLogic logicTemplate, List<String> indices) {
        Set<String> finalIndexSet = Sets.newHashSet();
        for (String indexName : indices) {
            if (StringUtils.isBlank(indexName)) {
                continue;
            }

            Date indexTime = IndexNameFactory.genIndexTimeByIndexName(
                    genIndexNameClear(indexName, logicTemplate.getExpression()), logicTemplate.getExpression(),
                    logicTemplate.getDateFormat());

            if (indexTime == null) {
                LOGGER.warn(
                        "class=TemplatePhyManagerImpl||method=getIndexByBeforeDay||template={}||indexName={}||msg=template parse index time fail",
                        logicTemplate.getName(), indexName);
                continue;
            }

            if (TemplateUtils.isSaveByMonth(logicTemplate.getDateFormat())) {
                // 需要将索引时间定为当月的最后一天 确保最后一天的数据能被保留到保存时长
                indexTime = AriusDateUtils.getLastDayOfTheMonth(indexTime);
            }

            if (needOperateAhead(physicalWithLogic)) {
                int aheadSeconds = ariusConfigInfoService.intSetting(ARIUS_COMMON_GROUP,
                        "operate.index.ahead.seconds", 2 * 60 * 60);
                indexTime = AriusDateUtils.getBeforeSeconds(indexTime, aheadSeconds);
            }

            long timeIntervalDay = (System.currentTimeMillis() - indexTime.getTime()) / MILLIS_PER_DAY;
            if (timeIntervalDay < days) {
                LOGGER.info(
                        "class=TemplatePhyManagerImpl||method=getIndexByBeforeDay||template={}||indexName={}||timeIntervalDay={}||msg=index not match",
                        logicTemplate.getName(), indexName, timeIntervalDay);
                continue;
            }

            LOGGER.info("class=TemplatePhyManagerImpl||method=getIndexByBeforeDay||indexName={}||indexTime={}||timeIntervalDay={}", indexName,
                    indexTime, timeIntervalDay);

            finalIndexSet.add(indexName);
        }
        return finalIndexSet;
    }
}
