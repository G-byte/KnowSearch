package com.didichuxing.datachannel.arius.admin.metadata.service;

import com.didichuxing.datachannel.arius.admin.common.Tuple;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.metrics.percentiles.BasePercentilesMetrics;
import com.didichuxing.datachannel.arius.admin.common.constant.PercentilesEnum;
import com.didichuxing.datachannel.arius.admin.common.util.FutureUtil;
import com.didichuxing.datachannel.arius.admin.persistence.es.index.dao.gateway.GatewayNodeMetricsDAO;
import com.didichuxing.datachannel.arius.admin.persistence.es.index.dao.stats.AriusStatsClusterInfoESDAO;
import com.didichuxing.datachannel.arius.admin.persistence.es.index.dao.stats.AriusStatsNodeInfoESDAO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by linyunan on 2021-08-05
 */
@Service
public class ESClusterPhyStaticsService {

    @Autowired
    private AriusStatsClusterInfoESDAO ariusStatsClusterInfoEsDao;

    @Autowired
    private AriusStatsNodeInfoESDAO    ariusStatsNodeInfoESDAO;
    @Autowired
    private GatewayNodeMetricsDAO gatewayNodeMetricsDAO;

    private static final FutureUtil<Void> futureUtil = FutureUtil.init("ESClusterPhyStaticsService", 10,10,200);

    /**
     * 获取集群维度分位统计信息
     * @param clusterName            集群名称
     * @param clusterMetricsType     根据指标名称去ES索引中匹配指标，匹配则返回，否则为空
     * @param aggType                聚合类型
     * @param startTime              开始时间
     * @param endTime                结束时间
     * @return                       分位图实体
     */
    public List<BasePercentilesMetrics> getAggPercentilesMetrics(String clusterName, String clusterMetricsType,
                                                                 String aggType, Long startTime, Long endTime) {
        AtomicReference<Map<Long, Double>>   avgAtomic      = new AtomicReference<>();
        AtomicReference<Map<Long, Double>>   st99Atomic     = new AtomicReference<>();
        AtomicReference<Map<Long, Double>>   st95Atomic     = new AtomicReference<>();
        AtomicReference<Map<Long, Double>>   st75Atomic     = new AtomicReference<>();
        AtomicReference<Map<Long, Double>>   st55Atomic     = new AtomicReference<>();

        futureUtil.runnableTask(() -> avgAtomic.set(ariusStatsClusterInfoEsDao.getAggSinglePercentilesMetrics(clusterName,
                clusterMetricsType, aggType, PercentilesEnum.AVG.getType(), startTime, endTime)))

                .runnableTask(() -> st99Atomic.set(ariusStatsClusterInfoEsDao.getAggSinglePercentilesMetrics(clusterName,
                        clusterMetricsType, aggType, PercentilesEnum.ST99.getType(), startTime, endTime)))

                .runnableTask(() -> st95Atomic.set(ariusStatsClusterInfoEsDao.getAggSinglePercentilesMetrics(clusterName,
                        clusterMetricsType, aggType, PercentilesEnum.ST95.getType(), startTime, endTime)))

                .runnableTask(() -> st75Atomic.set(ariusStatsClusterInfoEsDao.getAggSinglePercentilesMetrics(clusterName,
                        clusterMetricsType, aggType, PercentilesEnum.ST75.getType(), startTime, endTime)))

                .runnableTask(() -> st55Atomic.set(ariusStatsClusterInfoEsDao.getAggSinglePercentilesMetrics(clusterName,
                        clusterMetricsType, aggType, PercentilesEnum.ST55.getType(), startTime, endTime)))
                .waitExecute();

        Map<Long, Double> timeSlip2AvgValueMap  = avgAtomic.get();
        Map<Long, Double> timeSlip2St99ValueMap = st99Atomic.get();
        Map<Long, Double> timeSlip2St95ValueMap = st95Atomic.get();
        Map<Long, Double> timeSlip2St75ValueMap = st75Atomic.get();
        Map<Long, Double> timeSlip2St55ValueMap = st55Atomic.get();

        List<BasePercentilesMetrics> basePercentilesMetricList = new ArrayList<>(timeSlip2AvgValueMap.keySet().size());
        timeSlip2AvgValueMap.forEach((timeSlip, value) -> {
            BasePercentilesMetrics basePercentilesMetrics = new BasePercentilesMetrics();
            basePercentilesMetrics.setTimeStamp(timeSlip);
            basePercentilesMetrics.setAggType(value);
            basePercentilesMetricList.add(basePercentilesMetrics);
        });

        for (BasePercentilesMetrics basePercentilesMetrics : basePercentilesMetricList) {
            Double value99 = timeSlip2St99ValueMap.get(basePercentilesMetrics.getTimeStamp());
            Double value95 = timeSlip2St95ValueMap.get(basePercentilesMetrics.getTimeStamp());
            Double value75 = timeSlip2St75ValueMap.get(basePercentilesMetrics.getTimeStamp());
            Double value55 = timeSlip2St55ValueMap.get(basePercentilesMetrics.getTimeStamp());
            basePercentilesMetrics.setSt99(null == value99 ? 0 : value99);
            basePercentilesMetrics.setSt95(null == value95 ? 0 : value95);
            basePercentilesMetrics.setSt75(null == value75 ? 0 : value75);
            basePercentilesMetrics.setSt55(null == value55 ? 0 : value55);
        }

        return basePercentilesMetricList;
    }

    /**
     * 获取集群统计信息
     * @param clusterName 集群名称
     * @param aggType     聚合类型
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @param clazz       根据类中属性名称去ES索引中匹配指标，匹配到则返回，否则为空
     * @return            clazz
     */
    public <T> List<T> getAggClusterPhyMetrics(String clusterName, String aggType, Long startTime, Long endTime,
                                               Class<T> clazz) {
        return ariusStatsClusterInfoEsDao.getAggClusterPhyMetrics(clusterName, aggType, startTime, endTime, clazz);
    }

    /**
     * 获取集群写入总耗时
     * @param  cluster 集群名称
     * @return double
     */
    public double getClusterIndexingLatency(String cluster) {
        return ariusStatsNodeInfoESDAO.getClusterIndexingLatency(cluster);
    }

    /**
     * 获取集群查询总耗时
     * @param  cluster  集群名称
     * @return double
     */
    public double getClusterSearchLatency(String cluster) {
        return ariusStatsNodeInfoESDAO.getClusterSearchLatency(cluster);
    }
    
    
    /**
     * 获取集群shard总数
     *
     * @param cluster 集群
     * @return {@code Long}
     */
    public Long getClustersShardTotal(String cluster) {
        return ariusStatsClusterInfoEsDao.getClustersShardTotal(cluster);
    }
    
    /**
     * 获取pending task数量
     *
     * @param cluster 集群
     * @return {@code Long}
     */
    public Long getPendingTaskTotal(String cluster) {
        return ariusStatsClusterInfoEsDao.getPendingTaskTotal(cluster);
    }
    
    /**
     * 获取网关成功率和失败率
     *
     * @param cluster 集群
     * @return {@code Tuple<Double, Double>} tuple.1:成功率；tuple.2:失败率
     */
    public Tuple<Double/*成功率*/, Double/*失败率*/> getGatewaySuccessRateAndFailureRate(String cluster) {
        return gatewayNodeMetricsDAO.getGatewaySuccessRateAndFailureRate(cluster);
    }
    
    
    /**
     * 写入请求数
     *
     * @param cluster 集群
     * @return {@link Long}
     */
    public Long getCurrentIndexTotal(String cluster) {
        return ariusStatsNodeInfoESDAO.getCurrentIndexTotal(cluster);
    }

    /**
     * 查询请求数突增量
     *
     * @param cluster 集群
     * @return {@link Long}
     */
    public Long getCurrentQueryTotal(String cluster) {
        return ariusStatsNodeInfoESDAO.getCurrentQueryTotal(cluster);
    }
    
    /**
     * 集群http连接数
     *
     * @param cluster 集群
     * @return {@link Long}
     */
    public Long getHttpConnectionTotal(String cluster) {
        return ariusStatsNodeInfoESDAO.getHttpConnectionTotal(cluster);
    }
}