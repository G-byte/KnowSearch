package com.didichuxing.datachannel.arius.admin.common.event.template;

import com.didichuxing.datachannel.arius.admin.common.bean.entity.template.IndexTemplateLogicWithPhyTemplates;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.template.IndexTemplatePhy;

/**
 * @author d06679
 * @date 2019/4/18
 */
public class PhysicalTemplateDeleteEvent extends PhysicalTemplateEvent {

    private IndexTemplatePhy delTemplate;

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public PhysicalTemplateDeleteEvent(Object source, IndexTemplatePhy delTemplate,
                                       IndexTemplateLogicWithPhyTemplates logicWithPhysical) {
        super(source, logicWithPhysical);
        this.delTemplate = delTemplate;
    }

    public IndexTemplatePhy getDelTemplate() {
        return delTemplate;
    }
}
