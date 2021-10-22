package com.didichuxing.datachannel.arius.admin.remote.elasticcloud.bean.response;

import com.didichuxing.datachannel.arius.admin.client.bean.common.ecm.response.EcmTaskStatus;
import com.didichuxing.datachannel.arius.admin.remote.elasticcloud.bean.bizenum.ElasticCloudHostStatusEnum;
import lombok.Data;

import java.util.Date;

@Data
public class ElasticCloudPod {

    /**
     * pod名称
     */
    private String   podName;

    /**
     * podIP
     */
    private String   podIp;

    /**
     * pod主机名称
     */
    private String   podHostname;

    /**
     *pod下标
     */
    private Integer  podIndex;

    /**
     * 宿主机IP
     */
    private String   nodeIp;

    /**
     * 宿主机名称
     */
    private String   nodeName;

    /**
     * 服务名称
     */
    private String   svcName;

    /**
     * 状态
     */
    private String   status;

    /**
     * 创建时间
     */
    private Date     created;

    /**
     * 组内下标
     */
    private Integer  groupIdx;

    /**
     * 分组
     */
    private Integer  group;

    /**
     * 转换成 EcmTaskStatus
     * @see EcmTaskStatus
     */
    public EcmTaskStatus convert2EcmTaskStatus(Integer taskId, String clusterState) {
        EcmTaskStatus ecmTaskStatus = new EcmTaskStatus();
        ecmTaskStatus.setHostname(podHostname);
        ecmTaskStatus.setTaskId(taskId);
        ecmTaskStatus.setPodIp(podIp);
        ecmTaskStatus.setGroup(group);
        ecmTaskStatus.setPodIndex(podIndex);
        ecmTaskStatus.setStatusEnum(ElasticCloudHostStatusEnum.getEcmHostStatus(this.status, clusterState));
        return ecmTaskStatus;
    }
}
