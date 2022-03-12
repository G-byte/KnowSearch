package com.didichuxing.datachannel.arius.admin.core.service.cluster.region;

import com.didichuxing.datachannel.arius.admin.AriusAdminApplicationTests;
import com.didichuxing.datachannel.arius.admin.client.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.cluster.ClusterRegionDTO;
import com.didichuxing.datachannel.arius.admin.client.bean.dto.cluster.ESLogicClusterRackInfoDTO;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ClusterLogic;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ClusterPhy;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.template.IndexTemplatePhy;
import com.didichuxing.datachannel.arius.admin.common.bean.po.cluster.ClusterRegionPO;
import com.didichuxing.datachannel.arius.admin.common.constant.AdminConstant;
import com.didichuxing.datachannel.arius.admin.common.util.ConvertUtil;
import com.didichuxing.datachannel.arius.admin.common.util.RackUtils;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.logic.ClusterLogicService;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.physic.ClusterPhyService;
import com.didichuxing.datachannel.arius.admin.core.service.template.physic.TemplatePhyService;
import com.didichuxing.datachannel.arius.admin.persistence.mysql.region.ClusterRegionDAO;
import com.didichuxing.datachannel.arius.admin.util.CustomDataSource;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Transactional
@Rollback
public class RegionRackServiceTest extends AriusAdminApplicationTests {

    @Autowired
    private ClusterRegionDAO clusterRegionDAO;

    @MockBean
    private ClusterLogicService clusterLogicService;

    @MockBean
    private ClusterPhyService esClusterPhyService;

    @MockBean
    private TemplatePhyService templatePhyService;

    @Autowired
    private RegionRackService regionRackService;

    private static String OPERATOR = "wpk";

    private static String racks = "r1,r2";

    private static String clusterName = "wpk";

    @BeforeEach
    public void mockRules() {
        Mockito.when(esClusterPhyService.getClusterByName(Mockito.anyString())).thenReturn(new ClusterPhy());
        Mockito.when(esClusterPhyService.getClusterRacks(Mockito.anyString())).thenReturn(RackUtils.racks2Set(racks));
        Mockito.when(templatePhyService.getTemplateByRegionId(Mockito.anyLong())).thenReturn(Arrays.asList(new IndexTemplatePhy()));
    }

    @Test
    public void deleteRackByIdTest() {
        Assertions.assertFalse(regionRackService.deleteRackById(null));
    }

    @Test
    public void getRegionByIdTest() {
        Assertions.assertNull(regionRackService.getRegionById(null));
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Assertions.assertEquals(id, regionRackService.getRegionById(id).getId());
    }

    @Test
    public void listAllLogicClusterRacksTest() {
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Long logicClusterId = 1234l;
        Mockito.when(clusterLogicService.getClusterLogicById(Mockito.anyLong())).thenReturn(new ClusterLogic());
        regionRackService.bindRegion(id, logicClusterId, null, OPERATOR);
        Assertions.assertTrue(regionRackService
                .listAllLogicClusterRacks()
                .stream()
                .anyMatch(esClusterLogicRackInfo -> esClusterLogicRackInfo.getPhyClusterName().equals(clusterName)));
    }

    @Test
    public void listLogicClusterRacksTest1() {
        ESLogicClusterRackInfoDTO esLogicClusterRackInfoDTO = new ESLogicClusterRackInfoDTO();
        Assertions.assertTrue(regionRackService.listLogicClusterRacks(esLogicClusterRackInfoDTO).isEmpty());
        Long logicClusterId = 1234l;
        esLogicClusterRackInfoDTO.setLogicClusterId(logicClusterId);
        Assertions.assertTrue(regionRackService
                .listLogicClusterRacks(esLogicClusterRackInfoDTO)
                .stream()
                .anyMatch(esClusterLogicRackInfo -> esClusterLogicRackInfo.getLogicClusterId().equals(logicClusterId)));
    }

    @Test
    public void listLogicClusterRacksTest2() {
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Long logicClusterId = 1234l;
        Mockito.when(clusterLogicService.getClusterLogicById(Mockito.anyLong())).thenReturn(new ClusterLogic());
        regionRackService.bindRegion(id, logicClusterId, null, OPERATOR);
        Assertions.assertTrue(regionRackService
                .listLogicClusterRacks(logicClusterId)
                .stream()
                .anyMatch(esClusterLogicRackInfo -> esClusterLogicRackInfo.getPhyClusterName().equals(clusterName)));
    }

    @Test
    public void listAssignedRacksByClusterNameTest() {
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Long logicClusterId = 1234l;
        Mockito.when(clusterLogicService.getClusterLogicById(Mockito.anyLong())).thenReturn(new ClusterLogic());
        regionRackService.bindRegion(id, logicClusterId, null, OPERATOR);
        Assertions.assertTrue(regionRackService
                .listAssignedRacksByClusterName(clusterName)
                .stream()
                .anyMatch(esClusterLogicRackInfo -> esClusterLogicRackInfo.getPhyClusterName().equals(clusterName)));
    }

    @Test
    public void listPhysicClusterNamesTest() {
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Long logicClusterId = 1234l;
        Mockito.when(clusterLogicService.getClusterLogicById(Mockito.anyLong())).thenReturn(new ClusterLogic());
        regionRackService.bindRegion(id, logicClusterId, null, OPERATOR);
        Assertions.assertTrue(regionRackService
                .listPhysicClusterNames(logicClusterId)
                .stream()
                .anyMatch(s -> s.equals(clusterName)));
    }

    @Test
    public void listPhysicClusterIdTest() {
        ClusterPhy clusterPhy = CustomDataSource.esClusterPhyFactory();
        Mockito.when(esClusterPhyService.getClusterByName(Mockito.anyString())).thenReturn(
                clusterPhy);
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Long logicClusterId = 1234l;
        Mockito.when(clusterLogicService.getClusterLogicById(Mockito.anyLong())).thenReturn(new ClusterLogic());
        regionRackService.bindRegion(id, logicClusterId, null, OPERATOR);
        Assertions.assertTrue(regionRackService
                .listPhysicClusterId(logicClusterId)
                .stream()
                .anyMatch(integer -> integer.equals(clusterPhy.getId())));
    }

    @Test
    public void countRackMatchedRegionTest() {
        Assertions.assertEquals(0, regionRackService.countRackMatchedRegion(null, null));
        ClusterPhy clusterPhy = CustomDataSource.esClusterPhyFactory();
        Mockito.when(esClusterPhyService.getClusterByName(Mockito.anyString())).thenReturn(
                clusterPhy);
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Long logicClusterId = 1234l;
        Mockito.when(clusterLogicService.getClusterLogicById(Mockito.anyLong())).thenReturn(new ClusterLogic());
        regionRackService.bindRegion(id, logicClusterId, null, OPERATOR);
        int count = 1;
        Assertions.assertEquals(count, regionRackService.countRackMatchedRegion(clusterName,racks));
    }

    @Test
    public void listRegionsByLogicAndPhyClusterTest() {
        Assertions.assertTrue(regionRackService.listRegionsByLogicAndPhyCluster(null, null).isEmpty());
        ClusterPhy clusterPhy = CustomDataSource.esClusterPhyFactory();
        Mockito.when(esClusterPhyService.getClusterByName(Mockito.anyString())).thenReturn(
                clusterPhy);
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Long logicClusterId = 1234l;
        Mockito.when(clusterLogicService.getClusterLogicById(Mockito.anyLong())).thenReturn(new ClusterLogic());
        regionRackService.bindRegion(id, logicClusterId, null, OPERATOR);
        Assertions.assertTrue(regionRackService
                .listRegionsByLogicAndPhyCluster(logicClusterId, clusterName)
                .stream()
                .anyMatch(clusterRegion -> clusterRegion.getId().equals(id)));
    }

    @Test
    public void createPhyClusterRegionTest() {
        Assertions.assertEquals(Result.buildParamIllegal("物理集群名不能为空").getMessage(),
                regionRackService.createPhyClusterRegion(null, null, null, null).getMessage());
        Assertions.assertEquals(Result.buildParamIllegal(String.format("物理集群 %s 不存在", clusterName)).getMessage(),
                regionRackService.createPhyClusterRegion(clusterName, null, null, null).getMessage());
        Mockito.when(esClusterPhyService.getClusterByName(Mockito.anyString())).thenReturn(new ClusterPhy());
        Assertions.assertEquals(Result.buildParamIllegal("racks为空").getMessage(),
                regionRackService.createPhyClusterRegion(clusterName, null, null, null).getMessage());
        Mockito.when(esClusterPhyService.getClusterRacks(Mockito.anyString())).thenReturn(Sets.newHashSet("r1"));
        Assertions.assertTrue(regionRackService
				.createPhyClusterRegion(clusterName, racks, AdminConstant.YES, OPERATOR).failed());
        Mockito.when(esClusterPhyService.getClusterRacks(Mockito.anyString())).thenReturn(RackUtils.racks2Set(racks));
        Assertions.assertEquals(Result.buildParamIllegal("指定的share非法").getMessage(),
                regionRackService.createPhyClusterRegion(clusterName, racks, 123, OPERATOR).getMessage());
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Assertions.assertEquals(clusterName, clusterRegionDAO.getById(id).getPhyClusterName());
    }

    @Test
    public void createAndBindRegionTest() {
        Assertions.assertEquals(Result.buildParamIllegal("物理集群名不能为空").getMessage(),
                regionRackService.createAndBindRegion(null, null, null, null, OPERATOR).getMessage());
        Mockito.when(clusterLogicService.getClusterLogicById(Mockito.anyLong())).thenReturn(new ClusterLogic());
        Assertions.assertTrue(
				regionRackService.createAndBindRegion(clusterName, racks, 123l, null, OPERATOR).success());
    }

    @Test
    public void deletePhyClusterRegionTest() {
        Assertions.assertEquals(Result.buildFail("regionId为null").getMessage(),
                regionRackService.deletePhyClusterRegion(null, OPERATOR).getMessage());
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Assertions.assertEquals(Result.buildFail(String.format("region %d 不存在", id + 1)).getMessage(),
                regionRackService.deletePhyClusterRegion(id + 1, OPERATOR).getMessage());
        Assertions.assertTrue(regionRackService.deletePhyClusterRegion(id, OPERATOR).success());
        Assertions.assertNull(clusterRegionDAO.getById(id));
        id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        ;
        Long logicClusterId = 1234l;
        ClusterRegionDTO clusterRegionDTO = CustomDataSource.clusterRegionDTOFactory();
        clusterRegionDTO.setId(id);
        clusterRegionDTO.setLogicClusterId(logicClusterId);
        ClusterRegionPO clusterRegionPO = ConvertUtil.obj2Obj(clusterRegionDTO, ClusterRegionPO.class);
        clusterRegionDAO.update(clusterRegionPO);
        Assertions.assertEquals(Result.buildFail(String.format("region %d 已经被绑定到逻辑集群 %d", id, logicClusterId)).getMessage(),
                regionRackService.deletePhyClusterRegion(id, OPERATOR).getMessage());
    }

    @Test
    public void deletePhyClusterRegionWithoutCheckTest() {
        Assertions.assertEquals(Result.buildFail("regionId为null").getMessage(),
                regionRackService.deletePhyClusterRegionWithoutCheck(null, OPERATOR).getMessage());
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Assertions.assertEquals(Result.buildFail(String.format("region %d 不存在", id + 1)).getMessage(),
                regionRackService.deletePhyClusterRegionWithoutCheck(id + 1, OPERATOR).getMessage());
        Assertions.assertTrue(regionRackService.deletePhyClusterRegionWithoutCheck(id, OPERATOR).success());
        Assertions.assertNull(clusterRegionDAO.getById(id));
    }

    @Test
    public void bindRegionTest() {
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Assertions.assertEquals(Result.buildFail(String.format("region %d 不存在", id + 1)).getMessage(),
                regionRackService.bindRegion(id + 1, null, null, OPERATOR).getMessage());
        ClusterRegionDTO clusterRegionDTO = CustomDataSource.clusterRegionDTOFactory();
        clusterRegionDTO.setId(id);
        Long logicClusterId = 1234l;
        clusterRegionDTO.setLogicClusterId(logicClusterId);
        ClusterRegionPO clusterRegionPO = ConvertUtil.obj2Obj(clusterRegionDTO, ClusterRegionPO.class);
        clusterRegionDAO.update(clusterRegionPO);
        Assertions.assertEquals(Result.buildFail(String.format("region %d 已经被绑定", id)).getMessage(),
                regionRackService.bindRegion(id, null, null, OPERATOR).getMessage());
        clusterRegionDTO = CustomDataSource.clusterRegionDTOFactory();
        clusterRegionDTO.setId(id);
        clusterRegionPO = ConvertUtil.obj2Obj(clusterRegionDTO, ClusterRegionPO.class);
        clusterRegionDAO.update(clusterRegionPO);
        Mockito.when(clusterLogicService.getClusterLogicById(Mockito.anyLong())).thenReturn(null);
        Assertions.assertEquals(Result.buildFail(String.format("逻辑集群 %S 不存在", logicClusterId)).getMessage(),
                regionRackService.bindRegion(id, logicClusterId, null, OPERATOR).getMessage());
        Mockito.when(clusterLogicService.getClusterLogicById(Mockito.anyLong())).thenReturn(new ClusterLogic());
        Assertions.assertEquals(Result.buildParamIllegal("指定的share非法").getMessage(),
                regionRackService.bindRegion(id, logicClusterId, 123, OPERATOR).getMessage());
        Assertions.assertTrue(regionRackService.bindRegion(id, logicClusterId, null, OPERATOR).success());
    }

    @Test
    public void editRegionRacksTest() {
        Assertions.assertEquals(Result.buildFail("未指定regionId").getMessage(),
                regionRackService.editRegionRacks(null, null, OPERATOR).getMessage());
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Assertions.assertEquals(Result.buildFail(String.format("region %d 不存在", id + 1)).getMessage(),
                regionRackService.editRegionRacks(id + 1, racks, OPERATOR).getMessage());
        Assertions.assertTrue(regionRackService.editRegionRacks(id, racks, OPERATOR).success());
    }

    @Test
    public void unbindRegionTest() {
        Assertions.assertEquals(Result.buildFail("未指定regionId").getMessage(),
                regionRackService.unbindRegion(null, OPERATOR).getMessage());
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Assertions.assertEquals(Result.buildFail(String.format("region %d 不存在", id + 1)).getMessage(),
                regionRackService.unbindRegion(id + 1, OPERATOR).getMessage());
        Assertions.assertEquals(Result.buildFail(String.format("region %d 未被绑定", id)).getMessage(),
                regionRackService.unbindRegion(id, OPERATOR).getMessage());
        ClusterRegionDTO clusterRegionDTO = CustomDataSource.clusterRegionDTOFactory();
        clusterRegionDTO.setId(id);
        Long logicClusterId = 1234l;
        clusterRegionDTO.setLogicClusterId(logicClusterId);
        ClusterRegionPO clusterRegionPO = ConvertUtil.obj2Obj(clusterRegionDTO, ClusterRegionPO.class);
        clusterRegionDAO.update(clusterRegionPO);
        Assertions.assertEquals(Result.buildFail(String.format("region %d 上已经分配模板", id)).getMessage(),
                regionRackService.unbindRegion(id, OPERATOR).getMessage());
        Mockito.when(templatePhyService.getTemplateByRegionId(Mockito.anyLong())).thenReturn(null);
        Assertions.assertTrue(regionRackService.unbindRegion(id, OPERATOR).success());
        Assertions.assertEquals(AdminConstant.REGION_NOT_BOUND_LOGIC_CLUSTER_ID,
                clusterRegionDAO.getById(id).getLogicClusterId());
    }

    @Test
    public void listLogicClusterRegionsTest() {
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Long logicClusterId = 1234l;
        Assertions.assertTrue(regionRackService.listLogicClusterRegions(logicClusterId).isEmpty());
        Mockito.when(clusterLogicService.getClusterLogicById(Mockito.anyLong())).thenReturn(new ClusterLogic());
        regionRackService.bindRegion(id, logicClusterId, null, OPERATOR);
        Assertions.assertTrue(regionRackService
                .listLogicClusterRegions(logicClusterId)
                .stream()
                .anyMatch(clusterRegion -> clusterRegion.getId().equals(id)));
    }

    @Test
    public void listRegionsByClusterNameTest() {
        Assertions.assertTrue(regionRackService.listRegionsByClusterName(clusterName).isEmpty());
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Long logicClusterId = 1234l;
        Assertions.assertTrue(regionRackService.listLogicClusterRegions(logicClusterId).isEmpty());
        Mockito.when(clusterLogicService.getClusterLogicById(Mockito.anyLong())).thenReturn(new ClusterLogic());
        regionRackService.bindRegion(id, logicClusterId, null, OPERATOR);
        Assertions.assertTrue(regionRackService
                .listRegionsByClusterName(clusterName)
                .stream()
                .anyMatch(clusterRegion -> clusterRegion.getId().equals(id)));
    }

    /**
     * 一个集群上应该是可能有着多个物理集群的region
     * FIXME
     */
    @Test
    public void getLogicClusterIdByPhyClusterIdTest() {
        Integer phyClusterId = 123;
        Assertions.assertNull(regionRackService.getLogicClusterIdByPhyClusterId(phyClusterId));
        Mockito.when(esClusterPhyService.getClusterById(Mockito.anyInt())).thenReturn(new ClusterPhy());
        Assertions.assertNull(regionRackService.getLogicClusterIdByPhyClusterId(phyClusterId));
        Long id = regionRackService.createPhyClusterRegion(clusterName, racks, null, OPERATOR).getData();
        Long logicClusterId = 1234l;
        Assertions.assertTrue(regionRackService.listLogicClusterRegions(logicClusterId).isEmpty());
        Mockito.when(clusterLogicService.getClusterLogicById(Mockito.anyLong())).thenReturn(new ClusterLogic());
        regionRackService.bindRegion(id, logicClusterId, null, OPERATOR);
    }

}
