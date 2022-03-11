package com.didichuxing.datachannel.arius.admin.biz.template.srv.setting;

import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.template.ConsoleTemplateSettingDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.template.TemplateSettingDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.template.TemplateSettingVO;
import com.didichuxing.datachannel.arius.admin.client.mapping.AriusIndexTemplateSetting;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.template.IndexTemplatePhySettings;
import com.didichuxing.datachannel.arius.admin.common.exception.AdminOperateException;

/**
 * 逻辑模板settings service
 * @author wangshu
 * @date 2020/09/02
 */
public interface TemplateLogicSettingsManager {

    /**
     * 修改模板Setting
     * @param settingDTO Setting
     * @param operator 操作者
     * @return Result
     * @throws AdminOperateException
     */
    Result<Void> modifySetting(ConsoleTemplateSettingDTO settingDTO, String operator) throws AdminOperateException;

    /**
     * 修改模板Setting(仅开放对于副本设置和异步translog落盘方式的设置)
     * @param settingDTO 模板seting修改类
     * @param operator 操作者
     * @throws AdminOperateException
     */
    Result<Void> customizeSetting(TemplateSettingDTO settingDTO, String operator) throws AdminOperateException;

    /**
     * 获取逻辑模板settings
     * @param logicId 逻辑模板ID
     * @return
     * @throws AdminOperateException
     */
    Result<IndexTemplatePhySettings> getSettings(Integer logicId) throws AdminOperateException;

    /**
     * 创建逻辑模板settings视图
     * @param logicId 逻辑模板ID
     * @return 索引模板视图信息
     */
    Result<TemplateSettingVO> buildTemplateSettingVO(Integer logicId);

    /**
     * 更新settings信息
     * @param logicId 逻辑ID
     * @param settings settings
     * @return
     */
    Result<Void> updateSettings(Integer logicId, String operator, AriusIndexTemplateSetting settings);

    /**
     * 更加逻辑ID获取Settings
     * @param logicId 逻辑ID
     * @return
     */
    Result<IndexTemplatePhySettings> getTemplateSettings(Integer logicId);
}
