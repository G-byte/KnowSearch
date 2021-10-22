package com.didichuxing.datachannel.arius.admin.metadata.job.cluster.monitor.esMonitorJob.metrics;

import com.didichuxing.datachannel.arius.admin.common.bean.entity.stats.ESDataTempBean;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.stats.ESNodeToIndexTempBean;
import com.didichuxing.datachannel.arius.admin.common.Tuple;
import com.google.common.collect.Maps;

import java.util.Map;

public class MetricsRegister {
    /**
     * 缓存普通的采集自es 发送给odin的数据
     */
    private Map<String, ESDataTempBean>         dataBeanRegister;
    /**
     * 缓存采集自一个node上的索引的指标数据
     */
    private Map<String, ESNodeToIndexTempBean>  nodeIndexRegister;
    /**
     * 缓存需要复合计算的值
     */
    private Map<String, Double>                 computeValueRegister;

    private Map<String, Tuple<Long, Double>>    clusterNodeCpu;

    public MetricsRegister(){
        dataBeanRegister        = Maps.newConcurrentMap();
        nodeIndexRegister       = Maps.newConcurrentMap();
        computeValueRegister    = Maps.newConcurrentMap();
        clusterNodeCpu          = Maps.newConcurrentMap();
    }

    public void putBeforeNodeToIndexData(String key, ESNodeToIndexTempBean data) {
        nodeIndexRegister.put(key, data);
    }

    public ESNodeToIndexTempBean getBeforeNodeToIndexData(String key) {
        return nodeIndexRegister.get(key);
    }

    public void putBeforeEsData(String key, ESDataTempBean data) {
        dataBeanRegister.put(key, data);
    }

    public ESDataTempBean getBeforeEsData(String key) {
        return dataBeanRegister.get(key);
    }

    public void putBeforeComputeData(String key, Double data) {
        computeValueRegister.put(key, data);
    }

    public Double getBeforeComputeData(String key) {
        return computeValueRegister.get(key);
    }

    public void clearComputeValueRegister() {
        computeValueRegister.clear();
    }

    public void putNodeCpu(String ip, Tuple<Long, Double> cpu){
        clusterNodeCpu.put(ip, cpu);
    }

    public Tuple<Long, Double> getNodeCpu(String ip){
        return clusterNodeCpu.get(ip);
    }
}
