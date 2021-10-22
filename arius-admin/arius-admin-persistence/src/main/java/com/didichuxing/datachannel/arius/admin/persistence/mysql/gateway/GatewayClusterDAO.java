package com.didichuxing.datachannel.arius.admin.persistence.mysql.gateway;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.didichuxing.datachannel.arius.admin.common.bean.po.gateway.GatewayClusterPO;


/**
 * @author d06679
 */
@Repository
public interface GatewayClusterDAO {

    int insert(GatewayClusterPO param);

    List<GatewayClusterPO> listAll();

}
