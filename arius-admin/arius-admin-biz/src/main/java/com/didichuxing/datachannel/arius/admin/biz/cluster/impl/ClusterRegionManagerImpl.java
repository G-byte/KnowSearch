package com.didichuxing.datachannel.arius.admin.biz.cluster.impl;

import static com.didichuxing.datachannel.arius.admin.client.constant.result.ResultType.FAIL;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ClusterPhy;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.didichuxing.datachannel.arius.admin.biz.cluster.ClusterContextManager;
import com.didichuxing.datachannel.arius.admin.biz.cluster.ClusterRegionManager;
import com.didichuxing.datachannel.arius.admin.biz.template.srv.TemplateSrvManager;
import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.cluster.ClusterRegionDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.cluster.ESLogicClusterWithRegionDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.cluster.ClusterRegionVO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.cluster.LogicClusterRackVO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.cluster.PhyClusterRackVO;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ClusterLogicContext;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ClusterLogicRackInfo;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.region.ClusterRegion;
import com.didichuxing.datachannel.arius.admin.common.exception.AriusRunTimeException;
import com.didichuxing.datachannel.arius.admin.common.util.AriusObjUtils;
import com.didichuxing.datachannel.arius.admin.common.util.EnvUtil;
import com.didichuxing.datachannel.arius.admin.common.util.ListUtils;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.logic.ClusterLogicService;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.physic.ClusterPhyService;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.region.RegionRackService;
import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;
import com.google.common.collect.Lists;

@Component
public class ClusterRegionManagerImpl implements ClusterRegionManager {

    private static final ILog     LOGGER = LogFactory.getLog(ClusterRegionManagerImpl.class);

    @Autowired
    private RegionRackService     regionRackService;

    @Autowired
    private ClusterContextManager clusterContextManager;

    @Autowired
    private ClusterLogicService   clusterLogicService;

    @Autowired
    private ClusterPhyService     clusterPhyService;

    @Autowired
    private TemplateSrvManager    templateSrvManager;

    /**
     * 构建regionVO
     * @param regions region列表
     * @return
     */
    @Override
    public List<ClusterRegionVO> buildLogicClusterRegionVO(List<ClusterRegion> regions) {
        if (CollectionUtils.isEmpty(regions)) {
            return new ArrayList<>();
        }

        return regions.stream().filter(Objects::nonNull).map(this::buildLogicClusterRegionVO)
            .collect(Collectors.toList());
    }

    /**
     * 构建regionVO
     * @param region region
     * @return
     */
    @Override
    public ClusterRegionVO buildLogicClusterRegionVO(ClusterRegion region) {
        if (region == null) {
            return null;
        }

        ClusterRegionVO logicClusterRegionVO = new ClusterRegionVO();
        logicClusterRegionVO.setId(region.getId());
        logicClusterRegionVO.setLogicClusterId(region.getLogicClusterId());
        logicClusterRegionVO.setClusterName(region.getPhyClusterName());
        logicClusterRegionVO.setRacks(region.getRacks());
        return logicClusterRegionVO;
    }

    @Override
    public List<PhyClusterRackVO> buildCanDividePhyClusterRackVOs(String cluster) {
        // 所有的racks
        Set<String> clusterRacks                 =   clusterPhyService.getClusterRacks(cluster);
        List<PhyClusterRackVO> racks             =   Lists.newArrayList();
        Set<String>            usedRacks         =   fetchDivideRacks(cluster, clusterRacks);
        Set<String>            unusedRacks       =   fetchUnusedRacks(clusterRacks, usedRacks);

        racks.addAll(batchBuildClusterRackVOs(cluster, usedRacks, 1));
        racks.addAll(batchBuildClusterRackVOs(cluster, unusedRacks, 0));

        LOGGER.info(
            "class=ClusterRegionManagerImpl||method=buildCanDividePhyClusterRackVOs||clusterRacks={}||usedRacksInfo={}||racks={}",
            clusterRacks, usedRacks, racks);
        return racks;
    }

    /**
     * 获取已经划分在region中的racks
     * @param phyClusterName
     * @param clusterRacks
     * @return
     */
    private Set<String> fetchDivideRacks(String phyClusterName, Set<String> clusterRacks) {
        List<ClusterRegion> regions = regionRackService.listRegionsByClusterName(phyClusterName);
        List<String> regionRacks    = Lists.newArrayList();
        regions.forEach(region-> regionRacks.addAll(ListUtils.string2StrList(region.getRacks())));

        return clusterRacks.stream().filter(regionRacks::contains).collect(Collectors.toSet());
    }

    /**
     * 构建逻辑集群物RackVO
     * @param logicClusterRackInfos 逻辑集群rack信息
     * @return
     */
    @Override
    public List<LogicClusterRackVO> buildLogicClusterRackVOs(List<ClusterLogicRackInfo> logicClusterRackInfos) {
        if (CollectionUtils.isEmpty(logicClusterRackInfos)) {
            return new ArrayList<>();
        }

        return logicClusterRackInfos.stream().filter(Objects::nonNull).map(this::buildLogicClusterRackVO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchBindRegionToClusterLogic(ESLogicClusterWithRegionDTO param, String operator,
                                                      boolean isAddClusterLogicFlag) {
        //1. 前置校验
        if (AriusObjUtils.isNull(param)) {
            return Result.buildParamIllegal("参数为空");
        }
        if (CollectionUtils.isEmpty(param.getClusterRegionDTOS())) {
            return Result.buildParamIllegal("逻辑集群关联region信息为空");
        }

        //2. 集群合法关联性校验
        param.getClusterRegionDTOS().stream().distinct()
            .forEach(clusterRegionDTO -> checkCanBeBound(param.getId(), clusterRegionDTO, param.getType()));

        //3. 逻辑集群绑定的物理集群版本一致性校验
        Result<Void> phyClusterVersionsResult = boundPhyClusterVersionsCheck(param);
        if (phyClusterVersionsResult.failed()) {
            return Result.buildFrom(phyClusterVersionsResult);
        }

        //4. 是否要创建逻辑集群
        if (isAddClusterLogicFlag) {
            param.setDataCenter(EnvUtil.getDC().getCode());
            Result<Long> createLogicClusterResult = clusterLogicService.createClusterLogic(param, operator);
            if (createLogicClusterResult.failed()) {
                return Result.buildFrom(createLogicClusterResult);
            }
            param.setId(createLogicClusterResult.getData());
        }

        //5. 初始化物理集群索引服务
        initTemplateSrvOfClusterPhy(param, operator);

        //6. 为逻辑集群绑定region
        return doBindRegionToClusterLogic(param, operator);
    }

    @Override
    public Result<Void> unbindRegion(Long regionId, String operator) {
        return regionRackService.unbindRegion(regionId, operator);
    }

    @Override
    public Result<Void> bindRegion(Long regionId, Long logicClusterId, Integer share, String operator) {
        return regionRackService.bindRegion(regionId, logicClusterId, share, operator);
    }

    /***************************************** private method ****************************************************/
    /**
     * 获取没有使用的Rack列表
     * @param clusterRacks 物理集群Rack
     * @param usedRacks    使用的Rack
     * @return
     */
    private Set<String> fetchUnusedRacks(Set<String> clusterRacks, Set<String> usedRacks) {

        Set<String> unusedRacks = new HashSet<>(clusterRacks);

        for (String rack : clusterRacks) {
            if (usedRacks.contains(rack)) {
                unusedRacks.remove(rack);
            }
        }

        return unusedRacks;
    }

    /**
     * 批量构建物理集群RackVO
     * @param cluster   物理集群
     * @param racks     Rack列表
     * @param usedFlags 使用标示
     * @return
     */
    private List<PhyClusterRackVO> batchBuildClusterRackVOs(String cluster, Set<String> racks, Integer usedFlags) {

        return racks.stream().map(rack -> buildClusterRackVO(cluster, rack, usedFlags)).collect(Collectors.toList());
    }

    /**
     * 对于逻辑集群绑定的物理集群的版本进行一致性校验
     *
     * @param param 逻辑集群Region
     * @return
     */
    private Result<Void> boundPhyClusterVersionsCheck(ESLogicClusterWithRegionDTO param) {
        Set<String> boundPhyClusterVersions = Sets.newHashSet();
        for (ClusterRegionDTO clusterRegionDTO : param.getClusterRegionDTOS()) {
            ClusterPhy clusterPhy = clusterPhyService.getClusterByName(clusterRegionDTO.getPhyClusterName());
            if (clusterPhy == null) {
                return Result.buildFail("region对应的物理集群信息为空");
            }

            if (clusterPhy.getEsVersion() == null) {
                return Result.buildFail("region对应的物理集群信息对应的版本号不不存在");
            }
            boundPhyClusterVersions.add(clusterPhy.getEsVersion());
        }

        if (boundPhyClusterVersions.size() != 1) {
            return Result.buildFail("逻辑集群绑定的物理集群的版本号应该一致");
        }

        return Result.buildSucc();
    }

    /**
     * 构建物理集群RackVO
     * @param cluster   物理集群
     * @param rack      rack信息
     * @param usedFlags 使用标示
     * @return
     */
    private PhyClusterRackVO buildClusterRackVO(String cluster, String rack, Integer usedFlags) {
        PhyClusterRackVO clusterRack = new PhyClusterRackVO();
        clusterRack.setCluster(cluster);
        clusterRack.setRack(rack);
        clusterRack.setUsageFlags(usedFlags);
        return clusterRack;
    }

    private LogicClusterRackVO buildLogicClusterRackVO(ClusterLogicRackInfo clusterLogicRackInfo) {
        if (clusterLogicRackInfo == null) {
            return null;
        }

        LogicClusterRackVO logicClusterRackVO = new LogicClusterRackVO();
        logicClusterRackVO.setResourceId(clusterLogicRackInfo.getLogicClusterId());
        logicClusterRackVO.setCluster(clusterLogicRackInfo.getPhyClusterName());
        logicClusterRackVO.setRack(clusterLogicRackInfo.getRack());
        return logicClusterRackVO;
    }

    /**
     * 校验region是否可以被逻辑集群绑定
     * @param clusterLogicId         逻辑集群Id
     * @param clusterRegionDTO       region信息
     * @param clusterLogicType       逻辑集群类型
     */
    private void checkCanBeBound(Long clusterLogicId, ClusterRegionDTO clusterRegionDTO, Integer clusterLogicType) {
        Result<Boolean> validResult = clusterContextManager.canClusterLogicAssociatedPhyCluster(clusterLogicId,
            clusterRegionDTO.getPhyClusterName(), clusterRegionDTO.getId(), clusterLogicType);
        if (validResult.failed()) {
            throw new AriusRunTimeException(validResult.getMessage(), FAIL);
        }
    }

    private Result<Void> doBindRegionToClusterLogic(ESLogicClusterWithRegionDTO param, String operator) {
        List<ClusterRegionDTO> clusterRegionDTOS = param.getClusterRegionDTOS();
        if (CollectionUtils.isEmpty(clusterRegionDTOS)) {
            return Result.buildParamIllegal("region相关参数非法");
        }

        for (ClusterRegionDTO clusterRegionDTO : clusterRegionDTOS) {
            Result<Void> bindRegionResult = regionRackService.bindRegion(clusterRegionDTO.getId(), param.getId(), null,
                    operator);
            if (bindRegionResult.failed()) {
                throw new AriusRunTimeException(bindRegionResult.getMessage(), FAIL);
            }
        }

        return Result.buildSucc();
    }

    /**
     * 1. 逻辑集群无关联物理集群, 直接清理
     * 2. (共享类型)逻辑集群已关联物理集群, 新关联的物理集群添加逻辑集群已有索引服务
     * @param param             region实体
     * @param operator          操作者
     * @return
     */
    private void initTemplateSrvOfClusterPhy(ESLogicClusterWithRegionDTO param, String operator) {
        
        ClusterLogicContext clusterLogicContext = clusterContextManager.getClusterLogicContext(param.getId());
        if (null == clusterLogicContext) {
            LOGGER.error(
                "class=ClusterRegionManagerImpl||method=initTemplateSrvOfClusterPhy||clusterLogicId={}||errMsg=clusterLogicContext is empty",
                param.getId());
            return;
        }
        
        List<ClusterRegionDTO> clusterRegionDTOS = param.getClusterRegionDTOS();
        List<String> associatedClusterPhyNames = clusterLogicContext.getAssociatedClusterPhyNames();
        if (CollectionUtils.isEmpty(associatedClusterPhyNames)) {
            clearTemplateSrvOfClusterPhy(param.getId(), associatedClusterPhyNames, clusterRegionDTOS, operator);
        } else {
            addTemplateSrvToNewClusterPhy(param.getId(), associatedClusterPhyNames, clusterRegionDTOS, operator);
        }
    }

    /**
     * (共享类型)逻辑集群已关联物理集群, 新关联的物理集群默开启逻辑集群已有索引服务
     * @param clusterLogicId               逻辑集群ID
     * @param associatedClusterPhyNames    已关联物理集群名称
     * @param clusterRegionDTOS            region信息
     * @param operator                     操作者
     */
    private void addTemplateSrvToNewClusterPhy(Long clusterLogicId, List<String> associatedClusterPhyNames,
                                               List<ClusterRegionDTO> clusterRegionDTOS, String operator) {
        //获取已有逻辑集群索引服务
        List<Integer> clusterTemplateSrvIdList = templateSrvManager.getPhyClusterTemplateSrvIds(associatedClusterPhyNames.get(0));

        //更新已有新绑定物理集群中的索引服务
        for (ClusterRegionDTO clusterRegionDTO : clusterRegionDTOS) {
            if (associatedClusterPhyNames.contains(clusterRegionDTO.getPhyClusterName())) {
                continue;
            }

            try {
                String phyClusterName = clusterRegionDTO.getPhyClusterName();
                templateSrvManager.replaceTemplateServes(phyClusterName, clusterTemplateSrvIdList, operator);
            } catch (Exception e) {
                LOGGER.error(
                    "class=ClusterRegionManagerImpl||method=addTemplateSrvToNewClusterPhy||clusterLogicId={}||clusterPhy={}||errMsg={}",
                    clusterLogicId, clusterRegionDTO.getPhyClusterName(), e.getMessage());
            }
        }
    }

    /**
     * 逻辑集群无关联物理集群, 清理绑定物理集群索引服务
     * @param clusterLogicId               逻辑集群Id
     * @param associatedClusterPhyNames    已关联物理集群名称
     * @param clusterRegionDTOS             region信息
     * @param operator                      操作者
     */
    private void clearTemplateSrvOfClusterPhy(Long clusterLogicId, List<String> associatedClusterPhyNames,
                                              List<ClusterRegionDTO> clusterRegionDTOS, String operator) {
        for (ClusterRegionDTO clusterRegionDTO : clusterRegionDTOS) {
            if (associatedClusterPhyNames.contains(clusterRegionDTO.getPhyClusterName())) {
                continue;
            }
            
            try {
                templateSrvManager.delAllTemplateSrvByClusterPhy(clusterRegionDTO.getPhyClusterName(), operator);
            } catch (Exception e) {
                LOGGER.error(
                        "class=ClusterRegionManagerImpl||method=clearTemplateSrvOfClusterPhy||clusterLogicId={}||errMsg={}",
                        clusterLogicId, e.getMessage());
            }
        }
    }
}
