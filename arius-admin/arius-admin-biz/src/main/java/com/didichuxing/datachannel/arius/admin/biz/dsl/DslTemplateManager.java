package com.didichuxing.datachannel.arius.admin.biz.dsl;

import com.didichuxing.datachannel.arius.admin.client.bean.common.PaginationResult;
import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.dsl.template.DslTemplateConditionDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.template.DslTemplateVO;

import java.util.List;

/**
 * @author cjm
 */
public interface DslTemplateManager {

    /**
     * 根据appid修改查询模版限流值
     * @param appId 应用账号
     * @param dslTemplateMd5List dsl模板MD5，待修改限流值
     * @param queryLimit 限流值
     * @return Result<Boolean>
     */
    Result<Boolean> updateDslTemplateQueryLimit(Integer appId, List<String> dslTemplateMd5List, Double queryLimit);

    /**
     * 更新查询模版的 启用|停用 状态
     * @param appId 应用账号
     * @param dslTemplateMd5 dsl模板MD5
     * @return Result<Boolean>
     */
    Result<Boolean> changeDslTemplateStatus(Integer appId, String dslTemplateMd5);

    /**
     * 根据dslTemplateMd5查找DSL模版详情
     * @param appId 应用账号
     * @param dslTemplateMd5 dsl模板MD5
     * @return Result<DslTemplateVO>
     */
    Result<DslTemplateVO> getDslTemplateDetail(Integer appId, String dslTemplateMd5);

    /**
     * 分页获取DSL模版列表信息
     * @param appId 应用id
     * @param queryDTO 查询条件
     * @return 分页数据
     */
    PaginationResult<DslTemplateVO> getDslTemplatePage(Integer appId, DslTemplateConditionDTO queryDTO);
}
