package com.didichuxing.datachannel.arius.admin.client.bean.vo.order;

import com.didichuxing.datachannel.arius.admin.client.bean.vo.BaseVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author fengqiongfeng
 * @date 2020/8/25
 */
@Data
@NoArgsConstructor
public class OrderTypeVO extends BaseVO {
    @ApiModelProperty(value = "工单类型")
    private String type;

    @ApiModelProperty(value = "描述信息")
    private String message;

    public OrderTypeVO(String type, String message) {
        this.type = type;
        this.message = message;
    }
}
