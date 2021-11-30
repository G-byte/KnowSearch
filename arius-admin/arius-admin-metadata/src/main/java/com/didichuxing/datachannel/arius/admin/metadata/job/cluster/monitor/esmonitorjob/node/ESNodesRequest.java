package com.didichuxing.datachannel.arius.admin.metadata.job.cluster.monitor.esmonitorjob.node;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.didiglobal.logi.elasticsearch.client.model.ESActionResponse;
import com.didiglobal.logi.elasticsearch.client.model.RestRequest;
import com.didiglobal.logi.elasticsearch.client.model.RestResponse;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.ActionRequestValidationException;

import java.util.HashSet;
import java.util.Set;

public class ESNodesRequest extends BaseTimeoutRequest<ESNodesRequest> {
    private String nodeIds;
    private Set<String> flags = new HashSet<>();

    public ESNodesRequest clear() {
        flags.clear();
        return this;
    }

    public ESNodesRequest flag(String name) {
        flags.add(name);
        return this;
    }

    public ESNodesRequest nodeIds(String nodeIds) {
        this.nodeIds = nodeIds;
        return this;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    @Override
    public RestRequest toRequest() throws Exception {
        String endpoint = buildEndPoint();
        RestRequest restRequest = new RestRequest("GET", endpoint, null);
        //加上超时时间
        restRequest.getParams().put("timeout", timeout);
        return restRequest;
    }

    @Override
    public ESActionResponse toResponse(RestResponse response) throws Exception {
        String respStr = response.getResponseContent();
        JSONObject obj = JSON.parseObject(respStr);
        Object nodes = obj.remove("_nodes");
        ESNodesResponse esNodesResponse = JSON.parseObject(obj.toJSONString(), ESNodesResponse.class);
        if (null != nodes) {
            esNodesResponse.setFailedNodes(((JSONObject) nodes).getInteger("failed"));
        }
        return JSON.parseObject(obj.toJSONString(), ESNodesResponse.class);
    }

    private String buildEndPoint() {
        String flagStr = flags.size() < 10 ? StringUtils.join(flags, ",").trim() : null;
        String nodeUrl = null == nodeIds ? "_nodes" : String.format("_nodes/%s", nodeIds);
        String finalUrl = null == flagStr ? nodeUrl : nodeUrl + "/" + flagStr;
        return finalUrl;
    }
}
