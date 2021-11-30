/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.didi.arius.gateway.core.es.http.search;

import com.didi.arius.gateway.common.exception.InvalidParameterException;
import com.didi.arius.gateway.common.metadata.FetchFields;
import com.didi.arius.gateway.common.metadata.JoinLogContext;
import com.didi.arius.gateway.common.metadata.QueryContext;
import com.didi.arius.gateway.core.es.http.ESAction;
import com.didi.arius.gateway.elasticsearch.client.ESClient;
import com.didi.arius.gateway.elasticsearch.client.gateway.search.ESSearchResponse;
import com.didi.arius.gateway.elasticsearch.client.gateway.search.ESSearchScrollRequest;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.support.RestActions;
import org.elasticsearch.search.Scroll;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;

import static com.didi.arius.gateway.common.consts.RestConsts.SCROLL;
import static com.didi.arius.gateway.common.consts.RestConsts.SCROLL_SPLIT;
import static com.didi.arius.gateway.common.utils.CommonUtil.isIndexType;
import static org.elasticsearch.common.unit.TimeValue.parseTimeValue;

/**
 *
 */
@Component("restSearchScrollAction")
public class RestSearchScrollAction extends ESAction {

	@Override
	public String name() {
		return "searchScroll";
	}

    @Override
    public void handleInterRequest(QueryContext queryContext, RestRequest request, RestChannel channel) throws Exception {
        long start = System.currentTimeMillis();

        String scrollId = request.param("scroll_id");
        SearchScrollRequest searchScrollRequest = new SearchScrollRequest();
        searchScrollRequest.scrollId(scrollId);
        String scroll = request.param(SCROLL);
        if (scroll != null) {
            searchScrollRequest.scroll(new Scroll(parseTimeValue(scroll, null, SCROLL)));
        }

        if (RestActions.hasBodyContent(request)) {
            XContentType type = XContentFactory.xContentType(RestActions.getRestContent(request));
            if (type == null) {
                if (scrollId == null) {
                    scrollId = RestActions.getRestContent(request).toUtf8();
                    searchScrollRequest.scrollId(scrollId);
                }
            } else {
                // NOTE: if rest request with xcontent body has request parameters, these parameters override xcontent values
                buildFromContent(RestActions.getRestContent(request), searchScrollRequest);
            }
        }

        long paramTime = System.currentTimeMillis();

        ESClient readClient;
        String realScrollId;
        if (isIndexType(queryContext)) {
            String scrollIdWrap = searchScrollRequest.scrollId();

            int pos = scrollIdWrap.indexOf(SCROLL_SPLIT);
            if (pos <= 0) {
                throw new InvalidParameterException("scrollId format error, scrollId=" +  scrollIdWrap);
            }

            String clusterEncode = scrollIdWrap.substring(0, pos);
            byte[] bytes = Base64.getUrlDecoder().decode(clusterEncode);
            String cluster = new String(bytes);

            realScrollId = scrollIdWrap.substring(pos+1);
            readClient = esClusterService.getClientFromCluster(queryContext, cluster, actionName);
        } else {
            realScrollId = searchScrollRequest.scrollId();
            readClient = esClusterService.getClient(queryContext, actionName);
        }

        long getClientTime = System.currentTimeMillis();

        ESSearchScrollRequest esSearchScrollRequest = new ESSearchScrollRequest();
        esSearchScrollRequest.setScrollId(realScrollId);
        if (searchScrollRequest.scroll() != null) {
            esSearchScrollRequest.scroll(searchScrollRequest.scroll().keepAlive());
        }

        esSearchScrollRequest.putHeader("requestId", queryContext.getRequestId());
        esSearchScrollRequest.putHeader("Authorization", queryContext.getRequest().getHeader("Authorization"));

        FetchFields fetchFields = new FetchFields();
        fetchFields.setHasMessageField(false);
        queryContext.setFetchFields(fetchFields);

        ActionListener<ESSearchResponse> listener = newSearchListener(queryContext);
        readClient.searchScroll(esSearchScrollRequest, listener);

        JoinLogContext joinLogContext = queryContext.getJoinLogContext();
        joinLogContext.setParamCost(paramTime - start);
        joinLogContext.setGetClientCost(getClientTime - paramTime);
    }

    public static void buildFromContent(BytesReference content, SearchScrollRequest searchScrollRequest) {
        try (XContentParser parser = XContentHelper.createParser(content)) {
            if (parser.nextToken() != XContentParser.Token.START_OBJECT) {
                throw new IllegalArgumentException("Malforrmed content, must start with an object");
            } else {
                XContentParser.Token token;
                String currentFieldName = null;
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                    } else if ("scroll_id".equals(currentFieldName) && token == XContentParser.Token.VALUE_STRING) {
                        searchScrollRequest.scrollId(parser.text());
                    } else if (SCROLL.equals(currentFieldName) && token == XContentParser.Token.VALUE_STRING) {
                        searchScrollRequest.scroll(new Scroll(TimeValue.parseTimeValue(parser.text(), null, SCROLL)));
                    } else {
                        throw new IllegalArgumentException("Unknown parameter [" + currentFieldName + "] in request body or parameter is of the wrong type[" + token + "] ");
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse request body", e);
        }
    }
}
