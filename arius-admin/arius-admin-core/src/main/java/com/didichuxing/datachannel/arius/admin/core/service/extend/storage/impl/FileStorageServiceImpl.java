package com.didichuxing.datachannel.arius.admin.core.service.extend.storage.impl;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.common.component.HandleFactory;
import com.didichuxing.datachannel.arius.admin.core.service.extend.storage.FileStorageService;
import com.didichuxing.datachannel.arius.admin.remote.storage.FileStorageHandle;
import com.didichuxing.datachannel.arius.admin.remote.storage.content.FileStorageTypeEnum;
import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;

import javax.annotation.PostConstruct;

/**
 * @author linyunan
 * @date 2021-05-19
 */
@Service
@NoArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final ILog LOGGER = LogFactory.getLog(FileStorageServiceImpl.class);

    @Autowired
    private HandleFactory handleFactory;

    @Value("${extend.fileStorage}")
    private String fileStorageType;

    @PostConstruct
    public void fileStorageTypeCheck() {
        FileStorageTypeEnum fileStorageTypeEnum = FileStorageTypeEnum.valueOfType(fileStorageType);
        if (fileStorageTypeEnum.getCode().equals(FileStorageTypeEnum.UNKNOWN.getCode())) {
            LOGGER.error("class=FileStorageServiceImpl||method=fileStorageTypeCheck||fileStorageType={}", fileStorageTypeEnum);
        }
    }

    @Override
    public Result<String> upload(String fileName, String fileMd5, MultipartFile uploadFile) {
        Result<FileStorageHandle> result = getFileStorageHandleByType(fileStorageType);
        if (result.failed()) {
            LOGGER.info("class=FileStorageServiceImpl||method=upload||fileStorageType={}", fileStorageType);
            return Result.buildFrom(result);
        }
        return result.getData().upload(fileName, fileMd5, uploadFile);
    }

    @Override
    public Result<Void> remove(String fileName) {
        Result<FileStorageHandle> result = getFileStorageHandleByType(fileStorageType);
        if (result.failed()) {
            LOGGER.info("class=FileStorageServiceImpl||method=remove||fileStorageType={}", fileStorageType);
            return Result.buildFrom(result);
        }
        return result.getData().remove(fileName);
    }

    @Override
    public Result<MultipartFile> download(String fileName) {
        Result<FileStorageHandle> result = getFileStorageHandleByType(fileStorageType);
        if (result.failed()) {
            LOGGER.info("class=FileStorageServiceImpl||method=download||fileStorageType={}", fileStorageType);
            return Result.buildFrom(result);
        }
        return result.getData().download(fileName);
    }

    @Override
    public Result<String> getDownloadBaseUrl() {
        Result<FileStorageHandle> result = getFileStorageHandleByType(fileStorageType);
        if (result.failed()) {
            LOGGER.info("class=FileStorageServiceImpl||method=getDownloadBaseUrl||fileStorageType={}", fileStorageType);
            return Result.buildFrom(result);
        }
        return Result.build(Boolean.TRUE, result.getData().getDownloadBaseUrl());
    }

    /*****************************************private*************************************************************/

    private Result<FileStorageHandle> getFileStorageHandleByType(String fileStorageType) {
        if (FileStorageTypeEnum.valueOfType(fileStorageType).getCode() == -1) {
            return Result.buildFail(String.format("获取 %s 类型出错", fileStorageType));
        }
        LOGGER.info("class=FileStorageServiceImpl||method=getDownloadBaseUrl||fileStorageType={}", fileStorageType);
        FileStorageHandle fileStorageHandle = (FileStorageHandle) handleFactory.getByHandlerNamePer(fileStorageType);
        return Result.buildSucc(fileStorageHandle);
    }
}
