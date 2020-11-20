/*
  Copyright 2020 Google LLC
  Copyright 2020 EPAM Systems, Inc

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package org.opengroup.osdu.util;

import com.google.gson.Gson;
import lombok.extern.java.Log;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;


/**
 * All util methods to use elastic apis for tests
 * It should be used only in the Setup or TearDown phase of the test
 */
@Log
public class ElasticUtils {

    private static final int REST_CLIENT_CONNECT_TIMEOUT = 5000;
    private static final int REST_CLIENT_SOCKET_TIMEOUT = 60000;
    private static final int REST_CLIENT_RETRY_TIMEOUT = 60000;

    private final TimeValue REQUEST_TIMEOUT = TimeValue.timeValueMinutes(1);

    private final String username;
    private final String password;
    private final String host;
    private final boolean sslEnabled;

    public ElasticUtils() {
        this.username = Config.getUserName();
        this.password = Config.getPassword();
        this.host = Config.getElasticHost();
        this.sslEnabled = Config.isElasticSslEnabled();
    }

    public void createIndex(String index, String mapping) {
        try {
            try (RestHighLevelClient client = this.createClient(username, password, host)) {
                Settings settings = Settings.builder()
                        .put("index.number_of_shards", 1)
                        .put("index.number_of_replicas", 1).build();

                // creating index + add mapping to the index
                log.info("Creating index with name: " + index);
                CreateIndexRequest request = new CreateIndexRequest(index, settings);
                request.source("{\"mappings\":" + mapping + "}", XContentType.JSON);
                request.timeout(REQUEST_TIMEOUT);
                CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);

                //wait for ack
                for (int i = 0; ; i++) {
                    if (response.isAcknowledged() && response.isShardsAcknowledged()) {
                        break;
                    } else {
                        log.info("Failed to get confirmation from elastic server, will sleep for 15 seconds");
                        Thread.sleep(15000);
                        if (i > 3) {
                            log.info("Failed to get confirmation from elastic server after 3 retries");
                            throw new AssertionError("Failed to get confirmation from Elastic cluster");
                        }
                    }
                }

                log.info("Done creating index with name: " + index);
            }
        } catch (ElasticsearchStatusException e) {
            if (e.status() == RestStatus.BAD_REQUEST &&
                    (e.getMessage().contains("resource_already_exists_exception") || e.getMessage().contains("IndexAlreadyExistsException"))) {
                log.info("Index already exists. Ignoring error...");
            }
        } catch (Exception e) {
            throw new AssertionError(e.getMessage());
        }
    }

    public int indexRecords(String index, String kind, List<Map<String, Object>> testRecords) {
        log.info("Creating records inside index with name: " + index);

        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout(REQUEST_TIMEOUT);

        List<IndexRequest> records = ElasticUtils.getIndexReqFromRecord(index, kind, testRecords);
        for (IndexRequest record : records) {
            bulkRequest.add(record);
        }

        BulkResponse bulkResponse = null;
        try {
            try (RestHighLevelClient client = this.createClient(username, password, host)) {
                bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                log.info("Done creating records inside index with name: " + index);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "bulk indexing failed", e);
        }

        // Double check failures
        if (bulkResponse != null && bulkResponse.hasFailures()) {
            throw new AssertionError("setup failed in data post to Index");
        }

        try {
            try (RestHighLevelClient client = this.createClient(username, password, host)) {
                RefreshRequest request = new RefreshRequest(index);
                RefreshResponse refreshResponse = client.indices().refresh(request, RequestOptions.DEFAULT);
                log.info(String.format("refreshed index, acknowledged shards: %s | failed shards: %s | total shards: %s ", refreshResponse.getSuccessfulShards(), refreshResponse.getFailedShards(), refreshResponse.getTotalShards()));
            }
        } catch (IOException | ElasticsearchException e) {
            log.log(Level.SEVERE, "index refresh failed", e);
        }

        return records.size();
    }

    public void deleteIndex(String index) {
        try (RestHighLevelClient client = this.createClient(username, password, host)) {
            //retry if the elastic cluster is snapshotting and we cant delete it
            for (int retries = 0; ; retries++) {
                try {
                    log.info("Deleting index with name: " + index + ", retry count: " + retries);
                    DeleteIndexRequest request = new DeleteIndexRequest(index);
                    client.indices().delete(request, RequestOptions.DEFAULT);
                    log.info("Done deleting index with name: " + index);
                    return;
                } catch (ElasticsearchException e) {
                    if (e.status() == RestStatus.NOT_FOUND) {
                        return;
                    } else if (e.getMessage().contains("Cannot delete indices that are being snapshotted")) {
                        closeIndex(client, index);
                        log.info(String.format("skipping %s index delete, as snapshot is being run, closing the index instead", index));
                        return;
                    } else if (retries < 4) {
                        log.info("Retrying to delete index due to following error: " + e.getMessage());
                        try {
                            Thread.sleep(12000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    } else {
                        closeIndex(client, index);
                        log.info(String.format("maximum retries: %s reached for index: %s delete, closing the index instead", retries, index));
                    }
                }
            }
        } catch (IOException e) {
            throw new AssertionError(e.getMessage());
        }
    }

    public long fetchRecords(String index) throws IOException {
        try {
            try (RestHighLevelClient client = this.createClient(username, password, host)) {
                SearchRequest request = new SearchRequest(index);
                SearchResponse searchResponse = client.search(request, RequestOptions.DEFAULT);
                return searchResponse.getHits().totalHits;
            }
        } catch (ElasticsearchStatusException e) {
            log.log(Level.INFO, String.format("Elastic search threw exception: %s", e.getMessage()));
            return -1;
        }
    }

    public long fetchRecordsByExistQuery(String index, String attributeName) throws IOException {
        try {
            try (RestHighLevelClient client = this.createClient(username, password, host)) {
                SearchRequest searchRequest = new SearchRequest(index);
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(QueryBuilders.existsQuery(attributeName));
                searchRequest.source(searchSourceBuilder);

                SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
                return searchResponse.getHits().totalHits;
            }
        } catch (ElasticsearchStatusException e) {
            log.log(Level.INFO, String.format("Elastic search threw exception: %s", e.getMessage()));
            return -1;
        }
    }

    public ImmutableOpenMap<String, MappingMetaData> getMapping(String index) throws IOException {
        try (RestHighLevelClient client = this.createClient(username, password, host)) {
            GetMappingsRequest request = new GetMappingsRequest();
            request.indices(index);
            GetMappingsResponse response = client.indices().getMapping(request, RequestOptions.DEFAULT);
            ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> allMappings = response.mappings();
            return allMappings.get(index);
        }
    }

    public void refreshIndex(String index) throws IOException {
        try (RestHighLevelClient client = this.createClient(username, password, host)) {
            try {
                RefreshRequest request = new RefreshRequest(index);
                client.indices().refresh(request, RequestOptions.DEFAULT);
            } catch (ElasticsearchException exception) {
                log.info(String.format("index: %s refresh failed. message: %s", index, exception.getDetailedMessage()));
            }
        }
    }

    private boolean closeIndex(RestHighLevelClient client, String index) {
        try {
            CloseIndexRequest request = new CloseIndexRequest(index);
            request.timeout(TimeValue.timeValueMinutes(1));
            AcknowledgedResponse closeIndexResponse = client.indices().close(request, RequestOptions.DEFAULT);
            return closeIndexResponse.isAcknowledged();
        } catch (ElasticsearchException | IOException exception) {
            log.info(String.format("index: %s close failed. message: %s", index, exception.getMessage()));
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<IndexRequest> getIndexReqFromRecord(String index, String kind, List<Map<String, Object>> testRecords) {
        List<IndexRequest> dataList = new ArrayList<>();
        Gson gson = new Gson();
        try {
            for (Map<String, Object> record : testRecords) {
                String id = (String) record.get("id");
                Map<String, Object> mapData = gson.fromJson(gson.toJson(record), Map.class);
                IndexRequest indexRequest = new IndexRequest(index, kind.split(":")[2], id).source(mapData);
                dataList.add(indexRequest);
            }
        } catch (Exception e) {
            throw new AssertionError(e.getMessage());
        }
        return dataList;
    }


    private RestHighLevelClient createClient(String username, String password, String host) {

        RestHighLevelClient restHighLevelClient;
        int port = Config.getPort();
        try {
            String rawString = String.format("%s:%s", username, password);

            RestClientBuilder builder = createClientBuilder(host,rawString, port);

            restHighLevelClient = new RestHighLevelClient(builder);

        } catch (Exception e) {
            throw new AssertionError("Setup elastic error");
        }
        return restHighLevelClient;
    }

    public RestClientBuilder createClientBuilder(String host, String usernameAndPassword, int port) {
            String scheme = this.sslEnabled ? "https" : "http";
            RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme));
            builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(REST_CLIENT_CONNECT_TIMEOUT)
                    .setSocketTimeout(REST_CLIENT_SOCKET_TIMEOUT));
            builder.setMaxRetryTimeoutMillis(REST_CLIENT_RETRY_TIMEOUT);

            Header[] defaultHeaders = new Header[]{
                    new BasicHeader("client.transport.nodes_sampler_interval", "30s"),
                    new BasicHeader("client.transport.ping_timeout", "30s"),
                    new BasicHeader("client.transport.sniff", "false"),
                    new BasicHeader("request.headers.X-Found-Cluster", Config.getElasticHost()),
                    new BasicHeader("cluster.name", Config.getElasticHost()),
                    new BasicHeader("xpack.security.transport.ssl.enabled", Boolean.toString(true)),
                new BasicHeader("Authorization", String.format("Basic %s", Base64.getEncoder().encodeToString(usernameAndPassword.getBytes()))),
            };

            builder.setDefaultHeaders(defaultHeaders);
        return builder;
    }

    public boolean isIndexExist(String index) {
        boolean exists = false;
        try {
            exists = createRestClientAndCheckIndexExist(index);
        } catch (ElasticsearchStatusException e) {
            log.log(Level.INFO, String.format("Error getting index: %s %s", index, e.getMessage()));
        }
        return exists;
    }

    private boolean createRestClientAndCheckIndexExist(String index) {
        try (RestHighLevelClient client = this.createClient(username, password, host)) {
            GetIndexRequest request = new GetIndexRequest();
            request.indices(index);
            return client.indices().exists(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.log(Level.INFO, String.format("Error getting index: %s %s", index, e.getMessage()));
        }
        return false;
    }
}