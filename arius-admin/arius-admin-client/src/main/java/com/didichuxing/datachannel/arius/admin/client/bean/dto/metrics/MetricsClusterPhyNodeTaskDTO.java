package com.didichuxing.datachannel.arius.admin.client.bean.dto.metrics;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author didi
 * @date 2022-01-15 4:20 下午
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "节点task指标信息")
public class MetricsClusterPhyNodeTaskDTO extends MetricsClusterPhyNodeDTO {

    @ApiModelProperty("集群节点ip")
    private List<String> aggTypes;
}
