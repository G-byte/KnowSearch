package com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ecm;

import java.util.List;

import com.didichuxing.datachannel.arius.admin.common.bean.entity.BaseEntity;

import lombok.Data;

/**
 * ES集群对应角色集群  实体
 * @author didi
 * @since 2020-10-20
 */
@Data
public class ESRoleCluster extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * elastic_cluster外键ID
     */
    private Long elasticClusterId;

    /**
     * role集群名称
     */
    private String roleClusterName;

    /**
     * 集群角色(master-node/data-node/client-node)
     */
    private String role;

    /**
     * pod数量
     */
    private Integer podNumber;

    /**
     * 单机实例数
     */
    private Integer pidCount;

    /**
     * 机器规格
     */
    private String machineSpec;

    /**
     * ES版本
     */
    private String esVersion;

    /**
     * 配置包ID
     */
    private Integer cfgId;

    /**
     * 插件包ID列表
     */
    private String plugIds;

    /**
     * 标记删除
     */
    private Boolean deleteFlag;

    /**
     * role所拥有的节点
     */
    private List<ESRoleClusterHost> esRoleClusterHosts;
}
