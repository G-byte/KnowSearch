package com.didichuxing.datachannel.arius.admin.common.event.auth;

import com.didichuxing.datachannel.arius.admin.common.bean.entity.app.AppClusterLogicAuth;

/**
 * App逻辑集群删除事件
 * @author wangshu
 * @date 2020/09/19
 */
public class AppLogicClusterAuthDeleteEvent extends AppAuthEvent {
    private AppClusterLogicAuth logicClusterAuth;

    public AppLogicClusterAuthDeleteEvent(Object source, AppClusterLogicAuth appClusterLogicAuth) {
        super(source);
        this.logicClusterAuth = appClusterLogicAuth;
    }

    public AppClusterLogicAuth getLogicClusterAuth() {
        return logicClusterAuth;
    }
}
