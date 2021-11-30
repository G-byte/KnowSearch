package com.didichuxing.datachannel.arius.admin.biz.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.didichuxing.datachannel.arius.admin.biz.cluster.ClusterContextManager;
import com.didichuxing.datachannel.arius.admin.biz.cluster.ClusterPhyManager;
import com.didichuxing.datachannel.arius.admin.common.constant.arius.AriusUser;
import com.didichuxing.datachannel.arius.admin.common.event.resource.ClusterPhyEvent;
import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;

/**
 * Created by linyunan on 2021-06-03
 */
@Component
public class ClusterPhyChangeListener implements ApplicationListener<ClusterPhyEvent> {

    private static final ILog      LOGGER = LogFactory.getLog(ClusterPhyChangeListener.class);

    @Autowired
    private ClusterContextManager  clusterContextManager;

    @Autowired
    private ClusterPhyManager      clusterPhyManager;

    @Override
    public void onApplicationEvent(ClusterPhyEvent event) {
        try {
            clusterContextManager.flushClusterPhyContext(event.getClusterPhyName());

            clusterPhyManager.updateClusterHealth(event.getClusterPhyName(), AriusUser.SYSTEM.getDesc());
        } catch (RuntimeException e) {
            LOGGER.error(
                "class=ClusterPhyChangeListener||method=onApplicationEvent||appId={}||clusterPhyName={}||ErrorMsg={}",
                event.getAppId(), event.getClusterPhyName(), e.getMessage());
        }
    }
}
