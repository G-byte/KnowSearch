package com.didichuxing.datachannel.arius.admin.core.service.es.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.didichuxing.datachannel.arius.admin.client.bean.common.NodeAllocationInfo;
import com.didichuxing.datachannel.arius.admin.client.bean.common.NodeAttrInfo;
import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.setting.ESClusterGetSettingsAllResponse;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.stats.ECSegmentsOnIps;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.stats.ESClusterStatsResponse;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.stats.ESClusterTaskStatsResponse;
import com.didichuxing.datachannel.arius.admin.common.constant.cluster.ClusterHealthEnum;
import com.didichuxing.datachannel.arius.admin.common.exception.ESOperateException;
import com.didichuxing.datachannel.arius.admin.common.util.AriusObjUtils;
import com.didichuxing.datachannel.arius.admin.common.util.ConvertUtil;
import com.didichuxing.datachannel.arius.admin.common.util.ListUtils;
import com.didichuxing.datachannel.arius.admin.common.util.SizeUtil;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.physic.ClusterPhyService;
import com.didichuxing.datachannel.arius.admin.core.service.es.ESClusterService;
import com.didichuxing.datachannel.arius.admin.persistence.component.ESOpTimeoutRetry;
import com.didichuxing.datachannel.arius.admin.persistence.constant.ESOperateContant;
import com.didichuxing.datachannel.arius.admin.persistence.es.cluster.ESClusterDAO;
import com.didiglobal.logi.elasticsearch.client.ESClient;
import com.didiglobal.logi.elasticsearch.client.gateway.direct.DirectRequest;
import com.didiglobal.logi.elasticsearch.client.gateway.direct.DirectResponse;
import com.didiglobal.logi.elasticsearch.client.response.cluster.ESClusterHealthResponse;
import com.didiglobal.logi.elasticsearch.client.response.cluster.nodes.ClusterNodeInfo;
import com.didiglobal.logi.elasticsearch.client.response.cluster.nodessetting.ClusterNodeSettings;
import com.didiglobal.logi.elasticsearch.client.response.cluster.nodessetting.ESClusterNodesSettingResponse;
import com.didiglobal.logi.elasticsearch.client.response.indices.getalias.ESIndicesGetAliasResponse;
import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.rest.RestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.didichuxing.datachannel.arius.admin.persistence.constant.ESOperateContant.*;

/**
 * @author d06679
 * @date 2019/5/8
 */
@Service
public class ESClusterServiceImpl implements ESClusterService {

    private static final ILog LOGGER = LogFactory.getLog(ESClusterServiceImpl.class);

    @Autowired
    private ESClusterDAO      esClusterDAO;

    @Autowired
    private ClusterPhyService clusterPhyService;

    /**
     * 关闭集群re balance
     * @param cluster    集群
     * @param retryCount 重试次数
     * @return result
     * @throws ESOperateException
     */
    @Override
    public boolean syncCloseReBalance(String cluster, Integer retryCount) throws ESOperateException {
        return ESOpTimeoutRetry.esRetryExecute("syncCloseReBalance", retryCount,
            () -> esClusterDAO.configReBalanceOperate(cluster, "none"));
    }

    /**
     * 打开集群rebalance
     *
     * @param cluster   集群
     * @param esVersion 版本
     * @return result
     * @throws ESOperateException
     */
    @Override
    public boolean syncOpenReBalance(String cluster, String esVersion) throws ESOperateException {
        return ESOpTimeoutRetry.esRetryExecute("syncOpenReBalance", 3,
            () -> esClusterDAO.configReBalanceOperate(cluster, "all"));
    }

    /**
     * 配置远端集群
     *
     * @param cluster       集群
     * @param remoteCluster 远端集群
     * @param tcpAddresses  tcp地址
     * @param retryCount    重试次数
     * @return true/false
     */
    @Override
    public boolean syncPutRemoteCluster(String cluster, String remoteCluster, List<String> tcpAddresses,
                                        Integer retryCount) throws ESOperateException {
        return ESOpTimeoutRetry.esRetryExecute("syncPutRemoteCluster", retryCount,
            () -> esClusterDAO.putPersistentRemoteClusters(cluster,
                String.format(ESOperateContant.REMOTE_CLUSTER_FORMAT, remoteCluster), tcpAddresses));
    }

    /**
     * 判断配置否存在
     *
     * @param cluster         集群
     * @param settingFlatName setting名字
     * @return true/false
     */
    @Override
    public boolean hasSettingExist(String cluster, String settingFlatName) {
        Map<String, Object> clusterSettingMap = esClusterDAO.getPersistentClusterSettings(cluster);
        return clusterSettingMap.containsKey(settingFlatName);
    }

    /**
     * 配置集群的冷存搬迁配置
     *
     * @param cluster    集群
     * @param retryCount 重试次数
     * @return true/false
     * @throws ESOperateException
     */
    @Override
    public boolean syncConfigColdDateMove(String cluster, int inGoing, int outGoing, String moveSpeed,
                                          int retryCount) throws ESOperateException {

        Map<String, Object> configMap = Maps.newHashMap();

        if (inGoing > 0) {
            configMap.put(CLUSTER_ROUTING_ALLOCATION_OUTGOING, outGoing);
        }

        if (outGoing > 0) {
            configMap.put(CLUSTER_ROUTING_ALLOCATION_INGOING, inGoing);
        }

        configMap.put(COLD_MAX_BYTES_PER_SEC_KEY, moveSpeed);

        return ESOpTimeoutRetry.esRetryExecute("syncConfigColdDateMove", retryCount,
            () -> esClusterDAO.putPersistentConfig(cluster, configMap));
    }

    @Override
    public Map<String, List<String>> syncGetNode2PluginsMap(String cluster) {
        return esClusterDAO.getNode2PluginsMap(cluster);
    }

    /**
     * 获取某个集群内索引别名到索引名称的映射
     *
     * @param cluster
     * @return
     */
    @Override
    public Map<String/*alias*/, Set<String>> syncGetAliasMap(String cluster) {
        Map<String, Set<String>> ret = new HashMap<>();

        try {
            ESIndicesGetAliasResponse response = esClusterDAO.getClusterAlias(cluster);
            if(response == null || response.getM() == null) {
                return ret;
            }
            for (String index : response.getM().keySet()) {
                for (String alias : response.getM().get(index).getAliases().keySet()) {
                    if (!ret.containsKey(alias)) {
                        ret.put(alias, new HashSet<>());
                    }
                    ret.get(alias).add(index);
                }
            }
            return ret;
        } catch (Exception t) {
            LOGGER.error("class=ClusterClientPool||method=syncGetAliasMap||clusterName={}||errMsg=fail to get alias", cluster, t);
            return ret;
        }
    }

    @Override
    public int syncGetClientAlivePercent(String cluster, String password, String clientAddresses) {
        if (null == cluster || StringUtils.isBlank(clientAddresses)) {
            return 0;
        }

        List<String> addresses = ListUtils.string2StrList(clientAddresses);

        int alive = 0;
        for (String address : addresses) {
            boolean isAlive = judgeClientAlive(cluster, password, address);
            if (isAlive) {
                alive++;
            }
        }

        return alive * 100 / addresses.size();
    }

    /**
     * 判断es client是否存活
     *
     * @param cluster
     * @param address
     * @return
     */
    @Override
    public boolean judgeClientAlive(String cluster,String password ,String address) {

        String[] hostAndPortStr = StringUtils.split(address, ":");
        if (null == hostAndPortStr || hostAndPortStr.length <= 1) {
            LOGGER.info("class=ClusterClientPool||method=getNotSniffESClient||addresses={}||msg=clusterClientError",
                address);
            return false;
        }

        String host = hostAndPortStr[0];
        String port = hostAndPortStr[1];
        List<InetSocketTransportAddress> transportAddresses = Lists.newArrayList();
        ESClient esClient = new ESClient();
        try  {
            transportAddresses.add(new InetSocketTransportAddress(InetAddress.getByName(host), Integer.parseInt(port)));
            esClient.addTransportAddresses(transportAddresses.toArray(new TransportAddress[0]));
            esClient.setClusterName(cluster);
            if(StringUtils.isNotBlank(password)){
                esClient.setPassword(password);
            }
            esClient.start();

            ESClusterHealthResponse response = esClient.admin().cluster().prepareHealth().execute().actionGet(30,
                TimeUnit.SECONDS);
            return !response.isTimedOut();
        } catch (Exception e) {
            esClient.close();
            LOGGER.error(
                "class=ESClusterServiceImpl||method=judgeClientAlive||cluster={}||client={}||msg=judgeAlive is excepjudgeClientAlivetion!",
                cluster, address, e);
            return false;
        }finally {
            esClient.close();
        }
    }

	@Override
    public ESClusterHealthResponse syncGetClusterHealth(String clusterName) {
        return esClusterDAO.getClusterHealth(clusterName);
    }

    @Override
    public List<ESClusterTaskStatsResponse> syncGetClusterTaskStats(String clusterName) {
        return esClusterDAO.getClusterTaskStats(clusterName);
    }

    @Override
    public ClusterHealthEnum syncGetClusterHealthEnum(String clusterName) {
        ESClusterHealthResponse clusterHealthResponse = esClusterDAO.getClusterHealth(clusterName);

        ClusterHealthEnum clusterHealthEnum = ClusterHealthEnum.UNKNOWN;
        if (clusterHealthResponse != null) {
            clusterHealthEnum = ClusterHealthEnum.valuesOf(clusterHealthResponse.getStatus());
        }
        return clusterHealthEnum;
    }

    @Override
    public ESClusterStatsResponse syncGetClusterStats(String clusterName) {
        return esClusterDAO.getClusterStats(clusterName);
    }

    @Override
    public ESClusterGetSettingsAllResponse syncGetClusterSetting(String cluster) {
        return esClusterDAO.getClusterSetting(cluster);
    }

    @Override
    public Map<String, Integer> synGetSegmentsOfIpByCluster(String clusterName) {
        Map<String, Integer> segmentsOnIpMap = Maps.newHashMap();
        for (ECSegmentsOnIps ecSegmentsOnIp : esClusterDAO.getSegmentsOfIpByCluster(clusterName)) {
            if (segmentsOnIpMap.containsKey(ecSegmentsOnIp.getIp())) {
                Integer newSegments = segmentsOnIpMap.get(ecSegmentsOnIp.getIp()) + Integer.parseInt(ecSegmentsOnIp.getSegment());
                segmentsOnIpMap.put(ecSegmentsOnIp.getIp(), newSegments);
            } else {
                segmentsOnIpMap.put(ecSegmentsOnIp.getIp(), Integer.valueOf(ecSegmentsOnIp.getSegment()));
            }
        }
        return segmentsOnIpMap;
    }

    @Override
    public boolean syncPutPersistentConfig(String cluster, Map<String, Object> configMap) {
        return esClusterDAO.putPersistentConfig(cluster, configMap);
    }

    @Override
    public String synGetESVersionByCluster(String cluster) {
        return esClusterDAO.getESVersionByCluster(cluster);
    }

    @Override
    public Map<String, ClusterNodeInfo> syncGetAllSettingsByCluster(String cluster) {
        return esClusterDAO.getAllSettingsByCluster(cluster);
    }

    @Override
    public Map<String, ClusterNodeSettings> syncGetPartOfSettingsByCluster(String cluster) {
        return esClusterDAO.getPartOfSettingsByCluster(cluster);
    }

    @Override
    public Set<String> syncGetAllNodesAttributes(String cluster) {
        Set<String> nodeAttributes = Sets.newHashSet();
        List<NodeAttrInfo> nodeAttrInfos = esClusterDAO.syncGetAllNodesAttributes(cluster);
        if (CollectionUtils.isEmpty(nodeAttrInfos)) {
            return nodeAttributes;
        }

        //对于所有的节点属性进行去重的操作
        nodeAttrInfos.forEach(nodeAttrInfo -> nodeAttributes.add(nodeAttrInfo.getAttribute()));

        return nodeAttributes;
    }

    /**
     * rack对应的可使用磁盘大小 key->rack value->diskSize
     */
    @Override
    public Map<String, Float> getAllocationInfoOfRack(String cluster) {
        //获取节点关于可以使用的磁盘资源的信息
        Map<String, String> canUseDiskOnNodeMap = ConvertUtil.list2Map(esClusterDAO.getNodeAllocationInfoByCluster(cluster),
                NodeAllocationInfo::getNode, NodeAllocationInfo::getTotalDiskSize);
        if (MapUtils.isEmpty(canUseDiskOnNodeMap)) {
            LOGGER.warn("class=ESClusterServiceImpl||method=getAllocationInfoOfRack||msg=cant get node allocation");
            return null;
        }

        //获取指定集群下的节点attr标签值
        Map<String, Float> allocationInfoOfRackMap = Maps.newHashMap();
        List<NodeAttrInfo> nodeAttrInfos = esClusterDAO.syncGetAllNodesAttributes(cluster);
        if (CollectionUtils.isEmpty(nodeAttrInfos)) {
            LOGGER.warn("class=ESClusterServiceImpl||method=getAllocationInfoOfRack||msg=cant get node attributes");
            return null;
        }

        //对设置有rack标签的节点进行过滤，获取对应rack下的可使用磁盘空间
        nodeAttrInfos.stream().filter(nodeAttrInfo -> "rack".equals(nodeAttrInfo.getAttribute())).forEach(
                nodeAttrInfo -> {
                    //获取rack对应的设置数值
                    String attrRack = nodeAttrInfo.getValue();
                    //根据rack指定节点获取节点的可使用磁盘空间,es集群获取的raw数据都是以gb结尾，例如123.5gb
                    String canUseDiskDateOfNode = canUseDiskOnNodeMap.get(nodeAttrInfo.getNode());
                    //对应可使用磁盘信息为空则跳出本次循环
                    if (StringUtils.isBlank(canUseDiskDateOfNode)) {
                        return;
                    }

                    //对应指定rack的可使用磁盘空间大小进行操作
                    Float newDiskSize = Float.valueOf(SizeUtil.getUnitSize(canUseDiskDateOfNode));
                    if (allocationInfoOfRackMap.containsKey(attrRack)) {
                        allocationInfoOfRackMap.put(attrRack, newDiskSize + allocationInfoOfRackMap.get(attrRack));
                    } else {
                        allocationInfoOfRackMap.put(attrRack, newDiskSize);
                    }

                    //移除掉节点allocation_map中的节点名称
                    canUseDiskOnNodeMap.remove(nodeAttrInfo.getNode());
                }
        );

        if(MapUtils.isEmpty(canUseDiskOnNodeMap)) {
            return allocationInfoOfRackMap;
        }

        // 剩余的没有设置rack的节点全部作为rack为*来进行保存
        final float[] diskNumber = {0f};
        canUseDiskOnNodeMap.values().forEach(s -> diskNumber[0] = SizeUtil.getUnitSize(s) + diskNumber[0]);
        allocationInfoOfRackMap.put("*", diskNumber[0]);

        return allocationInfoOfRackMap;
    }

    @Override
    public Result<Set<String>> getClusterRackByHttpAddress(String addresses, String password) {
        Set<String> racks = new HashSet<>();
        ESClient client = new ESClient();
        client.addTransportAddresses(addresses);

        if (StringUtils.isNotBlank(password)) {
            client.setPassword(password);
        }

        try {
            client.start();
            ESClusterNodesSettingResponse response = client.admin().cluster().prepareNodesSetting().execute()
                    .actionGet(ES_OPERATE_TIMEOUT, TimeUnit.SECONDS);

            if (RestStatus.OK.getStatus() == response.getRestStatus().getStatus()) {
                for (Map.Entry<String, ClusterNodeSettings> entry : response.getNodes().entrySet()) {
                    // 获取节点的roles角色信息
                    List<String> roles = JSONArray.parseArray(JSON.toJSONString(entry.getValue().getRoles()), String.class);
                    // 低版本(2.3.3)的集群在nodes中不存在roles属性，关于角色的信息需要从node的settings中获取
                    if (CollectionUtils.isEmpty(roles)) {
                        // 获取setting.node中的角色信息
                        JSONObject nodeRole = entry.getValue().getSettings().getJSONObject("node");
                        if (AriusObjUtils.isNull(nodeRole) || !nodeRole.containsKey(ES_ROLE_DATA) || nodeRole.getBoolean(ES_ROLE_DATA)) {
                            setRacksForDateRole(racks, entry);
                        }
                        continue;
                    }

                    // 高版本可以通过node含有的roles属性判断节点角色
                    if (roles.contains(ES_ROLE_DATA)) {
                        setRacksForDateRole(racks, entry);
                    }
                }

                return Result.buildSucc(racks);
            } else {
                return Result.buildParamIllegal(String.format("通过地址:%s获取rack失败", addresses));
            }
        } catch (Exception e) {
            LOGGER.error("class=ESClusterServiceImpl||method=getClusterRackByHttpAddress||msg=get rack error||httpAddress={}||msg=client start error", addresses);
            return Result.buildParamIllegal(String.format("通过地址:%s获取rack失败", addresses));
        } finally {
            client.close();
        }
    }

    @Override
    public String synGetESVersionByHttpAddress(String addresses, String password) {
        ESClient client = new ESClient();
        client.addTransportAddresses(addresses);

        if (StringUtils.isNotBlank(password)) {
            client.setPassword(password);
        }
        String esVersion = null;
        try {
            client.start();
            DirectRequest directRequest = new DirectRequest("GET", "");
            DirectResponse directResponse = client.direct(directRequest).actionGet(30, TimeUnit.SECONDS);
            if (directResponse.getRestStatus() == RestStatus.OK
                    && StringUtils.isNoneBlank(directResponse.getResponseContent())) {
                JSONObject version = JSONObject.parseObject(directResponse.getResponseContent()).getJSONObject(VERSION);
                esVersion = version.getString(VERSION_NUMBER);
                if (version.containsKey(VERSION_INNER_NUMBER)) {
                    String innerVersion = version.getString(VERSION_INNER_NUMBER).split("\\.")[0].trim();
                    esVersion = Strings.isNullOrEmpty(innerVersion) ? esVersion : esVersion + "." + innerVersion;
                }
            }
            return esVersion;
        } catch (Exception e) {
            LOGGER.warn("class=ESClusterServiceImpl||method=synGetESVersionByHttpAddress||address={}||mg=get es segments fail", addresses, e);
            return null;
        } finally {
            client.close();
        }
    }

    /***************************************** private method ****************************************************/
    /**
     * 根据node的atrributes获取当前节点的rack配置信息
     * @param racks rack列表
     * @param entry 当前node的entry
     */
    private void setRacksForDateRole(Set<String> racks, Map.Entry<String, ClusterNodeSettings> entry) {
        if (AriusObjUtils.isNull(entry.getValue().getAttributes())
                || AriusObjUtils.isNull(entry.getValue().getAttributes().getRack())) {
            racks.add("*");
        } else {
            racks.add(entry.getValue().getAttributes().getRack());
        }
    }
}
