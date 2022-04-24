package com.didichuxing.datachannel.arius.admin.biz.metrics.handle;

import com.didichuxing.datachannel.arius.admin.biz.component.MetricsValueConvertUtils;
import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.metrics.MetricsClusterPhyDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.metrics.MetricsVO;
import com.didichuxing.datachannel.arius.admin.client.bean.vo.metrics.top.VariousLineChartMetricsVO;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.metrics.linechart.VariousLineChartMetrics;
import com.didichuxing.datachannel.arius.admin.common.component.BaseHandle;
import com.didichuxing.datachannel.arius.admin.common.util.AriusObjUtils;
import com.didichuxing.datachannel.arius.admin.common.util.ConvertUtil;
import com.didichuxing.datachannel.arius.admin.core.service.app.AppService;
import com.didichuxing.datachannel.arius.admin.core.service.common.AriusUserInfoService;
import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static com.didichuxing.datachannel.arius.admin.common.constant.ClusterConstant.*;
import static com.didichuxing.datachannel.arius.admin.common.constant.ClusterConstant.MAX_TIME_INTERVAL;

public abstract class BaseClusterMetricsHandle implements BaseHandle {
    private static final ILog LOGGER = LogFactory.getLog(BaseClusterMetricsHandle.class);

    @Autowired
    private AppService appService;

    @Autowired
    private AriusUserInfoService ariusUserInfoService;

    /**
     * 获取物理集群节点、节点任务、模板或者节点任务的指标信息
     * @param domainAccount 账号类型
     * @param appId appId
     * @param param 物理集群指标信息
     * @return 对应视图板块下的时序指标信息列表
     */
    public Result<List<VariousLineChartMetricsVO>> getClusterPhyRelatedCurveMetrics(MetricsClusterPhyDTO param, Integer appId, String domainAccount) {
        //1. verification
        Result<Void> checkParamResult = checkParamForClusterPhyMetrics(param, appId, domainAccount);
        if (checkParamResult.failed()) {
            LOGGER.warn("class=ClusterBaseMetricsHandle||method=getClusterPhyMetrics||msg=check param fail");
            return Result.buildFrom(checkParamResult);
        }

        //2. initialization
        init(param);

        //3. get metrics from es engine
        List<VariousLineChartMetrics> variousLineChartMetrics = getAggClusterPhyMetrics(param);

        //4. uniform percentage unit
        MetricsValueConvertUtils.convertClusterPhyMetricsPercent(variousLineChartMetrics);

        //5. optimize query burr
        MetricsValueConvertUtils.doOptimizeQueryBurrForNodeOrIndicesMetrics(variousLineChartMetrics);

        return Result.buildSucc(ConvertUtil.list2List(variousLineChartMetrics, VariousLineChartMetricsVO.class));
    }

    /**
     * 获取当前时刻集群的整体指标,其中包含非曲线数据，例如集群总览视图指标
     * @param param 物理集群指标信息
     * @param appId appId
     * @param domainAccount 账号类型
     * @return 当前时刻下的集群整体指标
     */
    public Result<MetricsVO> getOtherClusterPhyRelatedMetricsVO(MetricsClusterPhyDTO param, Integer appId, String domainAccount) {
        //1. verification
        Result<Void> checkParamResult = checkParamForClusterPhyMetrics(param, appId, domainAccount);
        if (checkParamResult.failed()) {
            LOGGER.warn("class=ClusterBaseMetricsHandle||method=getMetricsVO||msg=check param fail");
            return Result.buildFrom(checkParamResult);
        }

        //2. initialization
        init(param);

        return Result.buildSucc(buildClusterPhyMetricsVO(param));
    }

    private Result<Void> checkParamForClusterPhyMetrics(MetricsClusterPhyDTO param, Integer appId,
                                                        String domainAccount) {
        Result<Void> checkCommonParam = checkCommonParam(param, appId, domainAccount);
        if (checkCommonParam.failed()) {
            return checkCommonParam;
        }

        Result<Void> checkSpecialParamResult = checkSpecialParam(param);
        if (checkSpecialParamResult.failed()) {
            return checkSpecialParamResult;
        }

        return Result.buildSucc();
    }

    private Result<Void> checkCommonParam(MetricsClusterPhyDTO param, Integer appId, String domainAccount) {
        if (null == param) {
            return Result.buildParamIllegal("param is empty");
        }

        if (null == appId) {
            return Result.buildParamIllegal("appId is empty");
        }

        if (null == ariusUserInfoService.getByName(domainAccount)) {
            return Result.buildParamIllegal("user info is empty");
        }

        if (!appService.isAppExists(appId)) {
            return Result.buildParamIllegal(String.format("There is no appId:%s", appId));
        }

        return Result.buildSucc();
    }

    private void init(MetricsClusterPhyDTO param) {
        initCommonParam(param);
        initMetricsClusterPhy(param);
    }

    private void initCommonParam(MetricsClusterPhyDTO param) {
        if (AriusObjUtils.isBlack(param.getClusterPhyName())) {
            param.setClusterPhyName(ALL_CLUSTER);
        }

        if (0 == param.getEndTime() || null == param.getEndTime()) {
            param.setEndTime(System.currentTimeMillis());
        }

        if (0 == param.getStartTime() || null == param.getStartTime()) {
            param.setStartTime(param.getEndTime() - DEFAULT_TIME_INTERVAL);
        }

        //防止内存打爆, 触发熔断, 兜底方案, 结束时间近一周
        long intervalTime = param.getEndTime() - param.getStartTime();
        if (intervalTime > MAX_TIME_INTERVAL) {
            param.setStartTime(param.getEndTime() - MAX_TIME_INTERVAL);
        }

        if (null != param.getTopNu()) {
            if (param.getTopNu() <= 0) {
                param.setTopNu(5);
            }

            if (param.getTopNu() > 20) {
                param.setTopNu(20);
            }
        }
    }

    /**
     * 构建物理集群的整体指标
     *
     * @param param 物理集群指标
     * @return 集群指标类型视图
     */
    protected MetricsVO buildClusterPhyMetricsVO(MetricsClusterPhyDTO param) {
        return null;
    }

    /**
     * 从ES引擎中获取对应的物理指标类型
     *
     * @param param 物理集群指标
     * @return 对应指标下的时序信息列表
     */
    protected List<VariousLineChartMetrics> getAggClusterPhyMetrics(MetricsClusterPhyDTO param) {
        return new ArrayList<>();
    }

    /**
     * 不同视图间自有的校验规则
     *
     * @param param 物理集群指标
     * @return 校验结果
     */
    protected abstract Result<Void> checkSpecialParam(MetricsClusterPhyDTO param);

    /**
     * 初始化物理指标信息DTO
     *
     * @param param 物理集群指标
     */
    protected abstract void initMetricsClusterPhy(MetricsClusterPhyDTO param);
}
