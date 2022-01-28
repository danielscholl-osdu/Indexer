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

package org.opengroup.osdu.indexer.api;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.extern.java.Log;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.RecordInfo;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.model.search.SearchServiceRole;
import org.opengroup.osdu.core.common.model.storage.validation.ValidKind;
import org.opengroup.osdu.indexer.SwaggerDoc;
import org.opengroup.osdu.indexer.logging.AuditLogger;
import org.opengroup.osdu.indexer.service.IndexerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;
import springfox.documentation.annotations.ApiIgnore;

@Log
@RestController
@RequestScope
public class CleanupIndiciesApi {

  @Autowired
  private IndexerService indexerService;

  @Autowired
  private AuditLogger auditLogger;

  @ApiIgnore
  @PostMapping(path = "/index-cleanup", consumes = "application/json")
  @PreAuthorize("@authorizationFilter.hasPermission('" + SearchServiceRole.ADMIN + "')")
  public ResponseEntity cleanupIndices(@NotNull(message = SwaggerDoc.REQUEST_VALIDATION_NOT_NULL_BODY)
                                         @Valid @RequestBody RecordChangedMessages message) {
    if (message == null) {
      throw new AppException(HttpStatus.BAD_REQUEST.value(), "Request body is null",
          SwaggerDoc.REQUEST_VALIDATION_NOT_NULL_BODY);
    }

    if (message.missingAccountId()) {
      throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid tenant",
          String.format("Required header: '%s' not found", DpsHeaders.DATA_PARTITION_ID));
    }
    try {
      Type listType = new TypeToken<List<RecordInfo>>() {
      }.getType();
      List<RecordInfo> recordInfos = new Gson().fromJson(message.getData(), listType);

      if (recordInfos.isEmpty()) {
        log.info("none of record-change message can be deserialized");
        return new ResponseEntity(HttpStatus.OK);
      }
      indexerService.processSchemaMessages(recordInfos);

      auditLogger.getIndexCleanUpJobRun(recordInfos.stream()
              .map(RecordInfo::getKind)
              .collect(Collectors.toList()));
      return new ResponseEntity(HttpStatus.OK);
    } catch (AppException e) {
      throw e;
    } catch (JsonParseException e) {
      throw new AppException(HttpStatus.BAD_REQUEST.value(), "Request payload parsing error", "Unable to parse request payload.", e);
    } catch (Exception e) {
      throw new AppException(HttpStatus.BAD_REQUEST.value(), "Unknown error", "An unknown error has occurred.", e);
    }
  }

  public ResponseEntity deleteIndex(@RequestParam("kind") @NotBlank @ValidKind String kind) {
    try {
      String index = elasticIndexNameResolver.getIndexNameFromKind(kind);
      boolean responseStatus = indicesService.deleteIndex(index);
      if (responseStatus) {
        return new ResponseEntity(HttpStatus.OK);
      }
    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      throw new AppException(HttpStatus.BAD_REQUEST.value(), "Unknown error", "An unknown error has occurred.", e);
    }
    return new ResponseEntity(HttpStatus.BAD_REQUEST);
  }
}
