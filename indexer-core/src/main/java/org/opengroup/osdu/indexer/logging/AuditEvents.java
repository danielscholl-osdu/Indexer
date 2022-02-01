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

package org.opengroup.osdu.indexer.logging;

import com.google.common.base.Strings;
import org.opengroup.osdu.core.common.logging.audit.AuditAction;
import org.opengroup.osdu.core.common.logging.audit.AuditPayload;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;

import java.util.List;

public class AuditEvents {

    private static final String INDEX_CREATE_RECORD_ACTION_ID = "IN001";
    private static final String INDEX_CREATE_RECORDS_SUCCESS = "Successfully created record in index";
    private static final String INDEX_CREATE_RECORDS_FAILURE = "Failed creating record in index";


    private static final String INDEX_UPDATE_RECORD_ACTION_ID = "IN002";
    private static final String INDEX_UPDATE_RECORDS_SUCCESS = "Successfully updated record in index";
    private static final String INDEX_UPDATE_RECORDS_FAILURE = "Failed updating record in index";

    private static final String INDEX_DELETE_RECORD_ACTION_ID = "IN003";
    private static final String INDEX_DELETE_RECORDS_SUCCESS = "Successfully deleted record in index";
    private static final String INDEX_DELETE_RECORDS_FAILURE = "Failed deleting record in index";

    private static final String INDEX_PURGE_RECORD_ACTION_ID = "IN004";

    private static final String INDEX_STARTED_ACTION_ID = "IN006";
    private static final String INDEX_STARTED_OPERATION = "Indexing started";

    private static final String REINDEX_KIND_ACTION_ID = "IN007";
    private static final String REINDEX_KIND_OPERATION = "Reindex kind";

    private static final String COPY_INDEX_ACTION_ID = "IN008";
    private static final String COPY_INDEX_OPERATION = "Copy index";

    private static final String GET_TASK_STATUS_ACTION_ID = "IN009";
    private static final String GET_TASK_STATUS_OPERATION = "Get task status";

    private static final String RUN_JOB_ACTION_ID = "IN010";
    private static final String RUN_JOB_MESSAGE_SUCCESS = "Index clean-up status job run success";

    private static final String INDEX_MAPPING_UPDATE_ACTION_ID = "IN0011";
    private static final String INDEX_MAPPING_UPDATE_SUCCESS = "Successfully updated index mapping";
    private static final String INDEX_MAPPING_UPDATE_FAILURE = "Failed updating index mapping";

    private static final String CONFIGURE_PARTITION_ACTION_ID = "IN0012";
    private static final String CONFIGURE_PARTITION_OPERATION = "Data partition cluster configuration update";

    private static final String INDEX_DELETE_ACTION_ID = "IN0013";
    private static final String INDEX_DELETE_SUCCESS = "Successfully deleted index";
    private static final String INDEX_DELETE_FAILURE = "Failed deleting index";

    private final String user;

    public AuditEvents(String user) {
        if (Strings.isNullOrEmpty(user)) {
            throw new IllegalArgumentException("User not provided for audit events.");
        }
        this.user = user;
    }

    public AuditPayload getIndexCreateRecordSuccessEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.CREATE)
                .status(AuditStatus.SUCCESS)
                .actionId(INDEX_CREATE_RECORD_ACTION_ID)
                .message(INDEX_CREATE_RECORDS_SUCCESS)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getIndexCreateRecordFailEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.CREATE)
                .status(AuditStatus.FAILURE)
                .actionId(INDEX_CREATE_RECORD_ACTION_ID)
                .message(INDEX_CREATE_RECORDS_FAILURE)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getIndexUpdateRecordSuccessEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.UPDATE)
                .status(AuditStatus.SUCCESS)
                .actionId(INDEX_UPDATE_RECORD_ACTION_ID)
                .message(INDEX_UPDATE_RECORDS_SUCCESS)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getIndexUpdateRecordFailEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.UPDATE)
                .status(AuditStatus.FAILURE)
                .actionId(INDEX_UPDATE_RECORD_ACTION_ID)
                .message(INDEX_UPDATE_RECORDS_FAILURE)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getIndexDeleteRecordSuccessEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.DELETE)
                .status(AuditStatus.SUCCESS)
                .actionId(INDEX_DELETE_RECORD_ACTION_ID)
                .message(INDEX_DELETE_RECORDS_SUCCESS)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getIndexDeleteRecordFailEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.DELETE)
                .status(AuditStatus.FAILURE)
                .actionId(INDEX_DELETE_RECORD_ACTION_ID)
                .message(INDEX_DELETE_RECORDS_FAILURE)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getIndexDeleteFailEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.DELETE)
                .status(AuditStatus.FAILURE)
                .actionId(INDEX_DELETE_ACTION_ID)
                .message(INDEX_DELETE_FAILURE)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getIndexDeleteSuccessEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.DELETE)
                .status(AuditStatus.SUCCESS)
                .actionId(INDEX_DELETE_ACTION_ID)
                .message(INDEX_DELETE_SUCCESS)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getIndexPurgeRecordSuccessEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.DELETE)
                .status(AuditStatus.SUCCESS)
                .actionId(INDEX_PURGE_RECORD_ACTION_ID)
                .message(INDEX_DELETE_RECORDS_SUCCESS)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getIndexPurgeRecordFailEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.DELETE)
                .status(AuditStatus.FAILURE)
                .actionId(INDEX_PURGE_RECORD_ACTION_ID)
                .message(INDEX_DELETE_RECORDS_FAILURE)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getIndexEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.CREATE)
                .status(AuditStatus.SUCCESS)
                .actionId(INDEX_STARTED_ACTION_ID)
                .message(INDEX_STARTED_OPERATION)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getReindexEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.CREATE)
                .status(AuditStatus.SUCCESS)
                .actionId(REINDEX_KIND_ACTION_ID)
                .message(REINDEX_KIND_OPERATION)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getCopyIndexEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.CREATE)
                .status(AuditStatus.SUCCESS)
                .actionId(COPY_INDEX_ACTION_ID)
                .message(COPY_INDEX_OPERATION)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getTaskStatusEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.READ)
                .status(AuditStatus.SUCCESS)
                .actionId(GET_TASK_STATUS_ACTION_ID)
                .message(GET_TASK_STATUS_OPERATION)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getIndexCleanUpJobRunEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.JOB_RUN)
                .status(AuditStatus.SUCCESS)
                .user(this.user)
                .actionId(RUN_JOB_ACTION_ID)
                .message(RUN_JOB_MESSAGE_SUCCESS)
                .resources(resources)
                .build();
    }

    public AuditPayload getConfigurePartitionEvent(List<String> resources) {
        return AuditPayload.builder()
                .action(AuditAction.UPDATE)
                .status(AuditStatus.SUCCESS)
                .actionId(CONFIGURE_PARTITION_ACTION_ID)
                .message(CONFIGURE_PARTITION_OPERATION)
                .resources(resources)
                .user(this.user)
                .build();
    }

    public AuditPayload getIndexMappingUpdateEvent(List<String> resources, boolean isSuccess) {
        if (isSuccess) {
            return AuditPayload.builder()
                    .action(AuditAction.UPDATE)
                    .status(AuditStatus.SUCCESS)
                    .actionId(INDEX_MAPPING_UPDATE_ACTION_ID)
                    .message(INDEX_MAPPING_UPDATE_SUCCESS)
                    .resources(resources)
                    .user(this.user)
                    .build();
        } else {
            return AuditPayload.builder()
                    .action(AuditAction.UPDATE)
                    .status(AuditStatus.FAILURE)
                    .actionId(INDEX_MAPPING_UPDATE_ACTION_ID)
                    .message(INDEX_MAPPING_UPDATE_FAILURE)
                    .resources(resources)
                    .user(this.user)
                    .build();
        }
    }
}