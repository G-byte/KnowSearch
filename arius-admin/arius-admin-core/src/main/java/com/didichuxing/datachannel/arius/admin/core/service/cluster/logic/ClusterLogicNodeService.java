package com.didichuxing.datachannel.arius.admin.core.service.cluster.logic;

import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ecm.RoleClusterHost;

import java.util.List;

/**
 * 逻辑集群节点服务
 * @author wangshu
 * @date 2020/09/10
 */
public interface ClusterLogicNodeService {
    /**
     * 获取逻辑集群所有节点列表
     * @param clusterId 逻辑集群ID
     * @return
     */
    List<RoleClusterHost> getLogicClusterNodes(Long clusterId);

    /**
     * 获取逻辑集群所有节点包括对应物理集群非数据节点
     * @param clusterId 逻辑集群ID
     * @return
     */
    List<RoleClusterHost> getLogicClusterNodesIncludeNonDataNodes(Long clusterId);
}
