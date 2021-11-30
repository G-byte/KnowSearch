package com.didichuxing.datachannel.arius.admin.core.service.cluster.ecm.impl;

import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.cluster.ESPackageDTO;
import com.didichuxing.datachannel.arius.admin.client.constant.operaterecord.OperationEnum;
import com.didichuxing.datachannel.arius.admin.client.constant.resource.ESClusterTypeEnum;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.espackage.ESPackage;
import com.didichuxing.datachannel.arius.admin.common.bean.po.espackage.ESPackagePO;
import com.didichuxing.datachannel.arius.admin.common.bean.po.esplugin.ESPluginPO;
import com.didichuxing.datachannel.arius.admin.common.constant.FileCompressionType;
import com.didichuxing.datachannel.arius.admin.common.util.AriusObjUtils;
import com.didichuxing.datachannel.arius.admin.common.util.ConvertUtil;
import com.didichuxing.datachannel.arius.admin.common.util.ESVersionUtil;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.ecm.ESPackageService;
import com.didichuxing.datachannel.arius.admin.core.service.common.AriusUserInfoService;
import com.didichuxing.datachannel.arius.admin.core.service.extend.storage.FileStorageService;
import com.didichuxing.datachannel.arius.admin.persistence.mysql.ecm.ESPackageDAO;
import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.didichuxing.datachannel.arius.admin.client.constant.operaterecord.ModuleEnum.ES_CLUSTER_PLUGINS;
import static com.didichuxing.datachannel.arius.admin.client.constant.operaterecord.OperationEnum.*;

/**
 * @author linyunan
 * @date 2021-05-19
 */
@Service
public class ESPackageServiceImpl implements ESPackageService {
    private static final ILog    LOGGER = LogFactory.getLog(ESPackageServiceImpl.class);

    @Autowired
    private ESPackageDAO         esPackageDAO;

    @Autowired
    private AriusUserInfoService ariusUserInfoService;

    @Autowired
    private FileStorageService   fileStorageService;

    private static final Long MULTI_PART_FILE_SIZE_MAX = 1024 * 1024 * 500L;

    @Override
    public List<ESPackage> listESPackage() {
        return ConvertUtil.list2List(esPackageDAO.listAll(), ESPackage.class);
    }

    @Override
    public Result<Long> addESPackage(ESPackageDTO esPackageDTO, String operator) {
        Result<Void> checkResult = checkValid(esPackageDTO, operator, ADD);
        if (checkResult.failed()) {
            return Result.buildFrom(checkResult);
        }

        if (isHostType(esPackageDTO)) {
            Result<Void> uploadResult = upload(esPackageDTO);
            if (uploadResult.failed()) {
                return Result.buildFrom(uploadResult);
            }
        }

        return saveESPackageToDB(esPackageDTO);
    }

    @Override
    public Result<ESPackage> updateESPackage(ESPackageDTO esPackageDTO, String operator) {
        Result<Void> checkResult = checkValid(esPackageDTO, operator, EDIT);
        if (checkResult.failed()) {
            return Result.buildFrom(checkResult);
        }

        if (isHostType(esPackageDTO)) {
            Result<Void> uploadResult = upload(esPackageDTO);
            if (uploadResult.failed()) {
                return Result.buildFrom(uploadResult);
            }
        }

        return updatePackageToDB(esPackageDTO);
    }

    @Override
    public ESPackage getESPackagePOById(Long id) {
        return ConvertUtil.obj2Obj(esPackageDAO.getById(id), ESPackage.class);
    }

    @Override
    public Result<Long> deleteESPackage(Long id, String operator) {
        // 集群版本删除操作时进行的参数校验
        ESPackageDTO esPackageDTO = new ESPackageDTO();
        esPackageDTO.setId(id);
        Result<Void> checkResult = checkValid(esPackageDTO, operator, DELETE);
        if (checkResult.failed()) {
            return Result.buildFrom(checkResult);
        }

        // 在文件系统中删除对应的集群版本文件
        ESPackagePO esPackagePO = esPackageDAO.getById(id);
        Result<Void> response = fileStorageService.remove(getUniqueFileName(ConvertUtil.obj2Obj(esPackagePO, ESPackage.class)));
        if (response.failed()) {
            return Result.buildFail("删除文件失败");
        }

        // 删除数据库中id对应的集群版本的信息
        boolean succ = (1 == esPackageDAO.delete(id));
        return Result.build(succ, id);
    }

    @Override
    public ESPackage getByVersionAndType(String esVersion, Integer manifest) {
        return ConvertUtil.obj2Obj(esPackageDAO.getByVersionAndType(esVersion, manifest), ESPackage.class);
    }

    /*************************************************private**********************************************************/
    private Result<Void> upload(ESPackageDTO esPackageDTO) {
        Result<String> response = Result.buildFail();
        try {
            if (esPackageDTO.getUploadFile() != null) {
                response = fileStorageService.upload(getUniqueFileName(ConvertUtil.obj2Obj(esPackageDTO, ESPackage.class)), esPackageDTO.getMd5(),
                    esPackageDTO.getUploadFile());
                if (response.success()) {
                    esPackageDTO.setUrl(response.getData());
                } else {
                    return Result.buildFail("上传文件失败");
                }
            }
        } catch (Exception e) {
            LOGGER.info("class=ESPackageServiceImpl||method=addESPlugin||uploadResponse={}||pluginName={}||exception={}",
                response.getMessage(), esPackageDTO.getFileName(), e);
        }

        return Result.buildSucc();
    }

    private boolean isHostType(ESPackageDTO esPackageDTO) {
        return esPackageDTO.getManifest() == ESClusterTypeEnum.ES_HOST.getCode();
    }

    private Result<Void> checkValid(ESPackageDTO esPackageDTO, String operator, OperationEnum operation) {
        if (AriusObjUtils.isNull(esPackageDTO)) {
            return Result.buildParamIllegal("安装包为空");
        }

        if (!ariusUserInfoService.isOPByDomainAccount(operator)) {
            return Result.buildFail("非运维人员不能更新ES安装包!");
        }

        if(operation.equals(UNKNOWN)) {
            return Result.buildFail("操作类型未知");
        }

        if (!ESVersionUtil.isValid(esPackageDTO.getEsVersion()) && !operation.equals(DELETE)) {
            return Result.buildParamIllegal("版本号格式不正确, 必须是'1.1.1.1000'类似的格式");
        }

        ESPackagePO packageByVersion = null;
        if (operation.getCode() == ADD.getCode()) {
            packageByVersion = esPackageDAO.getByVersionAndType(esPackageDTO.getEsVersion(),
                esPackageDTO.getManifest());
            if (esPackageDTO.getUploadFile().getSize() > MULTI_PART_FILE_SIZE_MAX) {
                return Result.buildFail("es程序包[" + esPackageDTO.getFileName() + "]文件的大小超过限制，不能超过" + MULTI_PART_FILE_SIZE_MAX / 1024 / 1024 + "M");
            }
        } else if (operation.getCode() == EDIT.getCode()) {
            packageByVersion = esPackageDAO.getByVersionAndManifestNotSelf(esPackageDTO.getEsVersion(),
                esPackageDTO.getManifest(), esPackageDTO.getId());
        } else if(operation.getCode() == DELETE.getCode()) {
            if (null == esPackageDTO.getId()) {
                return Result.buildFail("所要删除的集群版本字段为空");
            }

            ESPackagePO esPackagePO = esPackageDAO.getById(esPackageDTO.getId());
            if(esPackagePO == null) {
                return Result.buildFail("对应id的集群版本不存在");
            }
        }

        if (!AriusObjUtils.isNull(packageByVersion)) {
            return Result.buildParamIllegal("版本号重复");
        }

        return Result.buildSucc();
    }

    private Result<Long> saveESPackageToDB(ESPackageDTO esPackageDTO) {
        ESPackagePO esPackagePO = ConvertUtil.obj2Obj(esPackageDTO, ESPackagePO.class);

        boolean succ = Boolean.FALSE;
        try {
            succ = (1 == esPackageDAO.insert(esPackagePO));
        } catch (Exception e) {
            LOGGER.info("class=ESPackageServiceImpl||method=saveESPackageToDB||msg={}", e.getMessage());
        }

        return Result.build(succ, esPackagePO.getId());
    }

    private Result<ESPackage> updatePackageToDB(ESPackageDTO esPackageDTO) {
        ESPackagePO esPackagePO = ConvertUtil.obj2Obj(esPackageDTO, ESPackagePO.class);
        boolean succ = Boolean.FALSE;
        try {
            succ = (1 == esPackageDAO.update(esPackagePO));
        } catch (Exception e) {
            LOGGER.info("class=ESPackageServiceImpl||method=updatePackageToDB||msg={}", e.getMessage());
        }

        return Result.build(succ, ConvertUtil.obj2Obj(esPackagePO, ESPackage.class));
    }

    private String getUniqueFileName(ESPackage esPackage) {
        return esPackage.getEsVersion() + "-" + esPackage.getManifest() + FileCompressionType.TAR_GZ;
    }
}
