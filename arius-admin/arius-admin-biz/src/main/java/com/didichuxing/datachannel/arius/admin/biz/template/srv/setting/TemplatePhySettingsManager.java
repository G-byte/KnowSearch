package com.didichuxing.datachannel.arius.admin.biz.template.srv.setting;

import com.didichuxing.datachannel.arius.admin.common.exception.AdminOperateException;
import com.didichuxing.datachannel.arius.admin.common.exception.ESOperateException;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.template.IndexTemplatePhySettings;

import java.util.Map;

public interface TemplatePhySettingsManager {
    /**
     * 校验当前物理模板settings信息
     * @param cluster 物理集群名称
     * @param template 逻辑模板
     * @param settings settings.
     * @return
     */
    boolean validTemplateSettings(String cluster, String template,
                                  IndexTemplatePhySettings settings) throws ESOperateException;

    /**
     * 获取模板settings
     * @param cluster 集群名称
     * @param template 模板名称
     * @return
     */
    IndexTemplatePhySettings fetchTemplateSettings(String cluster, String template) throws ESOperateException;

    /**
     * merge当前settings，并更新到物理模板中
     * @param cluster 集群名称
     * @param template 模板名称
     * @param settings 增量配置
     * @return
     */
    boolean mergeTemplateSettings(String cluster, String template,
                                  Map<String, String> settings) throws AdminOperateException;
}
