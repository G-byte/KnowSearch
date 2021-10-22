package com.didichuxing.datachannel.arius.admin.client.bean.vo.template;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: D10865
 * @description:
 * @date: Create on 2019/1/15 下午6:39
 * @modified By D10865
 *
 * dsl模板信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "DslTemplateVO", description = "dsl模板信息")
public class DslTemplateVO {
    /**
     * 查询模板创建时间
     */
    @ApiModelProperty("查询模板创建时间")
    private String ariusCreateTime;
    /**
     * 查询模板修改时间
     */
    @ApiModelProperty("查询模板修改时间")
    private String ariusModifyTime;
    /**
     * 平均响应长度
     */
    @ApiModelProperty("平均响应长度")
    private Double responseLenAvg;
    /**
     * 请求类型
     */
    @ApiModelProperty("请求类型")
    private String requestType;
    /**
     * 查询类型
     */
    @ApiModelProperty("查询类型")
    private String searchType;
    /**
     * 查询次数(分钟级别)
     */
    @ApiModelProperty("查询次数(分钟级别)")
    private Long searchCount;
    /**
     * es查询耗时
     */
    @ApiModelProperty("es查询耗时")
    private Double esCostAvg;
    /**
     * 平均查询语句长度
     */
    @ApiModelProperty("平均查询语句长度")
    private Double dslLenAvg;
    /**
     * 平均命中记录数
     */
    @ApiModelProperty("平均命中记录数")
    private Double totalHitsAvg;
    /**
     * 平均查询shard成功个数
     */
    @ApiModelProperty("平均查询shard成功个数")
    private Double successfulShardsAvg;
    /**
     * 平均shard成功个数
     */
    @ApiModelProperty("平均shard成功个数")
    private Double totalShardsAvg;
    /**
     * 查询请求时刻
     */
    @ApiModelProperty("查询请求时刻")
    private String logTime;
    /**
     * 查询索引示例
     */
    @ApiModelProperty("查询索引示例")
    private String indiceSample;
    /**
     * 查询模板
     */
    @ApiModelProperty("查询模板")
    private String dslTemplate;
    /**
     * 查询请求时刻
     */
    @ApiModelProperty("查询请求时刻")
    private Long timeStamp;
    /**
     * 查询语句类型
     */
    @ApiModelProperty("查询语句类型")
    private String dslType;
    /**
     * 查询索引名称
     */
    @ApiModelProperty("查询索引名称")
    private String indices;
    /**
     * 查询模板MD5
     */
    @ApiModelProperty("查询模板MD5")
    private String dslTemplateMd5;
    /**
     * 平均查询总耗时
     */
    @ApiModelProperty("平均查询总耗时")
    private Double totalCostAvg;
    /**
     * 查询shard失败个数
     */
    @ApiModelProperty("查询shard失败个数")
    private Double failedShardsAvg;
    /**
     * dsink写入时间
     */
    @ApiModelProperty("dsink写入时间")
    private Long sinkTime;
    /**
     * appid
     */
    @ApiModelProperty("appid")
    private Integer appid;
    /**
     * 查询语句
     */
    @ApiModelProperty("查询语句")
    private String dsl;
    /**
     * 平均gateway处理耗时
     */
    @ApiModelProperty("平均gateway处理耗时")
    private Double beforeCostAvg;
    /**
     * flin
     */
    @ApiModelProperty("flin")
    private String flinkTime;
    /**
     * 查询限流
     */
    @ApiModelProperty("查询限流")
    private Double queryLimit;
    /**
     * 是否来自用户控制台
     */
    @ApiModelProperty("是否来自用户控制台")
    private Boolean isFromUserConsole;
    /**
     * 是否强制设置查询限流值
     */
    @ApiModelProperty("是否强制设置查询限流值")
    private Boolean forceSetQueryLimit;
    /**
     * 是否可用 null/true表示可用，false表示不可用
     */
    @ApiModelProperty("是否可用 null/true表示可用，false表示不可用")
    private Boolean enable;
    /**
     * 黑白名单 null/white表示白名单，black表示黑名单
     */
    @ApiModelProperty("黑白名单 null/white表示白名单，black表示黑名单")
    private String checkMode;
    /**
     * 慢查dsl阈值，单位为ms
     */
    @ApiModelProperty("慢查dsl阈值，单位为ms")
    private Long slowDslThreshold;
    /**
     * 查询模板版本号
     */
    @ApiModelProperty("查询模板版本号")
    private String version;
    /**
     * 查询模板危害标签
     */
    @ApiModelProperty("查询模板危害标签")
    private String dslTag;
}
