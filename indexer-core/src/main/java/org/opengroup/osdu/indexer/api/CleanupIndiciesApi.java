package org.opengroup.osdu.indexer.api;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.java.Log;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.client.RestHighLevelClient;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.OperationType;
import org.opengroup.osdu.core.common.model.indexer.RecordInfo;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.model.search.SearchServiceRole;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.core.common.search.IndicesService;
import org.opengroup.osdu.indexer.SwaggerDoc;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

@Log
@RestController
@RequestScope
public class CleanupIndiciesApi {

  @Autowired
  private ElasticClientHandler elasticClientHandler;

  @Autowired
  private ElasticIndexNameResolver elasticIndexNameResolver;

  @Autowired
  private IndicesService indicesService;

  @PostMapping(path = "/index-cleanup", consumes = "application/json")
  @PreAuthorize("@authorizationFilter.hasRole('" + SearchServiceRole.ADMIN + "')")
  public ResponseEntity cleanupIndices(@NotNull(message = SwaggerDoc.REQUEST_VALIDATION_NOT_NULL_BODY)
                                         @Valid @RequestBody RecordChangedMessages recordChangedMessages) throws Exception {

    if (recordChangedMessages.missingAccountId()) {
      throw new AppException(org.apache.http.HttpStatus.SC_BAD_REQUEST, "Invalid tenant",
          String.format("Required header: '%s' not found", DpsHeaders.DATA_PARTITION_ID));
    }
    try {
      if (recordChangedMessages == null) {
        log.info("record change messages is null");
      }

      Type listType = new TypeToken<List<RecordInfo>>() {
      }.getType();
      List<RecordInfo> recordInfos = new Gson().fromJson(recordChangedMessages.getData(), listType);

      if (recordInfos.size() == 0) {
        log.info("none of record-change message can be deserialized");
        return new ResponseEntity(HttpStatus.OK);
      }
      processSchemaMessages(recordChangedMessages, recordInfos);
      return new ResponseEntity(HttpStatus.OK);
    } catch (AppException e) {
      throw e;
    } catch (JsonParseException e) {
      throw new AppException(HttpStatus.BAD_REQUEST.value(), "Request payload parsing error", "Unable to parse request payload.", e);
    } catch (Exception e) {
      throw new AppException(HttpStatus.BAD_REQUEST.value(), "Unknown error", "An unknown error has occurred.", e);
    }
  }

  private void processSchemaMessages(RecordChangedMessages message, List<RecordInfo> recordInfos) throws Exception {
    Map<String, OperationType> schemaMsgs = RecordInfo.getSchemaMsgs(recordInfos);
    if (schemaMsgs != null && !schemaMsgs.isEmpty()) {
      try (RestHighLevelClient restClient = elasticClientHandler.createRestClient()) {
        schemaMsgs.entrySet().forEach(msg -> {
          try {
            processSchemaEvents(restClient, msg);
          } catch (IOException | ElasticsearchStatusException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "unable to process schema delete", e.getMessage());
          }
        });
      }
    }
  }

  private void processSchemaEvents(RestHighLevelClient restClient,
                                   Map.Entry<String, OperationType> msg) throws IOException, ElasticsearchStatusException {
    String kind = msg.getKey();
    String index = elasticIndexNameResolver.getIndexNameFromKind(kind);

    boolean indexExist = indicesService.isIndexExist(restClient, index);
    if (msg.getValue() == OperationType.purge_schema) {
      if (indexExist) {
        indicesService.deleteIndex(restClient, index);
      } else {
        log.warning(String.format("Kind: %s not found", kind));
      }
    }
  }
}
