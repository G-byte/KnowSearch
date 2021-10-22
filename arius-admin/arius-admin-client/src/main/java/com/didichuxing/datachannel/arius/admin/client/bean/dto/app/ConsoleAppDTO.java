package com.didichuxing.datachannel.arius.admin.client.bean.dto.app;

import com.didichuxing.datachannel.arius.admin.client.bean.dto.BaseDTO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 *
 * @author d06679
 * @date 2019/3/13
 */
@Data
@ApiModel(description = "APP信息")
public class ConsoleAppDTO extends BaseDTO {

    @ApiModelProperty("APPID")
    private Integer id;

    @ApiModelProperty("部门ID")
    private String  departmentId;

    @ApiModelProperty("部门名称")
    private String  department;

    @ApiModelProperty("责任人")
    private String  responsible;

    @ApiModelProperty("备注")
    private String  memo;

    @ApiModelProperty("数据中心")
    private String  dataCenter;
}
