package com.didichuxing.datachannel.arius.admin.biz.page;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.didichuxing.datachannel.arius.admin.biz.cluster.ClusterPhyManager;
import com.didichuxing.datachannel.arius.admin.client.bean.common.PaginationResult;
import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.PageDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.cluster.ClusterPhyConditionDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.cluster.ConsoleClusterPhyVO;
import com.didichuxing.datachannel.arius.admin.client.constant.app.AppClusterPhyAuthEnum;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ClusterPhy;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ecm.RoleCluster;
import com.didichuxing.datachannel.arius.admin.common.constant.SortEnum;
import com.didichuxing.datachannel.arius.admin.common.constant.cluster.ClusterHealthEnum;
import com.didichuxing.datachannel.arius.admin.common.util.AriusObjUtils;
import com.didichuxing.datachannel.arius.admin.common.util.ConvertUtil;
import com.didichuxing.datachannel.arius.admin.common.util.FutureUtil;
import com.didichuxing.datachannel.arius.admin.core.service.app.AppService;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.physic.ClusterPhyService;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.physic.RoleClusterService;
import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;
import com.google.common.collect.Lists;

/**
 * Created by linyunan on 2021-10-14
 */
@Component
public class ClusterPhyPageSearchHandle extends BasePageSearchHandle<ConsoleClusterPhyVO> {

    private static final ILog        LOGGER = LogFactory.getLog(ClusterPhyPageSearchHandle.class);

    @Autowired
    private AppService               appService;

    @Autowired
    private ClusterPhyService        clusterPhyService;

    @Autowired
    private ClusterPhyManager        clusterPhyManager;

    @Autowired
    private RoleClusterService       roleClusterService;

    private static final FutureUtil<Void> futureUtil = FutureUtil.init("ClusterPhyPageSearchHandle",20, 40, 100);

    @Override
    protected Result<Boolean> validCheckForAppId(Integer appId) {
        if (!appService.isAppExists(appId)) {
            return Result.buildParamIllegal("项目不存在");
        }
        return Result.buildSucc(true);
    }

    @Override
    protected Result<Boolean> validCheckForCondition(PageDTO pageDTO, Integer appId) {
        if (pageDTO instanceof ClusterPhyConditionDTO) {
            ClusterPhyConditionDTO clusterPhyConditionDTO = (ClusterPhyConditionDTO) pageDTO;
            Integer authType = clusterPhyConditionDTO.getAuthType();
            if (null != authType && !AppClusterPhyAuthEnum.isExitByCode(authType)) {
                return Result.buildParamIllegal("权限类型不存在");
            }

            Integer status = clusterPhyConditionDTO.getHealth();
            if (null != status && !ClusterHealthEnum.isExitByCode(status)) {
                return Result.buildParamIllegal("集群状态类型不存在");
            }

            String clusterPhyName = clusterPhyConditionDTO.getCluster();
            if (!AriusObjUtils.isBlack(clusterPhyName) && (clusterPhyName.startsWith("*") || clusterPhyName.startsWith("?"))) {
                return Result.buildParamIllegal("物理集群名称不允许带类似*, ?等通配符查询");
            }

            if (null != clusterPhyConditionDTO.getSortTerm() && !SortEnum.isExit(clusterPhyConditionDTO.getSortTerm())) {
                return Result.buildParamIllegal(String.format("暂且不支持排序字段[%s]", clusterPhyConditionDTO.getSortTerm()));
            }

            return Result.buildSucc(true);
        }

        LOGGER.error("class=ClusterPhyPageSearchHandle||method=validCheckForCondition||errMsg=failed to convert PageDTO to ClusterPhyConditionDTO");

        return Result.buildFail();
    }

    @Override
    protected void init(PageDTO pageDTO) {
        // Do nothing
    }

    @Override
    protected PaginationResult<ConsoleClusterPhyVO> buildWithAuthType(PageDTO pageDTO, Integer authType, Integer appId) {
        ClusterPhyConditionDTO condition = buildClusterPhyConditionDTO(pageDTO);
        
        // 1. 获取管理/读写/读/无权限的物理集群信息
        List<ClusterPhy> appAuthClusterPhyList = clusterPhyManager.getClusterPhyByAppIdAndAuthType(appId, condition.getAuthType());
        if (CollectionUtils.isEmpty(appAuthClusterPhyList)) {
            return PaginationResult.buildSucc();
        }

        // 2. 过滤出符合条件的列表
        List<ClusterPhy> meetConditionClusterPhyList = getMeetConditionClusterPhyList(condition, appAuthClusterPhyList);

        // 3. 设置命中数
        long hitTotal = meetConditionClusterPhyList.size();

        List<ConsoleClusterPhyVO> meetConditionClusterPhyListVOList = ConvertUtil.list2List(meetConditionClusterPhyList, ConsoleClusterPhyVO.class);
        
        // 4. 根据匹配结果进行对模板id进行排序, 根据分页信息过滤出需要获取的模板id
        sort(meetConditionClusterPhyListVOList, condition.getSortTerm(), condition.getOrderByDesc());

        // 5. 最后页临界点处理
        long size = getLastPageSize(condition, meetConditionClusterPhyList.size());

        List<ConsoleClusterPhyVO> fuzzyAndLimitConsoleClusterPhyVOList  = meetConditionClusterPhyListVOList.subList(condition.getFrom().intValue(), (int)size);
        
        // 6. 设置权限
        fuzzyAndLimitConsoleClusterPhyVOList.forEach(consoleClusterPhyVO -> consoleClusterPhyVO.setCurrentAppAuth(condition.getAuthType()));

        // 7.设置物理集群的所属项目和所属AppId
        fuzzyAndLimitConsoleClusterPhyVOList.forEach(consoleClusterPhyVO -> clusterPhyManager.buildBelongAppIdsAndNames(consoleClusterPhyVO));

        // 8. 设置集群角色信息
        List<Integer> clusterIds = fuzzyAndLimitConsoleClusterPhyVOList.stream().map(ConsoleClusterPhyVO::getId).collect(Collectors.toList());
        Map<Long, List<RoleCluster>> roleListMap = roleClusterService.getAllRoleClusterByClusterIds(clusterIds);

        for (ConsoleClusterPhyVO consoleClusterPhyVO : fuzzyAndLimitConsoleClusterPhyVOList) {
            futureUtil.runnableTask(() -> clusterPhyManager.buildClusterRole(consoleClusterPhyVO,
                    roleListMap.get(consoleClusterPhyVO.getId().longValue())));
        }
        futureUtil.waitExecute();
        
        return PaginationResult.buildSucc(fuzzyAndLimitConsoleClusterPhyVOList, hitTotal, condition.getFrom(), condition.getSize());
    }

    @Override
    protected PaginationResult<ConsoleClusterPhyVO> buildWithoutAuthType(PageDTO pageDTO, Integer appId) {
        ClusterPhyConditionDTO condition = buildClusterPhyConditionDTO(pageDTO);

        List<ClusterPhy> pagingGetClusterPhyList      =  clusterPhyService.pagingGetClusterPhyByCondition(condition);

        List<ConsoleClusterPhyVO> consoleClusterPhyVOList = clusterPhyManager.buildClusterInfo(pagingGetClusterPhyList, appId);

        long totalHit = clusterPhyService.fuzzyClusterPhyHitByCondition(condition);
        return PaginationResult.buildSucc(consoleClusterPhyVOList, totalHit, condition.getFrom(), condition.getSize());
    }
    
    /****************************************private***********************************************/
    private ClusterPhyConditionDTO buildClusterPhyConditionDTO(PageDTO pageDTO) {
        if (pageDTO instanceof ClusterPhyConditionDTO) {
            return (ClusterPhyConditionDTO) pageDTO;
        }
        return null;
    }

    /**
     * 3个条件（集群名称、健康状态、版本）等组合查询 共7种情况
     *
     * @param condition               条件列表
     * @param appAuthClusterPhyList   带指定权限的物理集群列表
     * @return
     */
    private List<ClusterPhy> getMeetConditionClusterPhyList(ClusterPhyConditionDTO condition, List<ClusterPhy> appAuthClusterPhyList) {
        List<ClusterPhy> meetConditionClusterPhyList = Lists.newArrayList();

        //分页查询条件中只存在集群名称
        if (!AriusObjUtils.isBlack(condition.getCluster())) {
            appAuthClusterPhyList = appAuthClusterPhyList
                                  .stream()
                                  .filter(r -> r.getCluster().contains(condition.getCluster()))
                                  .collect(Collectors.toList());
        }

        //分页查询条件中只存在健康状态
        if (null != condition.getHealth()) {
            appAuthClusterPhyList = appAuthClusterPhyList
                                .stream()
                                .filter(r -> r.getHealth().equals(condition.getHealth()))
                                .collect(Collectors.toList());
        }

        //分页查询条件中只存在版本
        if (!AriusObjUtils.isBlack(condition.getEsVersion())) {
            appAuthClusterPhyList = appAuthClusterPhyList
                                .stream()
                                .filter(r -> r.getEsVersion().equals(condition.getEsVersion()))
                                .collect(Collectors.toList());
        }
        meetConditionClusterPhyList.addAll(appAuthClusterPhyList);
        return meetConditionClusterPhyList;
    }

    /**
     * 根据条件排序
     * @param meetConditionClusterPhyListVOList
     * @param sortTerm
     * @param orderByDesc
     */
    private void sort(List<ConsoleClusterPhyVO> meetConditionClusterPhyListVOList, String sortTerm, Boolean orderByDesc) {
        if (null == sortTerm) {
            Collections.sort(meetConditionClusterPhyListVOList);
            return;
        }

        meetConditionClusterPhyListVOList.sort((o1, o2) -> {
            if (SortEnum.DISK_FREE_PERCENT.getType().equals(sortTerm)) {
                return orderByDesc ? o2.getDiskUsagePercent().compareTo(o1.getDiskUsagePercent()) :
                        o1.getDiskUsagePercent().compareTo(o2.getDiskUsagePercent());
            }

            // 可在此添加需要排序的项
            if (SortEnum.ACTIVE_SHARD_NUM.getType().equals(sortTerm)) {
                return orderByDesc ? o2.getActiveShardNum().compareTo(o1.getActiveShardNum()) :
                        o1.getActiveShardNum().compareTo(o2.getActiveShardNum());
            }

            // 0 为不排序 
            return 0;
        });
    }
}
