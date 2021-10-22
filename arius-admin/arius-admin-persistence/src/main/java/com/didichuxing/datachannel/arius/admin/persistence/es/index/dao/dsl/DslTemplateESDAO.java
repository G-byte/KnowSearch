package com.didichuxing.datachannel.arius.admin.persistence.es.index.dao.dsl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.dsl.*;
import com.didichuxing.datachannel.arius.admin.common.bean.po.dsl.DslTemplatePO;
import com.didichuxing.datachannel.arius.admin.common.Tuple;
import com.didichuxing.datachannel.arius.admin.common.util.DateTimeUtil;
import com.didichuxing.datachannel.arius.admin.persistence.component.ScrollResultVisitor;
import com.didichuxing.datachannel.arius.admin.persistence.es.BaseESDAO;
import com.didichuxing.datachannel.arius.admin.persistence.es.index.dsls.DslsConstant;
import com.didichuxing.datachannel.arius.elasticsearch.client.response.query.query.ESQueryResponse;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DslTemplateESDAO extends BaseESDAO {

    /**
     * 查询模板数据的索引名称
     */
    private String indexName;
    /**
     * 查询模板过期删除时间
     */
    @Value("${delay.delete.expired.template.time}")
    private String deleteExpireDslTime;
    /**
     * 查询历史数据时间
     */
    @Value("${history.query.time}")
    private String historyQueryTime;
    /**
     * type名称
     */
    private String typeName = "type";
    /**
     * 滚动查询大小
     */
    private static final int SCROLL_SIZE = 1000;

    @PostConstruct
    public void init(){
        this.indexName = dataCentreUtil.getAriusDslTemplate();
    }

    /**
     * 更新查询模板信息
     *
     * @param list
     * @return
     */
    public boolean updateTemplates(List<DslTemplatePO> list) {

        return updateClient.batchUpdate(indexName, typeName, list);
    }

    /**
     * 获取所有查询模板最近修改时间在(now-1d,now)范围内，并且不启用的查询模板，然后删除过期的查询模板数据
     *
     * @return
     */
    public boolean deleteExpiredDslTemplate() {

        String dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_EXPIRED_DSL_TEMPLATE, SCROLL_SIZE, deleteExpireDslTime);

        List<DslTemplatePO> list = getTemplatesByDsl(dsl);
        List<String> ids = Lists.newLinkedList();
        for (DslTemplatePO DslTemplatePO : list) {
            ids.add(DslTemplatePO.getKey());
        }

        return updateClient.batchDelete(indexName, typeName, ids);
    }

    /**
     * 更新黑白名单
     *
     * @param checkModeList
     * @return
     */
    public boolean updateCheckModeByByAppidDslTemplate(List<DslCheckMode> checkModeList) {
        String ariusModifyTime = DateTimeUtil.getCurrentFormatDateTime();
        List<DslTemplatePO> list = Lists.newLinkedList();
        DslTemplatePO item = null;

        for (DslCheckMode dslCheckMode : checkModeList) {
            item = new DslTemplatePO();
            item.setCheckMode(dslCheckMode.getCheckMode());
            item.setAriusModifyTime(ariusModifyTime);
            item.setAppid(dslCheckMode.getAppid());
            item.setDslTemplateMd5(dslCheckMode.getDslTemplateMd5());

            list.add(item);
        }

        return updateTemplates(list);
    }

    /**
     * 更新查询限流值
     *
     * @param dslQueryLimitList
     * @return
     */
    public boolean updateQueryLimitByAppidDslTemplate(List<DslQueryLimit> dslQueryLimitList) {
        String ariusModifyTime = DateTimeUtil.getCurrentFormatDateTime();
        List<DslTemplatePO> list = Lists.newLinkedList();
        DslTemplatePO item = null;

        for (DslQueryLimit dslQueryLimit : dslQueryLimitList) {
            item = new DslTemplatePO();
            item.setQueryLimit(dslQueryLimit.getQueryLimit());
            item.setForceSetQueryLimit(true);
            item.setAriusModifyTime(ariusModifyTime);
            item.setAppid(dslQueryLimit.getAppid());
            item.setDslTemplateMd5(dslQueryLimit.getDslTemplateMd5());

            list.add(item);
        }

        return updateTemplates(list);
    }

    /**
     * 获取查询模板信息
     *
     * @param dslBases
     * @return
     */
    public Map<String, DslTemplatePO> getDslTemplateByKeys(List<? extends DslBase> dslBases) {
        Map<String, DslTemplatePO> result = Maps.newHashMap();
        for (DslBase dslBase : dslBases) {
            result.put(dslBase.getAppidDslTemplateMd5(), getDslTemplateByKey(dslBase.getAppid(), dslBase.getDslTemplateMd5()));
        }
        return result;
    }

    /**
     * 根据主键id获取查询模板
     *
     * @param appid
     * @param dslTemplateMd5
     * @return
     */
    public DslTemplatePO getDslTemplateByKey(Integer appid, String dslTemplateMd5) {
        return getDslTemplateByKey(String.format("%d_%s", appid, dslTemplateMd5));
    }

    /**
     * 根据主键id获取查询模板
     *
     * @param key
     * @return
     */
    public DslTemplatePO getDslTemplateByKey(String key) {
        return gatewayClient.doGet(indexName, typeName, key, DslTemplatePO.class);
    }

    /**
     * 获取某个appid的所有查询模板数据,已排除老查询模板
     *
     * @param appid
     * @return
     */
    public List<DslTemplatePO> getAllDslTemplatePOByAppid(Integer appid) {

        String dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_DSL_TEMPLATE_BY_APPID, SCROLL_SIZE, appid);

        return getTemplatesByDsl(dsl);
    }


    /**
     * 获取查询模板创建时间大于指定天偏移的查询模板数据，用于设置慢查耗时阈值
     *
     * @param dayOffset
     * @return
     */
    public List<DslTemplatePO> getDslTemplatesByDateRange(int dayOffset) {

        String dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_DSL_TEMPLATES_BY_RANGE, SCROLL_SIZE, dayOffset);

        return getTemplatesByDsl(dsl);
    }

    /**
     * 获取早期查询模板，不包括手动修改限流值
     *
     * @return
     */
    public List<DslTemplatePO> getEarliestDslTemplate() {
        String dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_EARLIEST_DSL_TEMPLATES, SCROLL_SIZE);

        return getTemplatesByDsl(dsl);
    }

    /**
     * 获取一段时间不使用的查询模板，不包括黑名单和手动修改限流值
     *
     * @return
     */
    public List<DslTemplatePO> getLongTimeNotUseDslTemplate() {
        String dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_LONG_TIME_NOT_USE_DSL_TEMPLATES, SCROLL_SIZE, historyQueryTime);

        return getTemplatesByDsl(dsl);
    }

    public List<DslTemplatePO> getDslMertricsByAppid(Long appid, Long startDate, Long endDate){
        String dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_DSL_TEMPLATE_BY_APPID_AND_RANGE, 10000, startDate, endDate, appid);

        return gatewayClient.performRequest(indexName, typeName, dsl, DslTemplatePO.class);
    }

    /**
     * 根据查询条件获取查询模板数据
     *
     * @param appId
     * @param startDate
     * @param endDate
     * @return
     */
    public Tuple<Long, List<DslTemplatePO>> getDslTemplateByCondition(Long appId, String searchKeyword, String dslTag, String sortInfo, Long from, Long size, Long startDate, Long endDate) {
        try {
            String dsl = null;
            JSONArray mustJson = new JSONArray();
            mustJson.add( JSONObject.parse("{\"term\":{\"version\":{\"value\":\"V2\"}}}"));
            mustJson.add(JSONObject.parse(String.format("{\"range\":{\"flinkTime\":{\"gte\":%d,\"lte\":%d}}}", startDate, endDate)));

            if (appId != null) {
                mustJson.add(JSONObject.parse(String.format("{\"term\":{\"appid\":{\"value\":%d}}}", appId)));
            }
            if (StringUtils.isNoneBlank(searchKeyword)) {
                mustJson.add(JSONObject.parse(String.format("{\"wildcard\":{\"my_all_fields\":\"%s\"}}", searchKeyword)));
            }
            if (StringUtils.isNoneBlank(dslTag)) {
                String[] items = StringUtils.splitByWholeSeparatorPreserveAllTokens(dslTag, ",");
                JSONArray dslTagJson = new JSONArray();
                Arrays.asList(items).forEach( item -> dslTagJson.add(item));
                String terms = String.format("{\"terms\":{\"dslTag\":%s}}", dslTagJson.toJSONString());
                mustJson.add(JSONObject.parse(terms));
            }
            if (StringUtils.isBlank(sortInfo)) {
                sortInfo = "";
            }

            dsl = String.format("{\"from\":%d,\"size\":%d,\"query\":{\"bool\":{\"must\":[%s]}},\"sort\":[%s]}", from, size, mustJson.toJSONString(), sortInfo);

            return gatewayClient.performRequestListAndGetTotalCount(indexName, typeName, dsl, DslTemplatePO.class);

        } catch (Exception e) {
            LOGGER.error("class=DslTemplateEsDao||method=getDslTemplateByCondition||errMsg=search template error", e);
            return null;
        }
    }

    /**
     * 获取过期的查询模板信息
     *
     * @return
     */
    public List<DslTemplatePO> getExpiredAndWillDeleteDslTemplate() {
        String dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_EXPIRED_DELETED_DSL_TEMPLATE, SCROLL_SIZE, deleteExpireDslTime);

        return getTemplatesByDsl(dsl);
    }

    /**
     * 获取最近查询模板，不包括手动修改限流值和黑名单
     *
     * @return
     */
    public List<DslTemplatePO> getNearestDslTemplate() {
        String dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_NEAREST_DSL_TEMPLATES, SCROLL_SIZE);

        return getTemplatesByDsl(dsl);
    }

    /**
     * 获取最近没有设置黑白名单的查询模板,已排除老查询模板
     *
     * @return
     */
    public List<DslTemplatePO> getNearestDslTemplateAccessable() {
        String dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_NEAREST_DSL_TEMPLATE_ACCESSABLE, SCROLL_SIZE);

        return getTemplatesByDsl(dsl);
    }

    /**
     * 获取到缺少ariusCreateTime字段的文档
     *
     * @return
     */
    public List<DslTemplatePO> getMissingAriusCreateTme() {
        String dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_MISSING_ARIUS_CREATE_TIME, SCROLL_SIZE);

        return getTemplatesByDsl(dsl);
    }

    /**
     * 获取某个appid的查询模板个数,已排除老版本查询模板
     *
     * @param appId
     * @return
     */
    public Long getTemplateCountByAppId(Integer appId) {
        String dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_DSL_TEMPLATE_COUNT_BY_APPID, appId);

        return gatewayClient.performRequestAndGetTotalCount(indexName, typeName, dsl);
    }

    /**
     * 获取某个appid的新增查询模板个数,已排除老查询模板
     *
     * @param appId
     * @param date
     * @param today
     * @return
     */
    public Long getIncreaseTemplateCountByAppId(Integer appId, String date, String today) {
        String dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_INCREASE_DSL_TEMPLATE_BY_APPID, appId, date, today);

        return gatewayClient.performRequestAndGetTotalCount(indexName, typeName, dsl);
    }

    /**
     * 根据index获得对应的templateMD5
     *
     * @param indexName
     * @return
     */
    public Map<String/*dslMd5*/, Set<String>/*dsls*/> getTemplateMD5ByIndexName(String indexName, Integer dayOffset) {

        Map<String/*dslMd5*/, Set<String>/*dsls*/> dslMap = Maps.newHashMap();
        String dsl = null;
        if (dayOffset == -1) {
            dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_DSL_TEMPLATES_BY_INDEXNAME, SCROLL_SIZE, indexName);
        } else {
            dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_DSL_TEMPLATES_BY_INDEXNAME_WITH_DAY_RANGE, SCROLL_SIZE, indexName, dayOffset);
        }

        gatewayClient.queryWithScroll(this.indexName, typeName, dsl, SCROLL_SIZE, null, DslTemplatePO.class, resultList -> {
            if (resultList != null) {
                for (DslTemplatePO DslTemplatePO : resultList) {
                    dslMap.computeIfAbsent(DslTemplatePO.getDslTemplateMd5(), key -> Sets.newLinkedHashSet()).add(DslTemplatePO.getDsl());
                }
            }
        } );

        return dslMap;
    }

    /**
     * 根据index获得对应的templateMD5
     *
     * @param indexName
     * @return
     */
    public Map<String/*dskMd5*/, Set<String>/*dsls*/> getTemplateMD5ByIndexNameAndAppId(String indexName, String appId, Integer dayOffset) {

        Map<String/*dskMd5*/, Set<String>/*dsls*/> dslMap = Maps.newHashMap();

        String dsl = null;
        if (dayOffset == -1) {
            dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_DSL_TEMPLATES_BY_INDEXNAME_APPID, SCROLL_SIZE, indexName, appId);
        } else {
            dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.GET_DSL_TEMPLATES_BY_INDEXNAME_APPID_WITH_DAY_RANGE, SCROLL_SIZE, indexName, appId, dayOffset);
        }

        gatewayClient.queryWithScroll(this.indexName, typeName, dsl, SCROLL_SIZE, null, DslTemplatePO.class, resultList -> {
            if (resultList != null) {
                for (DslTemplatePO DslTemplatePO : resultList) {
                    dslMap.computeIfAbsent(DslTemplatePO.getDslTemplateMd5(), key -> Sets.newLinkedHashSet()).add(DslTemplatePO.getDsl());
                }
            }
        } );

        return dslMap;
    }

    /**
     * 滚动获取查询模板
     *
     * @param request
     * @return
     */
    @Nullable
    public ScrollDslTemplateResponse handleScrollDslTemplates(ScrollDslTemplateRequest request) {

        String dsl = dslLoaderUtil.getFormatDslByFileName(DslsConstant.SCROLL_DSL_TEMPLATES,
                request.getScrollSize(), request.getLastModifyTime(), request.getDslTemplateVersion());

        List<DslTemplatePO> list = Lists.newLinkedList();
        ESQueryResponse response = null;
        // 没有游标id，则开始滚动查询
        if (StringUtils.isBlank(request.getScrollId())) {

            response = gatewayClient.prepareScrollQuery(indexName, typeName, dsl, null, DslTemplatePO.class, new ScrollResultVisitor<DslTemplatePO>() {
                @Override
                public void handleScrollResult(List<DslTemplatePO> resultList) {
                    if (resultList != null) {
                        list.addAll(resultList);
                    }
                }
            });

        } else {
            response = gatewayClient.queryScrollQuery(indexName, request.getScrollId(), DslTemplatePO.class, new ScrollResultVisitor<DslTemplatePO>() {
                @Override
                public void handleScrollResult(List<DslTemplatePO> resultList) {
                    if (resultList != null) {
                        list.addAll(resultList);
                    }
                }
            });
        }

        if (response == null) {
            return null;
        }

        String scrollId = response.getUnusedMap().get("_scroll_id").toString();

        return ScrollDslTemplateResponse
                .builder()
                .dslTemplatePoList(list)
                .scrollId(scrollId)
                .build();
    }


    /**
     * 根据查询语句获取查询模板数据
     *
     * @param dsl
     * @return
     */
    private List<DslTemplatePO> getTemplatesByDsl(String dsl) {
        List<DslTemplatePO> list = Lists.newLinkedList();

        gatewayClient.queryWithScroll(indexName, typeName, dsl, SCROLL_SIZE, null, DslTemplatePO.class, resultList -> {
            if (resultList != null) {
                list.addAll(resultList);
            }
        } );

        return list;
    }
}
