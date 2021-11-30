package com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.setting;

import com.alibaba.fastjson.JSON;
import com.didiglobal.logi.elasticsearch.client.model.ESActionRequest;
import com.didiglobal.logi.elasticsearch.client.model.ESActionResponse;
import com.didiglobal.logi.elasticsearch.client.model.RestRequest;
import com.didiglobal.logi.elasticsearch.client.model.RestResponse;
import org.elasticsearch.action.ActionRequestValidationException;

public class ESClusterGetSettingsAllRequest extends ESActionRequest<ESClusterGetSettingsAllRequest> {
    @Override
    public ActionRequestValidationException validate() {
        return null;
    }


    @Override
    public RestRequest toRequest() throws Exception {
        return new RestRequest("GET", "/_cluster/settings?include_defaults=true&pretty", null);
    }

    @Override
    public ESActionResponse toResponse(RestResponse response) throws Exception {
        String respStr = response.getResponseContent();
        return JSON.parseObject(respStr, ESClusterGetSettingsAllResponse.class);
    }
}
