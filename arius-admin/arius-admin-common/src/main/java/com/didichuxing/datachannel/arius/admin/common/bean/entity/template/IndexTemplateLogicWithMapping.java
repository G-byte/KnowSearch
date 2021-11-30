package com.didichuxing.datachannel.arius.admin.common.bean.entity.template;

import java.util.List;

import com.didichuxing.datachannel.arius.admin.client.mapping.Field;
import com.didichuxing.datachannel.arius.admin.client.mapping.AriusTypeProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author d06679
 * @date 2019/3/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexTemplateLogicWithMapping extends IndexTemplateLogic {

    private List<Field>        fields;

    private List<AriusTypeProperty> typeProperties;

}
