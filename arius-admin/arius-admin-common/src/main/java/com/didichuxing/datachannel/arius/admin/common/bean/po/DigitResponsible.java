package com.didichuxing.datachannel.arius.admin.common.bean.po;

/**
 * 所有实现了该接口的类的对想转换可以使用ResponsibleConvertTool来转换,内部实现了责任人的编码解码
 * @author d06679
 * @date 2019/3/18
 */
public interface DigitResponsible {

    String getResponsible();

    void setResponsible(String responsibleIds);

}