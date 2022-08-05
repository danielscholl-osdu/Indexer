// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexer.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpStatus;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.unit.TimeValue;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequestScope
public abstract class MappingServiceImpl implements IMappingService {

    @Autowired
    private IndicesService indicesService;
    @Autowired
    private ElasticClientHandler elasticClientHandler;
    @Autowired
    private ElasticIndexNameResolver elasticIndexNameResolver;

    private static TimeValue REQUEST_TIMEOUT = TimeValue.timeValueMinutes(1);

    /*
     * Get index schema
     *
     * @param index Index name
     * @param requestHeaders Incoming request headers
     * @throws Exception Throws exception if elastic cannot find index.
     * */
    @Override
    public String getIndexSchema(String index) throws Exception {

        try (RestHighLevelClient client = this.elasticClientHandler.createRestClient()) {
            return this.getIndexMapping(client, index);
        }
    }

    /**
     * Gets elastic mapping for index
     *
     * @param client Elasticsearch client
     * @param index  Index name
     * @return mapping Index mapping
     * @throws Exception Throws exception if elastic cannot find index.
     */
    public String getIndexMapping(RestHighLevelClient client, String index) throws Exception {

        Preconditions.checkArgument(client, Objects::nonNull, "client cannot be null");
        Preconditions.checkArgument(index, Objects::nonNull, "index cannot be null");

        // check if index exist
        boolean indexExist = indicesService.isIndexExist(client, index);
        if (!indexExist) {
            throw new AppException(HttpStatus.SC_NOT_FOUND, "Kind not found", String.format("Kind %s not found", this.elasticIndexNameResolver.getKindFromIndexName(index)));
        }

        try {
            GetMappingsRequest request = new GetMappingsRequest();
            request.indices(index);
            request.setTimeout(REQUEST_TIMEOUT);
            GetMappingsResponse response = client.indices().getMapping(request, RequestOptions.DEFAULT);
            if (response.mappings().isEmpty()) {
                throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unknown error", String.format("Error retrieving mapping for kind %s", this.elasticIndexNameResolver.getKindFromIndexName(index)));
            }
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Optional<MappingMetadata> mapping = response.mappings().values().stream().findFirst();
            return new Gson().toJson(mapping.get().getSourceAsMap(), type);
        } catch (IOException e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unknown error", String.format("Error retrieving mapping for kind %s", this.elasticIndexNameResolver.getKindFromIndexName(index)), e);
        }
    }
}