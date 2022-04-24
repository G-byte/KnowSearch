package com.didichuxing.datachannel.arius.admin.client.bean.dto.indices;

import com.didichuxing.datachannel.arius.admin.client.bean.dto.PageDTO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * @author lyn
 * @date 2021/09/29
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "索引条件查询实体")
public class IndicesConditionDTO extends PageDTO {
    @ApiModelProperty("集群名称")
    private List<String> clusterPhyName;

    @ApiModelProperty("索引名称")
    private String index;

    @ApiModelProperty("状态 green yellow red")
    private String health;

    @ApiModelProperty("排序字段(priStoreSize)、主分配个数(pri)、副本个数(rep)、存储大小(storeSize)、文档数量(docsCount)、删除文档数量(docsDeleted)、索引名称（index）")
    private String sortTerm;

    @ApiModelProperty(value = "是否降序排序（默认降序）", dataType = "Boolean", required = false)
    private Boolean orderByDesc = true;
}
