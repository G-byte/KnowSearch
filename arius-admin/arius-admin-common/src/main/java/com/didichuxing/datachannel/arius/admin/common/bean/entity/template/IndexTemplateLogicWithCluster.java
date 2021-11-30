package com.didichuxing.datachannel.arius.admin.common.bean.entity.template;

import java.util.List;

import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ClusterLogic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author d06679
 * @date 2019/3/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexTemplateLogicWithCluster extends IndexTemplateLogic {
    /**
     * 逻辑集群信息
     */
    private List<ClusterLogic> logicClusters;
}
