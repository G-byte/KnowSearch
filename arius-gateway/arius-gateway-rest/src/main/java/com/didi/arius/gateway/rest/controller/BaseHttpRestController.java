package com.didi.arius.gateway.rest.controller;

import com.didi.arius.gateway.common.consts.QueryConsts;
import com.didi.arius.gateway.common.consts.RestConsts;
import com.didi.arius.gateway.common.exception.QueryDslLengthException;
import com.didi.arius.gateway.common.metadata.JoinLogContext;
import com.didi.arius.gateway.common.metadata.QueryContext;
import com.didi.arius.gateway.common.utils.Convert;
import com.didi.arius.gateway.core.component.QueryConfig;
import com.didi.arius.gateway.core.es.http.RestActionListenerImpl;
import com.didi.arius.gateway.core.service.ESRestClientService;
import com.didi.arius.gateway.core.service.RateLimitService;
import com.didi.arius.gateway.core.service.RequestStatsService;
import com.didi.arius.gateway.core.service.arius.AppService;
import com.didi.arius.gateway.core.service.arius.DynamicConfigService;
import com.didi.arius.gateway.core.service.arius.ESClusterService;
import com.didi.arius.gateway.core.service.arius.IndexTemplateService;
import com.didi.arius.gateway.elasticsearch.client.ESClient;
import com.didi.arius.gateway.elasticsearch.client.gateway.direct.DirectRequest;
import com.didi.arius.gateway.elasticsearch.client.gateway.direct.DirectResponse;
import com.didi.arius.gateway.elasticsearch.client.gateway.search.ESSearchResponse;
import com.didi.arius.gateway.elasticsearch.client.model.ESActionRequest;
import com.didi.arius.gateway.rest.http.IRestHandler;
import com.didi.arius.gateway.rest.http.RestController;
import com.didiglobal.logi.log.LogGather;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.support.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class BaseHttpRestController implements IRestHandler {

    protected static final Logger logger = LoggerFactory.getLogger( BaseHttpRestController.class);
    protected static final Logger statLogger = LoggerFactory.getLogger(QueryConsts.STAT_LOGGER);
    protected static final Logger traceLogger = LoggerFactory.getLogger(QueryConsts.TRACE_LOGGER);
    protected static final Logger auditLogger = LoggerFactory.getLogger(QueryConsts.AUDIT_LOGGER);
    protected static final String AUTHORIZATION = "Authorization";

    @Autowired
    protected DynamicConfigService dynamicConfigService;

    @Autowired
    protected IndexTemplateService indexTemplateService;

    @Autowired
    protected RequestStatsService requestStatsService;

    @Autowired
    protected ESClusterService esClusterService;

    @Autowired
    protected AppService appService;

    @Autowired
    protected ESRestClientService esRestClientService;

    @Autowired
    protected QueryConfig queryConfig;

    @Autowired
    protected RestController controller;

    @Autowired
    protected RateLimitService rateLimitService;

    protected String actionName = this.getClass().getSimpleName();

    @PostConstruct
    public void init(){
        register();
    }

    @Override
    public void dispatchRequest(RestRequest request, RestChannel channel) {
        QueryContext queryContext = parseContext(request, channel);

        try {

            checkToken(queryContext);

            preRequest(queryContext);

            handleRequest(queryContext);

            postRequest(queryContext);
        } catch (Exception ex) {

            preException(queryContext, ex);

            try {
                channel.sendResponse(new BytesRestResponse(channel, ex));
            } catch (IOException ioe) {
                BytesRestResponse response = new BytesRestResponse( RestStatus.INTERNAL_SERVER_ERROR);
                channel.sendResponse(response);
            }
        }
    }

    /************************************************************** abstract method **************************************************************/
    /**
     *
     */
    protected abstract void register();

    /**
     *
     * @return
     */
    protected abstract String name();

    /**
     *
     * @param queryContext
     * @throws Exception
     */
    protected abstract void handleRequest(QueryContext queryContext) throws Exception;

    /************************************************************** protected method **************************************************************/
    /**
     *
     * @param queryContext
     * @param restResponse
     */
    protected void sendDirectResponse(QueryContext queryContext, RestResponse restResponse) {
        RestActionListenerImpl<ESSearchResponse> listener = new RestActionListenerImpl<>(queryContext);
        listener.onResponse(restResponse);
    }

    protected void checkIndices(QueryContext queryContext) {
        List<String> indices = queryContext.getIndices();
        appService.checkIndices(queryContext, indices);
    }

    protected void checkToken(QueryContext queryContext) {
        appService.checkToken(queryContext);
        String encode = Base64.getEncoder().encodeToString(String.format("%s", "user_" + queryContext.getAppid() + ":" + queryContext.getAppDetail().getVerifyCode()).getBytes( StandardCharsets.UTF_8));
        queryContext.getRequest().putHeader(AUTHORIZATION, "Basic " + encode);
    }

    protected void preRequest(QueryContext queryContext) {
        if (queryContext.getPostBody() != null && queryContext.getPostBody().length() > queryConfig.getDslMaxLength()) {
            throw new QueryDslLengthException(String.format("query length(%d) > %d exception", queryContext.getPostBody().length(), queryConfig.getDslMaxLength()));
        }

        queryContext.setRequestTime(System.currentTimeMillis());

        if (queryContext.isDetailLog()) {
            JoinLogContext joinLogContext = queryContext.getJoinLogContext();
            joinLogContext.setAppid(queryContext.getAppid());
            joinLogContext.setRequestId(queryContext.getRequestId());
            joinLogContext.setRequestType(queryContext.getAppDetail().getSearchType());
            joinLogContext.setMethod(queryContext.getMethod());
            joinLogContext.setClusterId(queryContext.getClusterId());
            joinLogContext.setUser(queryContext.getUser());
            joinLogContext.setUri(queryContext.getUri());
            joinLogContext.setQueryString(queryContext.getQueryString());
            joinLogContext.setRemoteAddr(queryContext.getRemoteAddr());
            joinLogContext.setDslLen(queryContext.getPostBody().length());
            joinLogContext.setDsl(queryContext.getPostBody().replaceAll("\\n", " "));
            joinLogContext.setTimeStamp(System.currentTimeMillis());
            traceLogger.info("_com_request_in||traceid={}||spanid={}||type=http||appid={}||requestId={}||uri={}||remoteAddr={}||requestLen={}||name={}",
                    queryContext.getTraceid(), queryContext.getSpanid(), queryContext.getAppid(), queryContext.getRequestId(), queryContext.getUri(), queryContext.getRemoteAddr(), queryContext.getPostBody().length(), name());
        } else {
            LogGather.recordInfoLog(QueryConsts.DLFLAG_PREFIX + "query_request_" + queryContext.getAppid() + "_" + name(), String.format("requestId=%s||method=%s||uri=%s||queryString=%s||remoteAddr=%s||postBodyLen=%d",
                    queryContext.getRequestId(), queryContext.getMethod(), queryContext.getUri(), queryContext.getQueryString(), queryContext.getRemoteAddr(), queryContext.getPostBody().length()));
        }

        if (queryContext.getXUserName() != null) {
            auditLogger.info("auditlog||system=arius||hostIp=127.0.0.1||userName={}||url={}||getParams={}||postParams={}||userIp={}||timestamp=||respose=",
                    queryContext.getXUserName(), queryContext.getUri(), queryContext.getQueryString(), queryContext.getPostBody(), queryContext.getRemoteAddr());
        }

        rateLimitService.addByteIn(queryContext.getPostBody().length());
    }

    protected void preException(QueryContext queryContext, Throwable e) {
        if (queryContext.isDetailLog()) {
            JoinLogContext joinLogContext = queryContext.getJoinLogContext();
            joinLogContext.setAriusType("error");
            joinLogContext.setExceptionName(e.getClass().getName());
            joinLogContext.setStack(Convert.logExceptionStack(e));
            joinLogContext.setTotalCost(System.currentTimeMillis() - queryContext.getRequestTime());
            joinLogContext.setInternalCost( joinLogContext.getTotalCost() - joinLogContext.getEsCost());
            joinLogContext.setSinkTime(System.currentTimeMillis());

            String log = joinLogContext.toString();
            statLogger.error(log);

            traceLogger.info("_com_request_out||traceid={}||spanid={}||type=http||appid={}||requestId={}||errname={}",
                    queryContext.getTraceid(), queryContext.getSpanid(), queryContext.getAppid(), queryContext.getRequestId(), e.getClass().getName());
        }

        LogGather.recordErrorLog(e.getClass().getName() + "_" + queryContext.getAppid(), String.format("http_exception||requestId=%s||appid=%d||uri=%s||postBody=%s",
                queryContext.getRequestId(), queryContext.getAppid(), queryContext.getUri(), queryContext.getPostBody()), e);

        requestStatsService.removeQueryContext(queryContext.getRequestId());

        rateLimitService.removeByteIn(queryContext.getPostBody().length());
    }

    protected void postRequest(QueryContext queryContext) {
        int appid = queryContext.getAppDetail() != null ? queryContext.getAppDetail().getId() : QueryConsts.TOTAL_APPID_ID;
        requestStatsService.statsAdd(name(), appid, queryContext.getSearchId(), queryContext.getCostTime(), RestStatus.OK);

        String searchId = queryContext.getSearchId() != null ? queryContext.getSearchId() : QueryConsts.TOTAL_SEARCH_ID;
        try {
            rateLimitService.addUp(appid, searchId, 0, 0);
        } catch (Exception e) {
            logger.warn("rateLimitService.addUp exception", e);
        }

    }

    protected void directRequest(ESClient client, QueryContext queryContext, RestActionListenerImpl<DirectResponse> listener) {
        String uri = queryContext.getUri();
        String queryString = queryContext.getQueryString() == null ? "" : queryContext.getQueryString();

        Map<String, String> params = new HashMap<>();
        RestUtils.decodeQueryString(queryString, 0, params);

        DirectRequest directRequest = new DirectRequest(queryContext.getMethod().toString(), uri);
        setSocketTimeout(params, directRequest);
        directRequest.setPostContent(queryContext.getPostBody());
        directRequest.setParams(params);

        directRequest.putHeader(AUTHORIZATION, queryContext.getRequest().getHeader(AUTHORIZATION));
        directRequest.putHeader("requestId", queryContext.getRequestId());

        client.direct(directRequest, listener);
    }

    protected void directRequest(ESClient client, QueryContext queryContext) {
        RestActionListenerImpl<DirectResponse> listener = new RestActionListenerImpl<>(queryContext);
        directRequest(client, queryContext, listener);
    }

    /************************************************************** private method **************************************************************/
    private QueryContext parseContext(RestRequest request, RestChannel channel) {
        QueryContext context = new QueryContext();
        context.setRequestTime(System.currentTimeMillis());
        context.setRestName(name());
        context.setJoinLogContext(new JoinLogContext());

        String searchId = request.header( QueryConsts.HEAD_SEARCH_ID);
        String clusterId = request.header(QueryConsts.HEAD_CLUSTER_ID);
        String user = request.header(QueryConsts.HEAD_USER);
        String authentication = request.header(QueryConsts.HEAD_AUTHORIZATION);
        String xUserName = request.header(QueryConsts.HEAD_USERNAME);
        String ssoUserName = request.header(QueryConsts.HEAD_SSO_USERNAME);
        String clientVersion = request.header(QueryConsts.HEAD_CLIENT_VERSION);

        String traceid = request.header(QueryConsts.TRACE_ID);
        String spanid = request.header(QueryConsts.SPAN_ID);

        if (traceid == null) {
            traceid = "";
        }

        if (spanid == null) {
            spanid = "";
        }

        String postBody = request.content().toUtf8();

        if (postBody == null) {
            postBody = "";
        }

        String remoteAddr = Convert.getClientIP(request);

        if (searchId == null) {
            searchId = QueryConsts.TOTAL_SEARCH_ID;
        }

        if (clusterId == null) {
            clusterId = request.param(QueryConsts.GET_CLUSTER_ID);
            request.params().remove(QueryConsts.GET_CLUSTER_ID);
        }

        context.setRequestId( UUID.randomUUID().toString());
        context.setMethod(request.method());
        context.setSearchId(searchId);
        context.setClusterId(clusterId);
        context.setUser(user);
        context.setClientVersion(clientVersion);

        String kibanaVersion = request.header(QueryConsts.HEAD_KIBANA_VERSION);
        context.setFromKibana(kibanaVersion != null);
        context.setNewKibana(kibanaVersion != null && kibanaVersion.startsWith(QueryConsts.NEW_KIBANA_VERSION_START));

        String uri = request.uri();
        int pathEndPos = uri.indexOf('?');
        if (pathEndPos > 0 && pathEndPos < uri.length())  {
            context.setQueryString(uri.substring(pathEndPos+1));
        } else {
            context.setQueryString("");
        }

        context.setPostBody(postBody);
        context.setRemoteAddr(remoteAddr);
        context.setAuthentication(authentication);

        if (user != null) {
            context.setXUserName(user);
        } else if (xUserName != null) {
            context.setXUserName(xUserName);
        } else if (ssoUserName != null) {
            context.setXUserName(ssoUserName);
        }

        context.setTraceid(traceid);
        context.setSpanid(spanid);

        context.setRequest(request);
        context.setChannel(channel);

        context.setSemaphore(queryConfig.getHttpSemaphore());
        context.setRequestSlowlogThresholdMills(queryConfig.getRequestSlowlogThresholdMills());
        context.setMaxHttpResponseLength(queryConfig.getMaxHttpResponseLength());

        requestStatsService.putQueryContext(context.getRequestId(), context);

        context.setTypedKeys(request.paramAsBoolean("typed_keys", false));

        if (dynamicConfigService.getDetailLogFlag()
                || context.isFromKibana()) {
            context.setDetailLog(true);
//        } else if (this instanceof RestBaseWriteAction) {
//            context.setDetailLog(false);
        } else {
            context.setDetailLog(true);
        }

        return context;
    }

    private void setSocketTimeout(Map<String, String> params, ESActionRequest request) {
        if (params.containsKey(RestConsts.SOCKET_TIMEOUT_PARAMS)) {
            String strSocketTimeout = params.remove(RestConsts.SOCKET_TIMEOUT_PARAMS);
            try {
                int socketTimeout = Integer.parseInt(strSocketTimeout);
                if (socketTimeout > 0 && socketTimeout <= QueryConsts.MAX_SOCKET_TIMEOUT) {
                    request.setSocketTimeout(socketTimeout);
                }
            } catch (Exception e) {
                // pass
            }
        }
    }
}
