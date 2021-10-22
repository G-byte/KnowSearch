package com.didichuxing.datachannel.arius.admin.biz.workorder.content;

import lombok.Data;

@Data
public class LogicClusterCreateContent extends BaseContent {
    /**
     * 集群名称
     */
    private String  name;

    /**
     * 集群类型
     */
    private Integer type;

    /**
     * 服务等级
     */
    private Integer level;

    /**
     * 数据中心
     */
    private String  dataCenter;

    /**
     * dataNode的规格
     */
    private String  dataNodeSpec;

    /**
     * dataNode的个数
     */
    private int     dataNodeNu;

    /**
     * 责任人
     */
    private String  responsible;

    /**
     * 备注
     */
    private String  memo;

    /**
     * 插件上传
     */
    private String  plugins;

    /**
     * 部门id
     */
    private String  libraDepartmentId;

    /**
     * 部门名称
     */
    private String  libraDepartment;

}
