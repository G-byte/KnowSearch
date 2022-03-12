package com.didichuxing.datachannel.arius.admin.client.bean.dto.cluster;

import com.didichuxing.datachannel.arius.admin.client.bean.dto.PageDTO;
import com.didichuxing.datachannel.arius.admin.client.constant.resource.ResourceLogicTypeEnum;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author d06679
 * @date 2019/3/22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description ="逻辑集群信息")
public class ESLogicClusterDTO extends PageDTO {

    @ApiModelProperty("逻辑集群ID")
    private Long    id;

    @ApiModelProperty("逻辑集群名字")
    private String  name;

    /**
     * @see ResourceLogicTypeEnum
     */
    @ApiModelProperty("类型(1:公共：2:独立, 3:独占)")
    private Integer type;

    @ApiModelProperty("所属应用ID")
    private Integer appId;

    @ApiModelProperty("数据中心")
    private String  dataCenter;

    @ApiModelProperty("数据节点个数")
    private Integer  dataNodeNu;

    @ApiModelProperty("责任人")
    private String  responsible;

    @ApiModelProperty("成本部门ID")
    private String  libraDepartmentId;

    @ApiModelProperty("成本部门名称")
    private String  libraDepartment;

    @ApiModelProperty("备注")
    private String  memo;

    @ApiModelProperty("服务等级")
    private Integer level;

    @ApiModelProperty("配额")
    private Double  quota;

    @ApiModelProperty("配置")
    private String  configJson;

    @ApiModelProperty("健康状态 1 green 2 yellow 3 red -1 未知")
    private Integer health;
}
