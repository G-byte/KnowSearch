package com.didichuxing.datachannel.arius.admin.metadata.job;

import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;

import java.util.List;

public abstract class AbstractMetaDataJob {
    protected static final ILog LOGGER = LogFactory.getLog(AbstractMetaDataJob.class);

    public abstract Object handleJobTask(String params);

    public Object handleBrocastJobTask(String params, String curretnWorker, List<String> allWorders){
        return null;
    }
}
