package com.didichuxing.datachannel.arius.admin.core.service.es;

import java.util.List;
import java.util.Map;

import com.didichuxing.datachannel.arius.admin.common.exception.ESOperateException;
import com.didiglobal.logi.elasticsearch.client.response.setting.common.MappingConfig;
import com.didiglobal.logi.elasticsearch.client.response.setting.template.MultiTemplatesConfig;
import com.didiglobal.logi.elasticsearch.client.response.setting.template.TemplateConfig;

/**
 * @author d06679
 * @date 2019/4/2
 */
public interface ESTemplateService {

    /**
     * 删除模板
     * @param cluster 集群名字
     * @param name 模板名字
     * @param retryCount 重试次数
     * @return result
     * @throws ESOperateException
     */
    boolean syncDelete(String cluster, String name, int retryCount) throws ESOperateException;

    /**
     * 修改模板rack和shard
     * @param cluster 集群
     * @param name 模板明细
     * @param rack rack
     * @param shard shard
     * @param retryCount 重试次数
     * @return result
     * @throws ESOperateException
     */
    boolean syncUpdateRackAndShard(String cluster, String name, String rack, Integer shard, Integer shardRouting,
                                   int retryCount) throws ESOperateException;

    /**
     * 创建模板, 会覆盖之前的存在的
     * @param cluster 集群
     * @param name 模板名字
     * @param expression 表达式
     * @param rack rack
     * @param shard shard
     * @param retryCount 重试次数
     * @return result
     * @throws ESOperateException
     */
    boolean syncCreate(String cluster, String name, String expression, String rack, Integer shard, Integer shardRouting,
                       int retryCount) throws ESOperateException;

    /**
     * 创建模板, 会覆盖之前的存在的
     * @param settings 模板settings
     * @param cluster 集群
     * @param name 模板名字
     * @param expression 表达式
     * @param mappings 模板mappings
     * @param retryCount 重试次数
     * @return
     * @throws ESOperateException
     */
    boolean syncCreate(Map<String, String> settings, String cluster, String name, String expression, MappingConfig mappings, int retryCount) throws ESOperateException;

    /**
     * 修改模板
     * @param cluster 集群
     * @param name 模板名字
     * @param expression 表达式
     * @param retryCount 重试次数
     * @return result
     * @throws ESOperateException
     */
    boolean syncUpdateExpression(String cluster, String name, String expression,
                                 int retryCount) throws ESOperateException;

    /**
     * 修改模板分片数量
     * @param cluster 集群
     * @param name 模板姓名
     * @param shardNum 分片数目
     * @param retryCount 重试次数
     * @return
     * @throws ESOperateException
     */
    boolean syncUpdateShardNum(String cluster, String name, Integer shardNum,
                               int retryCount) throws ESOperateException;

    /**
     * 修改模板setting
     * @param cluster 集群
     * @param name 模板明细
     * @param setting 配置
     * @param retryCount 重试次数
     * @return result
     * @throws ESOperateException
     */
    boolean syncUpsertSetting(String cluster, String name, Map<String, String> setting,
                              int retryCount) throws ESOperateException;

    /**
     * 跨集群拷贝模板mapping和索引
     * @param srcCluster 源集群
     * @param srcTemplateName 原模板
     * @param tgtCluster 目标集群
     * @param tgtTemplateName 目标模板
     * @param retryCount 重试次数
     * @return result
     */
    boolean syncCopyMappingAndAlias(String srcCluster, String srcTemplateName, String tgtCluster,
                                    String tgtTemplateName, int retryCount) throws ESOperateException;

    /**
     * 同步更新物理模板配置
     * @param cluster 集群名称
     * @param templateName 物理模板名称
     * @param templateConfig 模板配置
     * @param retryCount 重试次数
     * @return
     * @throws ESOperateException
     */
    boolean syncUpdateTemplateConfig(String cluster, String templateName, TemplateConfig templateConfig,
                                     int retryCount) throws ESOperateException;

    /**
     * 获取模板信息
     * @param cluster 集群
     * @param name    名字
     * @return Config
     */
    TemplateConfig syncGetTemplateConfig(String cluster, String name);

    /**
     * 获取模板的mapping配置
     *
     * @param clusterName
     * @param templateName
     * @return
     */
    MappingConfig syncGetMappingsByClusterName(String clusterName, String templateName);

    /**
     * 获取模板配置
     * @param clusterName 集群名称
     * @param templateName 模板名称
     * @return
     */
    MultiTemplatesConfig syncGetTemplates(String clusterName, String templateName);

    /**
     * 获取所有引擎模板
     * @param clusters 集群名
     * @return
     */
    Map<String, TemplateConfig> syncGetAllTemplates(List<String> clusters);

    /**
     * 修改模板名称
     * @param cluster 集群
     * @param srcName 源名称
     * @param tgtName 现名称
     * @return
     */
    boolean syncUpdateName(String cluster, String srcName, String tgtName, int retryCount) throws ESOperateException;

    /**
     * 验证模板配置是否正常
     * @param cluster 物理集群名称
     * @param name 模板名称
     * @param templateConfig 模板配置
     * @return
     * @throws ESOperateException
     */
    boolean syncCheckTemplateConfig(String cluster, String name, TemplateConfig templateConfig,
                                    int retryCount) throws ESOperateException;

    /**
     * 获取集群模板个数, 不包涵原生自带模板
     */
    long syncGetTemplateNum(String cluster);

    /**
     * 获取集群模板个数，兼容2.3.3低版本的集群
     * @param cluster 物理集群名称
     * @return 集群模板个数
     */
    long synGetTemplateNumForAllVersion(String cluster);
}
