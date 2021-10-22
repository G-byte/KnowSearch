package com.didichuxing.datachannel.arius.admin.biz.template.srv.mapping;

import java.util.List;
import java.util.Set;

import com.didichuxing.datachannel.arius.admin.client.bean.common.MappingOptimize;
import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.template.ConsoleTemplateSchemaDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.template.ConsoleTemplateSchemaOptimizeDTO;
import com.didichuxing.datachannel.arius.admin.client.mapping.AriusTypeProperty;
import com.didichuxing.datachannel.arius.admin.client.mapping.Field;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.template.IndexTemplateLogicWithMapping;
import com.didichuxing.datachannel.arius.admin.common.exception.AdminOperateException;

/**
 * 逻辑模板的mapping服务；对我暴露的是field对象，并屏蔽底层的mapping信息、部署信息
 * @author zhognhua
 */
public interface TemplateLogicMappingManager {

    /**
     * 查询指定的逻辑模板 带有Mapping信息
     * @param logicId 模板id
     * @throws Exception
     * @return 模板信息  不存在返回null
     */
    Result<IndexTemplateLogicWithMapping> getTemplateWithMapping(Integer logicId) throws Exception;

    /**
     * 更新
     * @param logicId 模板id
     * @param fields fields
     * @return result
     */
    Result updateFields(Integer logicId, List<Field> fields, Set<String> removeFields);

    /**
     * 校验模板field
     *
     * @param logicId 模板id
     * @param fields 属性列表
     * @return Result
     */
    Result checkFields(Integer logicId, List<Field> fields);

    /**
     * 更新
     * @param logicId 模板id
     * @param ariusTypeProperty mapping
     * @return result
     */
    Result updateMappingForNew(Integer logicId, AriusTypeProperty ariusTypeProperty);

    /**
     * updateProperties
     * @param logicId
     * @param properties
     * @return
     */
    Result updateProperties(Integer logicId, List<AriusTypeProperty> properties);

    /**
     * field装AriusTypeProperty
     * @param fields fields
     * @return str
     */
    AriusTypeProperty fields2Mapping(List<Field> fields);

    /**
     * 获取mapping优化信息
     * @param logicId logicId
     * @return result
     */
    Result<List<MappingOptimize>> getTemplateMappingOptimize(Integer logicId);

    /**
     * mapping优化
     * @param optimizeDTO dto
     * @param operator 操作人
     * @return result
     */
    Result modifySchemaOptimize(ConsoleTemplateSchemaOptimizeDTO optimizeDTO, String operator);

    /**
     * 修改模板schema
     * @param schemaDTO schema
     * @param operator 操作人
     * @return result
     */
    Result modifySchema(ConsoleTemplateSchemaDTO schemaDTO, String operator) throws AdminOperateException;
}
