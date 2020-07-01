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

import org.opengroup.osdu.core.common.model.search.SearchServiceRole;
import org.opengroup.osdu.indexer.logging.AuditLogger;
import org.opengroup.osdu.core.common.model.indexer.RecordReindexRequest;
import org.opengroup.osdu.indexer.service.IndexSchemaService;
import org.opengroup.osdu.indexer.service.ReindexService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import java.io.IOException;

import static java.util.Collections.singletonList;

@RestController
@RequestMapping("/reindex")
@RequestScope
public class ReindexApi {

    @Inject
    private ReindexService reIndexService;
    @Inject
    private IndexSchemaService indexSchemaService;
    @Inject
    private AuditLogger auditLogger;

    @PreAuthorize("@authorizationFilter.hasPermission('" + SearchServiceRole.ADMIN + "')")
    @PostMapping
    public ResponseEntity<?> reindex(
            @NotNull @Valid @RequestBody RecordReindexRequest recordReindexRequest,
            @RequestParam(value = "force_clean", defaultValue = "false") boolean forceClean) throws IOException {
        this.reIndexService.reindexRecords(recordReindexRequest, this.indexSchemaService.isStorageSchemaSyncRequired(recordReindexRequest.getKind(), forceClean));
        this.auditLogger.getReindex(singletonList(recordReindexRequest.getKind()));
        return new ResponseEntity<>(org.springframework.http.HttpStatus.OK);
    }
}
