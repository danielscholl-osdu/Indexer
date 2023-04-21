/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.service;

import com.google.api.client.util.Strings;
import org.apache.http.HttpStatus;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.indexer.model.IndexAliasesResult;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class IndexAliasServiceImpl implements IndexAliasService{
    private static final String KIND_COMPLETE_VERSION_PATTERN = "[\\w-\\.\\*]+:[\\w-\\.\\*]+:[\\w-\\.\\*]+:(\\d+\\.\\d+\\.\\d+)$";

    @Inject
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Inject
    private ElasticClientHandler elasticClientHandler;
    @Inject
    private JaxRsDpsLog jaxRsDpsLog;

    @Override
    public IndexAliasesResult createIndexAliasesForAll() {
        IndexAliasesResult result = new IndexAliasesResult();
        try (RestHighLevelClient restClient = this.elasticClientHandler.createRestClient()) {
            List<String> allKinds = getAllKinds(restClient);
            Set<String> allExistingAliases = getAllExistingAliases(restClient);
            for (String kind : allKinds) {
                String alias = elasticIndexNameResolver.getIndexAliasFromKind(kind);
                String indexName = elasticIndexNameResolver.getIndexNameFromKind(kind);
                if(allExistingAliases.contains(alias)) {
                    result.getIndicesWithAliases().add(indexName);
                }
                else {
                    if(createIndexAlias(restClient, kind)) {
                        result.getIndicesWithAliases().add(indexName);
                    }
                    else {
                        result.getIndicesWithoutAliases().add(indexName);
                    }
                }
            }
        }
        catch (Exception e) {
            jaxRsDpsLog.error("elastic search request failed", e);
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "elastic search cannot respond", "an unknown error has occurred.", e);
        }

        return result;
    }

    @Override
    public boolean createIndexAlias(RestHighLevelClient restClient, String kind) {
        if(!elasticIndexNameResolver.isIndexAliasSupported(kind)) {
            return false;
        }

        try {
            // To create an alias for an index, the index name must the concrete index name, not alias
            String actualIndexName = resolveConcreteIndexName(restClient, kind);
            if(Strings.isNullOrEmpty(actualIndexName))
                return false;

            Map<String, String> indexAliasMap = new HashMap<>();
            indexAliasMap.put(actualIndexName, elasticIndexNameResolver.getIndexAliasFromKind(kind));
            String kindWithMajorVersion = getKindWithMajorVersion(kind);
            if(elasticIndexNameResolver.isIndexAliasSupported(kindWithMajorVersion)) {
                String index = elasticIndexNameResolver.getIndexNameFromKind(kindWithMajorVersion);
                String alias = elasticIndexNameResolver.getIndexAliasFromKind(kindWithMajorVersion);
                indexAliasMap.put(index, alias);
            }

            boolean ok = true;
            for (Map.Entry<String, String> entry: indexAliasMap.entrySet()) {
                IndicesAliasesRequest addRequest = new IndicesAliasesRequest();
                IndicesAliasesRequest.AliasActions aliasActions = new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                        .index(entry.getKey())
                        .alias(entry.getValue());
                addRequest.addAliasAction(aliasActions);
                AcknowledgedResponse response = restClient.indices().updateAliases(addRequest, RequestOptions.DEFAULT);
                ok &= response.isAcknowledged();
            }
            return ok;
        }
        catch(Exception e) {
            jaxRsDpsLog.error(String.format("Fail to create index alias for kind '%s'", kind), e);
        }

        return false;
    }

    private Set<String> getAllExistingAliases(RestHighLevelClient restClient) throws IOException {
        GetAliasesRequest request = new GetAliasesRequest();
        GetAliasesResponse response = restClient.indices().getAlias(request, RequestOptions.DEFAULT);
        if(response.status() != RestStatus.OK)
            return new HashSet<>();

        Set<String> allAliases = new HashSet<>();
        for (Set<AliasMetadata> aliasSet: response.getAliases().values()) {
            List<String> aliases = aliasSet.stream().map(a -> a.getAlias()).collect(Collectors.toList());
            allAliases.addAll(aliases);
        }
        return allAliases;
    }

    private String getKindWithMajorVersion(String kind) {
        // If kind is common:welldb:wellbore:1.2.0, then kind with major version is common:welldb:wellbore:1.*.*
        int idx = kind.lastIndexOf(":");
        String version = kind.substring(idx+1);
        if(version.indexOf(".") > 0) {
            String kindWithoutVersion = kind.substring(0, idx);
            String majorVersion = version.substring(0, version.indexOf("."));
            return String.format("%s:%s.*.*", kindWithoutVersion, majorVersion);
        }
        return null;
    }

    private String resolveConcreteIndexName(RestHighLevelClient restClient, String kind) throws IOException {
        String index = elasticIndexNameResolver.getIndexNameFromKind(kind);
        if(!isCompleteVersionKind(kind)) {
            return index;
        }

        GetAliasesRequest request = new GetAliasesRequest(index);
        GetAliasesResponse response = restClient.indices().getAlias(request, RequestOptions.DEFAULT);
        if(response.status() == RestStatus.NOT_FOUND) {
            /* index resolved from kind is actual concrete index
             * Example:
             * {
             *   "opendes-wke-well-1.0.7": {
             *       "aliases": {}
             *   }
             * }
             */
            return index;
        }
        if(response.status() == RestStatus.OK) {
            /* index resolved from kind is NOT actual create index. It is just an alias
             * The concrete index name in this example is "opendes-osdudemo-wellbore-1.0.0_1649167113090"
             * Example:
             * {
             *   "opendes-osdudemo-wellbore-1.0.0_1649167113090": {
             *       "aliases": {
             *           "opendes-osdudemo-wellbore-1.0.0": {}
             *       }
             *    }
             * }
             */
            Map<String, Set<AliasMetadata>> aliases = response.getAliases();
            for (Map.Entry<String, Set<AliasMetadata>> entry: aliases.entrySet()) {
                String actualIndex = entry.getKey();
                List<String> aliaseNames = entry.getValue().stream().map(a -> a.getAlias()).collect(Collectors.toList());
                if(aliaseNames.contains(index))
                    return actualIndex;
            }
        }
        return index;
    }

    private boolean isCompleteVersionKind(String kind) {
        return !Strings.isNullOrEmpty(kind) && kind.matches(KIND_COMPLETE_VERSION_PATTERN);
    }

    private List<String> getAllKinds(RestHighLevelClient client) throws IOException {
        List<String> kinds;
        SearchRequest elasticSearchRequest = new SearchRequest("_all");
        TermsAggregationBuilder termsAggregationBuilder = new TermsAggregationBuilder("kinds");
        termsAggregationBuilder.field("kind");
        termsAggregationBuilder.size(10000);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.aggregation(termsAggregationBuilder);
        elasticSearchRequest.source(sourceBuilder);
        SearchResponse searchResponse = client.search(elasticSearchRequest, RequestOptions.DEFAULT);
        Terms kindBuckets = searchResponse.getAggregations().get("kinds");
        kinds = kindBuckets.getBuckets().stream().map(bucket -> bucket.getKey().toString()).collect(Collectors.toList());
        return kinds;
    }
}
