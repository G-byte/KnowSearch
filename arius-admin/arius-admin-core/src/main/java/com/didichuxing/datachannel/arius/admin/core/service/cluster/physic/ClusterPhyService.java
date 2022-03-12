package com.didichuxing.datachannel.arius.admin.core.service.cluster.physic;

import java.util.List;
import java.util.Set;

import com.didichuxing.datachannel.arius.admin.client.bean.common.ESPlugin;
import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.cluster.ClusterPhyConditionDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.cluster.ClusterSettingDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.cluster.ESClusterDTO;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ClusterPhy;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ecm.RoleCluster;
import com.didichuxing.datachannel.arius.admin.common.constant.cluster.ClusterHealthEnum;
import com.didichuxing.datachannel.arius.admin.common.exception.ESOperateException;

public interface ClusterPhyService {

    /**
     * 条件查询物理集群
     * @param params 条件
     * @return 集群列表
     *
     */
    List<ClusterPhy> listClustersByCondt(ESClusterDTO params);

    /**
     * 删除物理集群
     * @param clusterId 集群id
     * @param operator 操作人
     * @return 成功 true 失败 false
     */
    Result<Boolean> deleteClusterById(Integer clusterId, String operator);

    /**
     * 新建物理集群
     * @param param 集群信息
     * @param operator 操作人
     * @return 成功 true 失败 false
     */
    Result<Boolean> createCluster(ESClusterDTO param, String operator);

    /**
     * 编辑物理集群信息
     * @param param 物理集群信息
     * @param operator 操作人
     * @return 成功 true 失败 false
     *
     */
    Result<Boolean> editCluster(ESClusterDTO param, String operator);

    /**
     * 根据集群名字查询集群
     * @param clusterName 集群名字
     * @return 集群
     */
    ClusterPhy getClusterByName(String clusterName);

    /**
     * 更新物理集群插件列表
     * @param pluginIds 插件id列表
     * @param phyClusterId 物理集群id
     */
    Result<Void> updatePluginIdsById(String pluginIds, Integer phyClusterId);

    /**
     * 列出所有集群
     * @return 集群列表,如果没有返回空列表
     */
    List<ClusterPhy> listAllClusters();

    /**
     * 集群是否存在
     *
     * @param clusterName 集群名字
     * @return true 存在
     */
    boolean isClusterExists(String clusterName);

    /**
     * rack是否存在
     *
     * @param cluster 集群名字
     * @param racks   rack名字  支持逗号间隔
     * @return true 存在
     */
    default boolean isRacksExists(String cluster, String racks) {
        return true;
    }

    /**
     * 获取集群全部的rack
     * @param cluster cluster
     * @return set
     */
    Set<String> getClusterRacks(String cluster);

    /**
     * 获取集群热存Rack列表
     * @param cluster 集群名称
     * @return
     */
    Set<String> listHotRacks(String cluster);

    /**
     * 获取冷存Rack列表
     * @param cluster 集群名称
     * @return
     */
    Set<String> listColdRacks(String cluster);

    /**
     * 获取集群插件列表
     * @param cluster 集群名称
     * @return
     */
    List<ESPlugin> listClusterPlugins(String cluster);

    /**
     * 查询指定集群
     * @param phyClusterId 集群id
     * @return 集群  不存在返回null
     */
    ClusterPhy getClusterById(Integer phyClusterId);

    /**
     * 获取写节点的个数
     * @param cluster 集群
     * @return count
     */
    int getWriteClientCount(String cluster);

    /**
     * 确保集群配置了DCDR的远端集群地址，如果没有配置尝试配置
     * @param cluster 集群
     * @param remoteCluster 远端集群
     * @throws ESOperateException
     * @return
     */
    boolean ensureDcdrRemoteCluster(String cluster, String remoteCluster) throws ESOperateException;

    /**
     * 获取物理集群角色
     * @param clusterId  物理集群ID
     * @return 物理集群的角色列表
     */
    List<RoleCluster> listPhysicClusterRoles(Integer clusterId);

    /**
     * 更新集群的动态配置信息
     * @param param 更新集群下动态配置项的信息
     * @return result
     */
    Result<Boolean> updatePhyClusterDynamicConfig(ClusterSettingDTO param);

    /**
     * 获取集群路由的属性，例如rack1
     * @param cluster 集群名称
     * @return 属性的列表
     */
    Set<String> getRoutingAllocationAwarenessAttributes(String cluster);

    /**
     * 模糊分页查询物理集群列表信息，仅获取部分属性
     */
    List<ClusterPhy> pagingGetClusterPhyByCondition(ClusterPhyConditionDTO param);

    /**
     * 模糊查询统计总命中数
     * @param param 模糊查询条件
     * @return
     */
    Long fuzzyClusterPhyHitByCondition(ClusterPhyConditionDTO param);
}
