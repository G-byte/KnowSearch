package com.didichuxing.datachannel.arius.admin.task.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.didichuxing.datachannel.arius.admin.metadata.job.index.IndexCatInfoCollector;
import com.didiglobal.logi.job.annotation.Task;
import com.didiglobal.logi.job.common.TaskResult;
import com.didiglobal.logi.job.core.job.Job;
import com.didiglobal.logi.job.core.job.JobContext;

@Task(name = "IndexCatInfoCollectorTask", description = "采集索引Cat/Index基本信息", cron = "0 0/3 * * * ? *", autoRegister = true)
public class IndexCatInfoCollectorTask implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexCatInfoCollectorTask.class);

    @Autowired
    private IndexCatInfoCollector indexCatInfoCollector;

    @Override
    public TaskResult execute(JobContext jobContext) throws Exception {
        LOGGER.info("class=IndexCatInfoCollectorTask||method=execute||msg=start");
        indexCatInfoCollector.handleJobTask("");
        return TaskResult.SUCCESS;
    }
}
