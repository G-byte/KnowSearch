package com.didichuxing.datachannel.arius.admin.core.service.extend.employee.impl;

import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.employee.BaseEmInfo;
import com.didichuxing.datachannel.arius.admin.common.component.HandleFactory;
import com.didichuxing.datachannel.arius.admin.common.util.AriusObjUtils;
import com.didichuxing.datachannel.arius.admin.core.service.extend.employee.EmployeeService;
import com.didichuxing.datachannel.arius.admin.remote.employee.EmployeeHandle;
import com.didichuxing.datachannel.arius.admin.remote.employee.content.EmployeeTypeEnum;
import com.didichuxing.datachannel.arius.admin.remote.storage.content.FileStorageTypeEnum;
import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * @author linyunan
 * @date 2021-05-17
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final ILog LOGGER = LogFactory.getLog(EmployeeServiceImpl.class);

    @Autowired
    private HandleFactory     handleFactory;

    @Value("${extend.employee}")
    private String employeeType;

    @PostConstruct
    public void employeeTypeCheck() {
        EmployeeTypeEnum employeeTypeEnum = EmployeeTypeEnum.valueOfType(employeeType);
        if (employeeTypeEnum.getCode().equals(FileStorageTypeEnum.UNKNOWN.getCode())) {
            LOGGER.info("class=EmployeeServiceImpl||method=employeeTypeCheck||employeeType={}", employeeTypeEnum);
        }
    }

    @Override
    public <T extends BaseEmInfo> Result<T> getByDomainAccount(String domainAccount) {
        Result<String> getEmployeeTypeResult = getEmployeeType();
        if (getEmployeeTypeResult.failed()) {
            return Result.buildFrom(getEmployeeTypeResult);
        }

        LOGGER.info("class=EmployeeServiceImpl||method=getByDomainAccount||employeeType={}", employeeType);

        return ((EmployeeHandle) handleFactory.getByHandlerNamePer(employeeType)).getByDomainAccount(domainAccount);
    }

    @Override
    public Result<Void> checkUsers(String domainAccounts) {
        Result<String> getEmployeeTypeResult = getEmployeeType();
        if (getEmployeeTypeResult.failed()) {
            return Result.buildFrom(getEmployeeTypeResult);
        }

        LOGGER.info("class=EmployeeServiceImpl||method=checkUsers||employeeType={}", employeeType);

        return ((EmployeeHandle) handleFactory.getByHandlerNamePer(employeeType)).checkUsers(domainAccounts);
    }

    @Override
    public Result<Object> searchOnJobStaffByKeyWord(String keyWord) {

        Result<String> getEmployeeTypeResult = getEmployeeType();
        if (getEmployeeTypeResult.failed()) {
            return Result.buildFrom(getEmployeeTypeResult);
        }

        LOGGER.info("class=EmployeeServiceImpl||method=searchOnJobStaffByKeyWord||employeeType={}", employeeType);

        return ((EmployeeHandle) handleFactory.getByHandlerNamePer(employeeType)).searchOnJobStaffByKeyWord(keyWord);
    }

    /*************************************private****************************************************/
    private Result<String> getEmployeeType() {
        if (AriusObjUtils.isNull(employeeType)) {
            return Result.build(Boolean.TRUE, EmployeeTypeEnum.DEFAULT.getType());
        }

        if (EmployeeTypeEnum.valueOfType(employeeType).getCode() == -1) {
            return Result.buildFail(String.format("获取 %s 类型出错", employeeType));
        }
        return Result.buildSucc();
    }
}
