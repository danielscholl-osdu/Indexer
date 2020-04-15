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

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.audit.AuditAction;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
public class AuditEventsTest {

    @Test(expected = IllegalArgumentException.class)
    public void should_throwException_when_creatingAuditEventsWithoutUser() {
        new AuditEvents(null);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_getIndexCreateRecordEventSuccess() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getIndexCreateRecordSuccessEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        assertEquals("Successfully created record in index", payload.get("message"));
        assertEquals(AuditAction.CREATE, payload.get("action"));
        assertEquals("IN001", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_getIndexCreateRecordEventFail() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getIndexCreateRecordFailEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.FAILURE, payload.get("status"));
        assertEquals("Failed creating record in index", payload.get("message"));
        assertEquals(AuditAction.CREATE, payload.get("action"));
        assertEquals("IN001", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_getIndexUpdateRecordEventSuccess() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getIndexUpdateRecordSuccessEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        assertEquals("Successfully updated record in index", payload.get("message"));
        assertEquals(AuditAction.UPDATE, payload.get("action"));
        assertEquals("IN002", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_getIndexUpdateRecordEventFail() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getIndexUpdateRecordFailEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.FAILURE, payload.get("status"));
        assertEquals("Failed updating record in index", payload.get("message"));
        assertEquals(AuditAction.UPDATE, payload.get("action"));
        assertEquals("IN002", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_getIndexDeleteRecordEventSuccess() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getIndexDeleteRecordSuccessEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        assertEquals("Successfully deleted record in index", payload.get("message"));
        assertEquals(AuditAction.DELETE, payload.get("action"));
        assertEquals("IN003", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_getIndexDeleteRecordEventFail() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getIndexDeleteRecordFailEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.FAILURE, payload.get("status"));
        assertEquals("Failed deleting record in index", payload.get("message"));
        assertEquals(AuditAction.DELETE, payload.get("action"));
        assertEquals("IN003", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_getIndexPurgeRecordEventSuccess() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getIndexPurgeRecordSuccessEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        assertEquals("Successfully deleted record in index", payload.get("message"));
        assertEquals(AuditAction.DELETE, payload.get("action"));
        assertEquals("IN004", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_getIndexPurgeRecordEventFail() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getIndexPurgeRecordFailEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.FAILURE, payload.get("status"));
        assertEquals("Failed deleting record in index", payload.get("message"));
        assertEquals(AuditAction.DELETE, payload.get("action"));
        assertEquals("IN004", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_getReindexEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getReindexEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        assertEquals("Reindex kind", payload.get("message"));
        assertEquals(AuditAction.CREATE, payload.get("action"));
        assertEquals("IN007", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_getCopyIndexEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getCopyIndexEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        assertEquals("Copy index", payload.get("message"));
        assertEquals(AuditAction.CREATE, payload.get("action"));
        assertEquals("IN008", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void getTaskStatusEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getTaskStatusEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        assertEquals("Get task status", payload.get("message"));
        assertEquals(AuditAction.READ, payload.get("action"));
        assertEquals("IN009", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_getIndexCleanUpJobRunEvent() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getIndexCleanUpJobRunEvent(Lists.newArrayList("anything"))
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        assertEquals("Index clean-up status job run success", payload.get("message"));
        assertEquals(AuditAction.JOB_RUN, payload.get("action"));
        assertEquals("IN010", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_getIndexMappingUpdateEventSuccess() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getIndexMappingUpdateEvent(Lists.newArrayList("anything"),true)
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.SUCCESS, payload.get("status"));
        assertEquals("Successfully updated index mapping", payload.get("message"));
        assertEquals(AuditAction.UPDATE, payload.get("action"));
        assertEquals("IN0011", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void should_getIndexMappingUpdateEventFail() {
        AuditEvents auditEvent = new AuditEvents("testUser");
        Map<String, String> payload = (Map) auditEvent.getIndexMappingUpdateEvent(Lists.newArrayList("anything"),false)
                .get("auditLog");
        assertEquals(Lists.newArrayList("anything"), payload.get("resources"));
        assertEquals(AuditStatus.FAILURE, payload.get("status"));
        assertEquals("Failed updating index mapping", payload.get("message"));
        assertEquals(AuditAction.UPDATE, payload.get("action"));
        assertEquals("IN0011", payload.get("actionId"));
        assertEquals("testUser", payload.get("user"));
    }
}
