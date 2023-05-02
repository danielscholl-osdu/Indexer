// Copyright © Schlumberger
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

import org.opengroup.osdu.indexer.logging.AuditLogger;
import org.opengroup.osdu.indexer.model.IndexAliasesResult;
import org.opengroup.osdu.indexer.service.IClusterConfigurationService;
import org.opengroup.osdu.indexer.service.IndexAliasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;

import java.io.IOException;

import static java.util.Collections.singletonList;
import static org.opengroup.osdu.core.common.model.http.DpsHeaders.DATA_PARTITION_ID;

@RestController
@RequestMapping("/partitions")
@RequestScope
public class PartitionSetupApi {

    private static final String OPS = "users.datalake.ops";

    @Autowired
    private IndexAliasService indexAliasService;
    @Autowired
    private IClusterConfigurationService clusterConfigurationService;
    @Autowired
    private AuditLogger auditLogger;

    @PreAuthorize("@authorizationFilter.hasPermission('" + OPS + "')")
    @PutMapping(path = "/provision", consumes = "application/json")
    public ResponseEntity<?> provisionPartition(@RequestHeader(DATA_PARTITION_ID) String dataPartitionId) throws IOException {
        this.clusterConfigurationService.updateClusterConfiguration();
        this.auditLogger.getConfigurePartition(singletonList(dataPartitionId));
        return new ResponseEntity<>(org.springframework.http.HttpStatus.OK);
    }

    @PreAuthorize("@authorizationFilter.hasPermission('" + OPS + "')")
    @PostMapping(path = "/aliases")
    public ResponseEntity<IndexAliasesResult> createIndexAliases()  {
        IndexAliasesResult result = indexAliasService.createIndexAliasesForAll();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
