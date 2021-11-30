package com.didichuxing.datachannel.arius.admin.core.service.app.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ClusterLogic;
import com.didichuxing.datachannel.arius.admin.core.service.app.AppService;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.logic.ClusterLogicService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.app.AppLogicClusterAuthDTO;
import com.didichuxing.datachannel.arius.admin.client.constant.app.AppClusterLogicAuthEnum;
import com.didichuxing.datachannel.arius.admin.client.constant.operaterecord.ModuleEnum;
import com.didichuxing.datachannel.arius.admin.client.constant.operaterecord.OperationEnum;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.app.AppClusterLogicAuth;
import com.didichuxing.datachannel.arius.admin.common.bean.po.app.AppClusterLogicAuthPO;
import com.didichuxing.datachannel.arius.admin.common.component.SpringTool;
import com.didichuxing.datachannel.arius.admin.common.event.auth.AppLogicClusterAuthAddEvent;
import com.didichuxing.datachannel.arius.admin.common.event.auth.AppLogicClusterAuthDeleteEvent;
import com.didichuxing.datachannel.arius.admin.common.event.auth.AppLogicClusterAuthEditEvent;
import com.didichuxing.datachannel.arius.admin.common.util.AriusObjUtils;
import com.didichuxing.datachannel.arius.admin.common.util.ConvertUtil;
import com.didichuxing.datachannel.arius.admin.common.util.EnvUtil;
import com.didichuxing.datachannel.arius.admin.core.component.ResponsibleConvertTool;
import com.didichuxing.datachannel.arius.admin.core.service.app.AppClusterLogicAuthService;
import com.didichuxing.datachannel.arius.admin.core.service.common.AriusUserInfoService;
import com.didichuxing.datachannel.arius.admin.core.service.common.OperateRecordService;
import com.didichuxing.datachannel.arius.admin.persistence.mysql.app.AppLogicClusterAuthDAO;
import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;

/**
 * APP 逻辑集群权限服务
 * @author wangshu
 * @date 2020/09/19
 */
@Service
public class AppClusterLogicAuthServiceImpl implements AppClusterLogicAuthService {

    private static final ILog      LOGGER = LogFactory.getLog(AppLogicTemplateAuthServiceImpl.class);

    @Autowired
    private AppLogicClusterAuthDAO logicClusterAuthDAO;

    @Autowired
    private ClusterLogicService    clusterLogicService;

    @Autowired
    private AppService             appService;

    @Autowired
    private OperateRecordService   operateRecordService;

    @Autowired
    private AriusUserInfoService   ariusUserInfoService;

    @Autowired
    private ResponsibleConvertTool responsibleConvertTool;

    /**
     * 设置APP对某逻辑集群的权限.
     * 封装了新增、更新、删除操作，调用接口时只需描述期望的权限状态
     * @param appId          APP的ID
     * @param logicClusterId 逻辑集群ID
     * @param auth           要设置的权限
     * @param responsible    责任人，逗号分隔的用户名列表
     * @param operator       操作人
     * @return 设置结果
     */
    @Override
    public Result<Void> ensureSetLogicClusterAuth(Integer appId, Long logicClusterId, AppClusterLogicAuthEnum auth,
                                            String responsible, String operator) {
        // 参数检查
        if (appId == null) {
            return Result.buildParamIllegal("未指定appId");
        }

        if (logicClusterId == null) {
            return Result.buildParamIllegal("未指定逻辑集群ID");
        }

        if (StringUtils.isBlank(operator)) {
            return Result.buildParamIllegal("未指定操作人");
        }

        // 获取已经存在的权限，可能来自于权限表（id不为null）和创建信息表（id为null）
        AppClusterLogicAuth oldAuth = getLogicClusterAuth(appId, logicClusterId);

        if (oldAuth == null || oldAuth.getType().equals(AppClusterLogicAuthEnum.NO_PERMISSIONS.getCode())) {
            // 之前无权限
            return handleNoAuth(appId, logicClusterId, auth, responsible, operator);
        } else {
            // 之前有权限
            if (oldAuth.getId() != null) {
                // 期望删除权限
                return deleteAuth(auth, responsible, operator, oldAuth);
            } else {
                //权限来自于创建信息表（权限肯定为OWN）,对于集群owner的app权限信息不能修改，只能增加大于OWN的权限
                return addAuth(appId, logicClusterId, auth, responsible, operator);
            }
        }
    }

    private Result<Void> addAuth(Integer appId, Long logicClusterId, AppClusterLogicAuthEnum auth, String responsible, String operator) {
        if (auth != null
            && AppClusterLogicAuthEnum.valueOf(auth.getCode()).higher(AppClusterLogicAuthEnum.OWN)) {
            return addLogicClusterAuth(
                new AppLogicClusterAuthDTO(null, appId, logicClusterId, auth.getCode(), responsible), operator);
        } else {
            return Result.buildFail("不支持对集群owner的权限进行修改");
        }
    }

    private Result<Void> deleteAuth(AppClusterLogicAuthEnum auth, String responsible, String operator, AppClusterLogicAuth oldAuth) {
        if (auth == AppClusterLogicAuthEnum.NO_PERMISSIONS) {
            return deleteLogicClusterAuthById(oldAuth.getId(), operator);
        }

        // 期望更新权限信息
        AppLogicClusterAuthDTO newAuthDTO = new AppLogicClusterAuthDTO(oldAuth.getId(), null, null,
            auth == null ? null : auth.getCode(), StringUtils.isBlank(responsible) ? null : responsible);
        return updateLogicClusterAuth(newAuthDTO, operator);
    }

    private Result<Void> handleNoAuth(Integer appId, Long logicClusterId, AppClusterLogicAuthEnum auth, String responsible, String operator) {
        // NO_PERMISSIONS不需添加
        if (auth == null || auth == AppClusterLogicAuthEnum.NO_PERMISSIONS) {
            return Result.buildSucc();
        }

        // 新增
        return addLogicClusterAuth(
            new AppLogicClusterAuthDTO(null, appId, logicClusterId, auth.getCode(), responsible), operator);
    }

    /**
     * 插入逻辑集群权限点
     * @param logicClusterAuth 逻辑集群权限点
     * @return
     */
    @Override
    public Result<Void> addLogicClusterAuth(AppLogicClusterAuthDTO logicClusterAuth, String operator) {

        Result<Void> checkResult = validateLogicClusterAuth(logicClusterAuth, OperationEnum.ADD);
        if (checkResult.failed()) {
            LOGGER.warn("class=AppClusterLogicAuthServiceImpl||method=createLogicClusterAuth||msg={}||msg=check fail!",
                checkResult.getMessage());
            return checkResult;
        }

        return addLogicClusterAuthWithoutCheck(logicClusterAuth, operator);
    }

    /**
     * 更新逻辑集群权限点
     * @param logicClusterAuth 逻辑集群权限点
     * @return
     */
    @Override
    public Result<Void> updateLogicClusterAuth(AppLogicClusterAuthDTO logicClusterAuth, String operator) {
        // 只支持修改权限类型和责任人
        logicClusterAuth.setAppId(null);
        logicClusterAuth.setLogicClusterId(null);

        Result<Void> checkResult = validateLogicClusterAuth(logicClusterAuth, OperationEnum.EDIT);
        if (checkResult.failed()) {
            LOGGER.warn("class=AppClusterLogicAuthServiceImpl||method=createLogicClusterAuth||msg={}||msg=check fail!",
                checkResult.getMessage());
            return checkResult;
        }

        return updateLogicClusterAuthWithoutCheck(logicClusterAuth, operator);
    }

    /**
     * 删除权限点
     * @param authId 权限点ID
     * @return
     */
    @Override
    public Result<Void> deleteLogicClusterAuthById(Long authId, String operator) {

        AppClusterLogicAuthPO oldAuthPO = logicClusterAuthDAO.getById(authId);
        if (oldAuthPO == null) {
            return Result.buildNotExist("权限不存在");
        }

        boolean succeed = 1 == logicClusterAuthDAO.delete(authId);
        if (succeed) {
            SpringTool.publish(new AppLogicClusterAuthDeleteEvent(this,
                responsibleConvertTool.obj2Obj(oldAuthPO, AppClusterLogicAuth.class)));

            operateRecordService.save(ModuleEnum.LOGIC_CLUSTER_PERMISSIONS, OperationEnum.DELETE, oldAuthPO.getId(),
                StringUtils.EMPTY, operator);
        }

        return Result.build(succeed);
    }

    @Override
    public Result<Boolean> deleteLogicClusterAuthByLogicClusterId(Long logicClusterId) {
        boolean succ = logicClusterAuthDAO.deleteByLogicClusterId(logicClusterId) >= 0;
        return Result.buildBoolen(succ);
    }

    /**
     * 获取APP所有权限点
     * @param appId 逻辑ID
     * @return
     */
    @Override
    public List<AppClusterLogicAuth> getAllLogicClusterAuths(Integer appId) {

        if (appId == null) {
            return new ArrayList<>();
        }

        // 权限表
        List<AppClusterLogicAuthPO> authPOs = logicClusterAuthDAO.listByAppId(appId);
        List<AppClusterLogicAuth> authDTOs = ConvertUtil.list2List(authPOs, AppClusterLogicAuth.class);

        // 从逻辑集群表获取APP作为owner的集群
        List<ClusterLogic> clusterLogicList = clusterLogicService.getOwnedClusterLogicListByAppId(appId);
        authDTOs.addAll(clusterLogicList
                        .stream()
                        .map(clusterLogic -> buildLogicClusterAuth(clusterLogic, AppClusterLogicAuthEnum.OWN))
                        .collect(Collectors.toList()));

        return authDTOs;
    }

    @Override
    public List<AppClusterLogicAuth> getLogicClusterAccessAuths(Integer appId) {
        return ConvertUtil.list2List(logicClusterAuthDAO.listWithAccessByAppId(appId), AppClusterLogicAuth.class);
    }

    /**
     * 根据ID获取逻辑集群权限点
     * @param authId 权限点ID
     * @return
     */
    @Override
    public AppClusterLogicAuth getLogicClusterAuthById(Long authId) {
        return ConvertUtil.obj2Obj(logicClusterAuthDAO.getById(authId), AppClusterLogicAuth.class);
    }

    /**
     * 获取指定app对指定逻辑集群的权限.
     * @param appId          APP ID
     * @param logicClusterId 逻辑集群ID
     */
    @Override
    public AppClusterLogicAuthEnum getLogicClusterAuthEnum(Integer appId, Long logicClusterId) {
        if (appId == null || logicClusterId == null) {
            return AppClusterLogicAuthEnum.NO_PERMISSIONS;
        }

        AppClusterLogicAuth auth = getLogicClusterAuth(appId, logicClusterId);
        return auth == null ? AppClusterLogicAuthEnum.NO_PERMISSIONS
            : AppClusterLogicAuthEnum.valueOf(auth.getType());
    }

    /**
     * 获取指定app对指定逻辑集群的权限，若没有权限则返回null.
     * 有权限时，返回结果中id不为null则为来自于权限表的数据，否则为来自于创建表的数据
     * @param appId          APP ID
     * @param logicClusterId 逻辑集群ID
     */
    @Override
    public AppClusterLogicAuth getLogicClusterAuth(Integer appId, Long logicClusterId) {
        if (appId == null || logicClusterId == null) {
            return null;
        }

        // 从逻辑集群表获取创建信息
        ClusterLogic clusterLogic = clusterLogicService.getClusterLogicById(logicClusterId);
        AppClusterLogicAuthEnum authFromCreateRecord = (clusterLogic != null
                                                        && clusterLogic.getAppId().equals(appId))
                                                            ? AppClusterLogicAuthEnum.OWN
                                                            : AppClusterLogicAuthEnum.NO_PERMISSIONS;

        // 从权限表获取权限信息
        AppClusterLogicAuthPO authPO = logicClusterAuthDAO.getByAppIdAndLogicCluseterId(appId, logicClusterId);
        AppClusterLogicAuthEnum authFromAuthRecord = (authPO != null)
            ? AppClusterLogicAuthEnum.valueOf(authPO.getType())
            : AppClusterLogicAuthEnum.NO_PERMISSIONS;

        // 都没有权限
        if (authFromCreateRecord == AppClusterLogicAuthEnum.NO_PERMISSIONS
            && authFromAuthRecord == AppClusterLogicAuthEnum.NO_PERMISSIONS) {
            return buildLogicClusterAuth(clusterLogic, AppClusterLogicAuthEnum.NO_PERMISSIONS);
        }

        // 选择权限高的构建AppLogicClusterAuthDTO，优先取权限表中的记录
        return authFromAuthRecord.higherOrEqual(authFromCreateRecord)
            ? ConvertUtil.obj2Obj(authPO, AppClusterLogicAuth.class)
            : buildLogicClusterAuth(clusterLogic, AppClusterLogicAuthEnum.OWN);

    }

    /**
     * 获取逻辑集群权限点列表
     * @param logicClusterId  逻辑集群ID
     * @param clusterAuthType 集群权限类型
     * @return
     */
    @Override
    public List<AppClusterLogicAuth> getLogicClusterAuths(Long logicClusterId,
                                                             AppClusterLogicAuthEnum clusterAuthType) {

        AppClusterLogicAuthPO queryParams = new AppClusterLogicAuthPO();
        if (logicClusterId != null) {
            queryParams.setLogicClusterId(logicClusterId);
        }

        if (clusterAuthType != null) {
            queryParams.setType(clusterAuthType.getCode());
        }

        // 权限表
        List<AppClusterLogicAuthPO> authPOs = logicClusterAuthDAO.listByCondition(queryParams);
        List<AppClusterLogicAuth>  authDTOS = ConvertUtil.list2List(authPOs, AppClusterLogicAuth.class);

        // 从逻辑集群表获取APP作为owner的集群
        if (logicClusterId != null && clusterAuthType == AppClusterLogicAuthEnum.OWN) {
            ClusterLogic clusterLogic = clusterLogicService.getClusterLogicById(logicClusterId);
            if (clusterLogic != null) {
                authDTOS.add(buildLogicClusterAuth(clusterLogic, AppClusterLogicAuthEnum.OWN));
            }
        }

        return authDTOS;
    }

    @Override
    public boolean canCreateLogicTemplate(Integer appId, Long logicClusterId) {
        if (appId == null || logicClusterId == null) {
            return false;
        }

        AppClusterLogicAuthEnum authEnum = getLogicClusterAuthEnum(appId, logicClusterId);
        return authEnum.higherOrEqual(AppClusterLogicAuthEnum.ACCESS);
    }

    /**
     * 增加权限  不做参数校验
     * @param authDTO  权限信息
     * @param operator 操作人
     * @return result
     */
    @Override
    public Result<Void> addLogicClusterAuthWithoutCheck(AppLogicClusterAuthDTO authDTO, String operator) {
        AppClusterLogicAuthPO authPO = responsibleConvertTool.obj2Obj(authDTO, AppClusterLogicAuthPO.class);

        boolean succeed = 1 == logicClusterAuthDAO.insert(authPO);
        if (succeed) {
            // 发送消息
            SpringTool.publish(new AppLogicClusterAuthAddEvent(this,
                responsibleConvertTool.obj2Obj(authPO, AppClusterLogicAuth.class)));

            // 记录操作
            operateRecordService.save(ModuleEnum.LOGIC_CLUSTER_PERMISSIONS, OperationEnum.ADD, authPO.getId(),
                JSON.toJSONString(authPO), operator);
        }

        return Result.build(succeed);
    }

    @Override
    public AppClusterLogicAuth buildClusterLogicAuth(Integer appId, Long clusterLogicId,
                                                     AppClusterLogicAuthEnum appClusterLogicAuthEnum) {
        if (null == appClusterLogicAuthEnum || null == appId || null == clusterLogicId) {
            return null;
        }

        if (!AppClusterLogicAuthEnum.isExitByCode(appClusterLogicAuthEnum.getCode())) {
            return null;
        }

        AppClusterLogicAuth appClusterLogicAuth = new AppClusterLogicAuth();
        appClusterLogicAuth.setAppId(appId);
        appClusterLogicAuth.setLogicClusterId(clusterLogicId);
        appClusterLogicAuth.setType(appClusterLogicAuthEnum.getCode());
        return appClusterLogicAuth;
    }

    /**************************************** private method ****************************************************/
    /**
     * 验证权限参数
     * @param authDTO   参数信息
     * @param operation 操作
     * @return result
     */
    private Result<Void> validateLogicClusterAuth(AppLogicClusterAuthDTO authDTO, OperationEnum operation) {
        if (!EnvUtil.isOnline()) {
            LOGGER.info("class=AppAuthServiceImpl||method=validateTemplateAuth||authDTO={}||operator={}",
                JSON.toJSONString(authDTO), operation);
        }

        if (authDTO == null) {
            return Result.buildParamIllegal("权限信息为空");
        }

        Integer appId = authDTO.getAppId();
        Long logicClusterId = authDTO.getLogicClusterId();
        AppClusterLogicAuthEnum authEnum = AppClusterLogicAuthEnum.valueOf(authDTO.getType());

        if (OperationEnum.ADD.equals(operation)) {
            Result<Void> result = handleAdd(authDTO, appId, logicClusterId, authEnum);
            if (result.failed()) return result;

        } else if (OperationEnum.EDIT.equals(operation)) {
            Result<Void> result = handleEdit(authDTO);
            if (result.failed()) return result;
        }

        Result<Void> isIllegalResult = isIllegal(authDTO, authEnum);
        if (isIllegalResult.failed()) return isIllegalResult;

        return Result.buildSucc();
    }

    private Result<Void> handleEdit(AppLogicClusterAuthDTO authDTO) {
        // 更新权限检查
        if (AriusObjUtils.isNull(authDTO.getId())) {
            return Result.buildParamIllegal("权限ID为空");
        }

        if (null == logicClusterAuthDAO.getById(authDTO.getId())) {
            return Result.buildNotExist("权限不存在");
        }
        return Result.buildSucc();
    }

    private Result<Void> handleAdd(AppLogicClusterAuthDTO authDTO, Integer appId, Long logicClusterId, AppClusterLogicAuthEnum authEnum) {
        // 新增权限检查
        Result<Void> judgeResult = validateAppIdIsNull(appId, logicClusterId);
        if (judgeResult.failed()) {
            return judgeResult;
        }
        
        ClusterLogic clusterLogic = clusterLogicService.getClusterLogicById(logicClusterId);
        if (AriusObjUtils.isNull(clusterLogic)) {
            return Result.buildParamIllegal(String.format("逻辑集群[%d]不存在", logicClusterId));
        }

        if (AriusObjUtils.isNull(authDTO.getType())) {
            return Result.buildParamIllegal("权限类型为空");
        }

        if (AriusObjUtils.isNull(authDTO.getResponsible())) {
            return Result.buildParamIllegal("责任人为空");
        }

        // 重复添加不做幂等，抛出错误
        if (null != logicClusterAuthDAO.getByAppIdAndLogicCluseterId(appId, logicClusterId)) {
            return Result.buildDuplicate("权限已存在");
        }

        // APP是逻辑集群的owner，无需添加
        if (clusterLogic.getAppId().equals(appId) && authEnum == AppClusterLogicAuthEnum.OWN) {
            return Result.buildDuplicate(String.format("APP[%d]已有管理权限", appId));
        }
        return Result.buildSucc();
    }

    private Result<Void> isIllegal(AppLogicClusterAuthDTO authDTO, AppClusterLogicAuthEnum authEnum) {
        if (AppClusterLogicAuthEnum.NO_PERMISSIONS == authEnum) {
            // 不应该走到这一步，防御编码
            return Result.buildParamIllegal("无权限无需添加");
        }

        // 不能添加管理权限
        if (AppClusterLogicAuthEnum.ALL == authEnum) {
            return Result.buildParamIllegal("不支持添加超管权限");
        }

        // 校验责任人是否合法
        if (!AriusObjUtils.isNull(authDTO.getResponsible())
            && AriusObjUtils.isNull(ariusUserInfoService.getByDomainAccount(authDTO.getResponsible()))) {
            return Result.buildParamIllegal("责任人非法");
        }
        return Result.buildSucc();
    }

    private Result<Void> validateAppIdIsNull(Integer appId, Long logicClusterId) {
        if (AriusObjUtils.isNull(appId)) {
            return Result.buildParamIllegal("appId为空");
        }

        
        if (!appService.isAppExists(appId)) {
            return Result.buildParamIllegal(String.format("app[%d]不存在", appId));
        }

        if (AriusObjUtils.isNull(logicClusterId)) {
            return Result.buildParamIllegal("逻辑集群ID为空");
        }
        return Result.buildSucc();
    }

    /**
     * 由逻辑集群记录构建owner APP的权限数据
     * @param clusterLogic 逻辑集群记录
     */
    private AppClusterLogicAuth buildLogicClusterAuth(ClusterLogic clusterLogic, AppClusterLogicAuthEnum appClusterLogicAuthEnum) {
        if (clusterLogic == null) {
            return null;
        }
        AppClusterLogicAuth appLogicClusterAuth = new AppClusterLogicAuth();
        appLogicClusterAuth.setId(null);
        appLogicClusterAuth.setAppId(clusterLogic.getAppId());
        appLogicClusterAuth.setLogicClusterId(clusterLogic.getId());
        appLogicClusterAuth.setType(appClusterLogicAuthEnum.getCode());
        appLogicClusterAuth.setResponsible(clusterLogic.getResponsible());
        return appLogicClusterAuth;
    }

    /**
     * 修改权限 可以修改权限类型和责任人 不校验参数
     * @param authDTO  参数
     * @param operator 操作人
     * @return result
     */
    private Result<Void> updateLogicClusterAuthWithoutCheck(AppLogicClusterAuthDTO authDTO, String operator) {

        AppClusterLogicAuthPO oldAuthPO = logicClusterAuthDAO.getById(authDTO.getId());
        AppClusterLogicAuthPO newAuthPO = responsibleConvertTool.obj2Obj(authDTO, AppClusterLogicAuthPO.class);
        boolean succeed = 1 == logicClusterAuthDAO.update(newAuthPO);
        if (succeed) {
            SpringTool.publish(new AppLogicClusterAuthEditEvent(this,
                responsibleConvertTool.obj2Obj(oldAuthPO, AppClusterLogicAuth.class), responsibleConvertTool
                    .obj2Obj(logicClusterAuthDAO.getById(authDTO.getId()), AppClusterLogicAuth.class)));

            operateRecordService.save(ModuleEnum.LOGIC_CLUSTER_PERMISSIONS, OperationEnum.EDIT, oldAuthPO.getId(),
                JSON.toJSONString(newAuthPO), operator);
        }

        return Result.build(succeed);
    }
}
