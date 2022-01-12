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

import lombok.extern.java.Log;
import org.opengroup.osdu.indexer.logging.AuditLogger;
import org.opengroup.osdu.indexer.service.IClusterConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import java.io.IOException;
import java.util.ArrayList;


@Log
@RestController
@RequestMapping("/partitions/settings")
@RequestScope
public class DataPartitionSettingsApi {

    private static final String OPS = "users.datalake.ops";

    @Autowired
    private IClusterConfigurationService clusterConfigurationService;
    @Autowired
    private AuditLogger auditLogger;

    @PreAuthorize("@authorizationFilter.hasPermission('" + OPS + "')")
    @PostMapping
    public ResponseEntity<?> partitionInit() throws IOException {
        this.clusterConfigurationService.updateClusterConfiguration();
        this.auditLogger.getConfigurePartition(new ArrayList<>());
        return new ResponseEntity<>(org.springframework.http.HttpStatus.OK);
    }
}
