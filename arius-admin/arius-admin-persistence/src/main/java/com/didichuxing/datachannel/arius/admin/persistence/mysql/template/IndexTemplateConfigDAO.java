package com.didichuxing.datachannel.arius.admin.persistence.mysql.template;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.didichuxing.datachannel.arius.admin.common.bean.po.template.TemplateConfigPO;

/**
 * @author d06679
 * @date 2019/3/29
 */
@Repository
public interface IndexTemplateConfigDAO {

    List<TemplateConfigPO> listByCondition(TemplateConfigPO param);

    List<TemplateConfigPO> listAll();

    int insert(TemplateConfigPO param);

    int update(TemplateConfigPO param);

    int delete(Long configId);

    TemplateConfigPO getById(Long configId);

    TemplateConfigPO getByLogicId(Integer logicId);

}
