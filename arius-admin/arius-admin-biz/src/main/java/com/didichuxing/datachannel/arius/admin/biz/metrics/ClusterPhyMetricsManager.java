package com.didichuxing.datachannel.arius.admin.biz.metrics;

import java.util.List;

import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.metrics.*;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.metrics.other.cluster.ESClusterTaskDetailVO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.metrics.top.VariousLineChartMetricsVO;
import com.didichuxing.datachannel.arius.admin.common.constant.metrics.ClusterPhyTypeMetricsEnum;

/**
 * Created by linyunan on 2021-07-30
 *
 *  * 物理集群指标看板业务类
 *  * 1. 查询集群维度指标数据
 *  * 2. 查询集群节点维度指标数据
 *  * 3. 查询集群索引维度指标数据
 */
public interface ClusterPhyMetricsManager {

    /**
     * 获取一级指标类型列表 key:type value:code
     * @param type 类型
     * @see ClusterPhyTypeMetricsEnum
     */
    List<String> getMetricsCode2TypeMap(String type);

    /**
     * 获取指定类型的指标
     * @param domainAccount 账号信息
     * @param appId appId
     * @param param 物理集群指标
     * @param metricsTypeEnum 指标处理器类型
     * @return result
     */
    <T> Result<T> getClusterMetricsByMetricsType(MetricsClusterPhyDTO param, Integer appId, String domainAccount, ClusterPhyTypeMetricsEnum metricsTypeEnum);

    /**
     * 获取物理集群多个节点的指标信息
     * @param param 物理集群指标
     * @param appId appId
     * @param domainAccount 账号信息
     * @param metricsTypeEnum 指标处理器类型
     * @return result
     */
    Result<List<VariousLineChartMetricsVO>> getMultiClusterMetrics(MultiMetricsClusterPhyNodeDTO param, Integer appId, String domainAccount, ClusterPhyTypeMetricsEnum metricsTypeEnum);

    /**
     * 获取物理集群中的索引列表
     */
    Result<List<String>> getClusterPhyIndexName(String clusterPhyName, Integer appId);

    /**
     * 获取账号下已配置指标类型
     */
    List<String> getDomainAccountConfigMetrics(MetricsConfigInfoDTO param, String domainAccount);

    /**
     * 更新账号下已配置的指标类型
     */
    Result<Integer> updateDomainAccountConfigMetrics(MetricsConfigInfoDTO param, String domainAccount);

    /**
     * 获取物理集群中的索引列表
     */
    Result<List<ESClusterTaskDetailVO>> getClusterPhyTaskDetail(String clusterPhyName, String node, String startTime, String endTime, Integer appId);
}
