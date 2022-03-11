package com.didichuxing.datachannel.arius.admin.common.bean.entity.task.detail;

import java.util.List;

import com.didichuxing.datachannel.arius.admin.client.constant.dcdr.DcdrStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DCDRTasksDetail extends AbstractTaskDetail {
    private List<DCDRSingleTemplateMasterSlaveSwitchDetail> dcdrSingleTemplateMasterSlaveSwitchDetailList;

    private int                    total;
    private int                    successNum;
    private int                    failedNum;
    private int                    runningNum;
    private int                    cancelNum;
    private int                    waitNum;

    /**
     * 0 取消 1 成功 2 运行中 3 失败 4 待运行
     */
    private int                    state;

    private int                    percent;

    public void calculateProcess() {
        int successNum    = 0;
        int failedNum     = 0;
        int runningNum    = 0;
        int cancelNum     = 0;
        int waitNum       = 0;

        for (DCDRSingleTemplateMasterSlaveSwitchDetail dcdrSingleTemplateMasterSlaveSwitchDetail : this.dcdrSingleTemplateMasterSlaveSwitchDetailList) {
            if (DcdrStatusEnum.SUCCESS.getCode().equals(dcdrSingleTemplateMasterSlaveSwitchDetail.getTaskStatus()))   successNum++;
            if (DcdrStatusEnum.CANCELLED.getCode().equals(dcdrSingleTemplateMasterSlaveSwitchDetail.getTaskStatus())) cancelNum++;
            if (DcdrStatusEnum.RUNNING.getCode().equals(dcdrSingleTemplateMasterSlaveSwitchDetail.getTaskStatus()))   runningNum++;
            if (DcdrStatusEnum.FAILED.getCode().equals(dcdrSingleTemplateMasterSlaveSwitchDetail.getTaskStatus()))    failedNum++;
            if (DcdrStatusEnum.WAIT.getCode().equals(dcdrSingleTemplateMasterSlaveSwitchDetail.getTaskStatus()))      waitNum++;
        }

        this.total      = this.dcdrSingleTemplateMasterSlaveSwitchDetailList.size();
        this.successNum = successNum;
        this.failedNum  = failedNum;
        this.runningNum = runningNum;
        this.cancelNum  = cancelNum;
        this.waitNum    = waitNum;
        this.percent    = successNum * 100 / this.total;

        if (runningNum > 0) {
            this.state = DcdrStatusEnum.RUNNING.getCode(); return;
        }
        if (failedNum > 0) {
            this.state = DcdrStatusEnum.FAILED.getCode(); return;
        }
        if (cancelNum == this.dcdrSingleTemplateMasterSlaveSwitchDetailList.size()) {
            this.state = DcdrStatusEnum.CANCELLED.getCode(); return;
        }
        if (cancelNum > 0 && (cancelNum + successNum) == this.dcdrSingleTemplateMasterSlaveSwitchDetailList.size()) {
            this.state = DcdrStatusEnum.CANCELLED.getCode(); return;
        }
        if (successNum > 0 && successNum == this.dcdrSingleTemplateMasterSlaveSwitchDetailList.size()) {
            this.state = DcdrStatusEnum.SUCCESS.getCode();
        }
    }
}
