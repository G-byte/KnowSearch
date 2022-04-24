package com.didichuxing.datachannel.arius.admin.client.bean.vo.metrics.other.cluster;

import com.didichuxing.datachannel.arius.admin.client.bean.vo.metrics.percentiles.ESPercentilesMetricsVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by linyunan on 2021-07-31
 */
@Data
@NoArgsConstructor
@ApiModel("CPU使用率指标信息")
public class CpuUsageMetricsVO extends ESPercentilesMetricsVO {
}
