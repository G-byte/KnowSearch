package com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster;

import java.util.List;

import com.didichuxing.datachannel.arius.admin.client.constant.resource.ESClusterTypeEnum;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.BaseEntity;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ecm.RoleCluster;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ecm.RoleClusterHost;
import com.didichuxing.datachannel.arius.admin.common.constant.template.TemplateServiceEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClusterPhy extends BaseEntity implements Comparable<ClusterPhy>{

    /**
     * 集群id
     */
    private Integer               id;

    /**
     * 集群名字
     */
    private String                cluster;

    /**
     * 描述
     */
    private String                desc;

    /**
     * TCP读地址
     */
    private String                readAddress;

    /**
     * TCP写地址
     */
    private String                writeAddress;

    /**
     * http读地址
     */
    private String                httpAddress;

    /**
     * http写地址
     */
    private String                httpWriteAddress;

    /**
     * 集群类型
     * @see ESClusterTypeEnum
     */
    private Integer               type;

    /**
     * 数据中心
     */
    private String                dataCenter;

    /**
     * 机房
     */
    private String                idc;

    /**
     * 服务等级
     */
    private Integer               level;

    /**
     * es版本
     */
    private String                esVersion;

    /**
     * 配置包Id
     */
    private Long                  cfgId;

    /**
     * 插件Id列表
     */
    private String                plugIds;

    /**
     * 程序包Id
     */
    private Long                  packageId;

    /**
     * 镜像名称
     */
    private String                imageName;

    /**
     * odin 节点
     */
    private String                nsTree;

    /**
     *节点数量
     */
    private Integer               pidCount;
    /**
     * 机器规格
     */
    private String                machineSpec;

    /**
     * 集群支持的索引服务
     * @see TemplateServiceEnum
     */
    private String                templateSrvs;

    /**
     * 集群密码
     */
    private String                password;

    /**
     * 集群创建人
     */
    private String                creator;

    /**
     * ES的角色
     */
    private List<RoleCluster>     roleClusters;

    /**
     * ES的角色的机器节点
     */
    private List<RoleClusterHost> roleClusterHosts;

    /**
     * client运行模式，读写共享还是读写分离
     */
    private Integer               runMode;

    /**
     * 读写分离时候，指定action（获取写client）
     */
    private String                writeAction;

    /**
     * 集群状态
     */
    private Integer               health;

    @Override
    public int compareTo(ClusterPhy o) {
        if (null == o) {
            return 0;
        }

        return o.getId().intValue() - this.getId().intValue();
    }
}
