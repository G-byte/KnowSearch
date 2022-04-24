package com.didichuxing.datachannel.arius.admin.core.service.es.impl;

import static com.didichuxing.datachannel.arius.admin.common.constant.ClusterPhyMetricsContant.BIG_SHARD;
import static com.didichuxing.datachannel.arius.admin.common.constant.metrics.ESHttpRequestContent.*;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.rest.RestStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.didichuxing.datachannel.arius.admin.common.Tuple;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.metrics.ordinary.MovingShardMetrics;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.metrics.ordinary.ShardMetrics;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.shard.Segments;
import com.didichuxing.datachannel.arius.admin.common.bean.po.shard.SegmentsPO;
import com.didichuxing.datachannel.arius.admin.common.util.ConvertUtil;
import com.didichuxing.datachannel.arius.admin.core.service.es.ESShardService;
import com.didichuxing.datachannel.arius.admin.persistence.es.cluster.ESShardDAO;
import com.didiglobal.logi.elasticsearch.client.gateway.direct.DirectResponse;
import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;
import com.google.common.collect.Lists;

/**
 * Created by linyunan on 3/22/22
 */
@Service
public class ESShardServiceImpl implements ESShardService {

    private static final ILog LOGGER = LogFactory.getLog(ESShardServiceImpl.class);
    
    @Autowired
    private ESShardDAO esShardDAO;

    @Override
    public List<MovingShardMetrics> syncGetMovingShards(String clusterName) {
        DirectResponse directResponse = esShardDAO.getDirectResponse(clusterName, "Get", GET_MOVING_SHARD);

        List<MovingShardMetrics> movingShardsMetrics = Lists.newArrayList();
        if (directResponse.getRestStatus() == RestStatus.OK
                && StringUtils.isNoneBlank(directResponse.getResponseContent())) {

            movingShardsMetrics = ConvertUtil.str2ObjArrayByJson(directResponse.getResponseContent(),
                    MovingShardMetrics.class);

        }
        return movingShardsMetrics;
    }

    @Override
    public List<ShardMetrics> syncGetBigShards(String clusterName) {
        List<ShardMetrics> shardsMetrics = getShardMetrics(clusterName);
        return shardsMetrics.stream().filter(this::filterBigShard).collect(Collectors.toList());
    }

    @Override
    public List<ShardMetrics> syncGetSmallShards(String clusterName) {
        List<ShardMetrics> shardsMetrics = getShardMetrics(clusterName);
        return shardsMetrics.stream().filter(this::filterSmallShard).collect(Collectors.toList());
    }

    @Override
    public Tuple</*大shard列表*/List<ShardMetrics>, /*小shard列表*/List<ShardMetrics>> syncGetBigAndSmallShards(String clusterName) {
        List<ShardMetrics> shardsMetrics = getShardMetrics(clusterName);
        Tuple<List<ShardMetrics>, List<ShardMetrics>> tuple = new Tuple<>();
        tuple.setV1(shardsMetrics.stream().filter(this::filterBigShard).collect(Collectors.toList()));
        tuple.setV2(shardsMetrics.stream().filter(this::filterSmallShard).collect(Collectors.toList()));
        return tuple;
    }

    @Override
    public List<Segments> syncGetSegments(String clusterName) {
        String segmentsPartInfoRequestContent = getSegmentsPartInfoRequestContent();
        List<SegmentsPO> segmentsPOS = esShardDAO.commonGet(clusterName, segmentsPartInfoRequestContent, SegmentsPO.class);
        return ConvertUtil.list2List(segmentsPOS, Segments.class);
    }

    /*********************************************private******************************************/
    @NotNull
    private List<ShardMetrics> getShardMetrics(String clusterName) {
        String shardsRequestContent = getShardsAllInfoRequestContent("20s");
        return esShardDAO.commonGet(clusterName, shardsRequestContent, ShardMetrics.class);
    }

    private boolean filterBigShard(ShardMetrics shardMetrics) {
        if (null == shardMetrics) { return false;}

        String store = shardMetrics.getStore();
        if (null == store) { return false;}
        String value = store.substring(0, store.length() - 2);
        if (store.endsWith("tb")) {
            value += "1024";
            return BIG_SHARD <= Double.valueOf(value);
        }else if (store.endsWith("gb")){
            return BIG_SHARD <= Double.valueOf(value);
        }else {
            return false;
        }
    }

    private boolean filterSmallShard(ShardMetrics shardMetrics) {
        if (null == shardMetrics) { return false;}
        String store = shardMetrics.getStore();
        if (null == store) { return false;}
        return !store.endsWith("tb") && !store.endsWith("gb");
    }
}
