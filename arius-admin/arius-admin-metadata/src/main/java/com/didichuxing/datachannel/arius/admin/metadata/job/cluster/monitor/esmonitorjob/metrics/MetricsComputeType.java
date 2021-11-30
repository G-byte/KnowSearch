package com.didichuxing.datachannel.arius.admin.metadata.job.cluster.monitor.esmonitorjob.metrics;

import com.didichuxing.datachannel.arius.admin.common.bean.entity.stats.CollectBean;

public enum MetricsComputeType {
    /**
     * MINUS
     */
    MINUS,
    /**
     * AVG
     */
    AVG,
    /**
     * DERIVE_DIVISION
     */
    DERIVE_DIVISION,
    /**
     * NONE
     */
    NONE;

    public Double compute(CollectBean before, CollectBean now) {
        Double beforeD = before.getValue();
        Double nowD    = now.getValue();

        switch (this) {
            case MINUS:
                return minusByDouble(beforeD, nowD);
            case AVG:
                return avg(before, now, minusByDouble(beforeD, nowD));
            case DERIVE_DIVISION:
                return divide(beforeD, nowD);
            case NONE:
                return nowD;
            default:
                return null;
        }
    }

    public MetricsComputer getComputer(MetricsRegister metricsRegister) {
        if (this == DERIVE_DIVISION) {
            return new DeriveComputer(this, metricsRegister);
        }

        return new SimpleComputer(this, metricsRegister);
    }

    private Double avg(CollectBean before, CollectBean now, Double result) {
        if(result == null){return null;}
        if(result == 0.0){return 0.0;}

        //now.getTimestamp()时间戳单位是毫秒
        Double value = 1000 * result / (now.getTimestamp() - before.getTimestamp());
        if(value < 0.0){value = 0.0;}

        return value;
    }

    private Double divide(Double first, Double second) {
        if (first == 0.0 || second == 0.0){return 0.0;}

        return first / second;
    }

    private Double minusByDouble(Double before, Double now) {
        return now - before;
    }
}
