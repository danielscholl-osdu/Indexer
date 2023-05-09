/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.indexer.provider.gcp.indexing.processing;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.auth.TokenProvider;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestStatus;
import org.opengroup.osdu.core.gcp.oqm.model.OqmAckReplier;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessage;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessageReceiver;
import org.opengroup.osdu.indexer.provider.gcp.indexing.scope.ThreadDpsHeaders;
import org.opengroup.osdu.indexer.provider.gcp.indexing.thread.ThreadScopeContextHolder;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public abstract class IndexerOqmMessageReceiver implements OqmMessageReceiver {

    protected final ThreadDpsHeaders dpsHeaders;
    private final TokenProvider tokenProvider;

    @Override
    public void receiveMessage(OqmMessage oqmMessage, OqmAckReplier oqmAckReplier) {
        log.info("OQM message: {} - {} - {}", oqmMessage.getId(), oqmMessage.getData(), oqmMessage.getAttributes());
        if (!validInput(oqmMessage)) {
            log.error("Not valid event payload, event will not be processed.");
            oqmAckReplier.ack();
            return;
        }

        try {
            DpsHeaders headers = getHeaders(oqmMessage);
            // Filling thread context required by the core services.
            dpsHeaders.setThreadContext(headers.getHeaders());
            sendMessage(oqmMessage);
            oqmAckReplier.ack();
        } catch (AppException appException) {
            int statusCode = appException.getError().getCode();
            if (statusCode > 199 && statusCode < 300 && statusCode != RequestStatus.INVALID_RECORD) {
                skipMessage(oqmMessage, dpsHeaders, oqmAckReplier, appException);
            } else {
                rescheduleMessage(oqmMessage, dpsHeaders, oqmAckReplier, getException(appException));
            }
        } catch (Exception exception) {
            rescheduleMessage(oqmMessage, dpsHeaders, oqmAckReplier, exception);
        } catch (Throwable e) {
            //Catching throwable is necessary, otherwise, errors like NoSuchMethodError will slip through silently.
            log.error(
                "HALT! Unrecoverable malfunction! Unexpected error was thrown during processing! Event ID: "
                    + oqmMessage.getId() + ". Correlation ID: " + dpsHeaders.getCorrelationId(), e);
            throw e;
        } finally {
            // Cleaning thread context after processing is finished and the thread dies out.
            ThreadScopeContextHolder.currentThreadScopeAttributes().clear();
        }
    }

    private static void skipMessage(OqmMessage oqmMessage, DpsHeaders dpsHeaders,
        OqmAckReplier oqmAckReplier, AppException appException) {
        log.info(
            "Event ID: " + oqmMessage.getId() + ". Correlation ID: " + dpsHeaders.getCorrelationId()
                + ", was not processed, and will NOT be rescheduled.", appException);
        oqmAckReplier.ack();
    }

    private static void rescheduleMessage(OqmMessage oqmMessage, DpsHeaders dpsHeaders,
        OqmAckReplier oqmAckReplier, Exception exception) {
        log.error(
            "Event id : " + oqmMessage.getId() + ". Correlation ID: " + dpsHeaders.getCorrelationId()
                + ", was not processed, and will BE rescheduled.", exception);
        oqmAckReplier.nack();
    }

    /**
     * It is possible to get both AppException with wrapped in original Exception
     * or the original Exception without any wrapper.
     */
    @NotNull
    private static Exception getException(AppException appException) {
        return Optional.ofNullable(appException.getOriginalException()).orElse(appException);
    }

    private boolean validInput(OqmMessage oqmMessage) {
        boolean isValid = true;
        if (Strings.isNullOrEmpty(oqmMessage.getData()) || oqmMessage.getData().equals("{}")) {
            log.error("Message body is empty, message id: {}, attributes: {}", oqmMessage.getId(), oqmMessage.getAttributes());
            isValid = false;
        }
        if (oqmMessage.getAttributes() == null || oqmMessage.getAttributes().size() == 0) {
            log.error("Attribute map not found, message id: {}, attributes: {}", oqmMessage.getId(), oqmMessage.getAttributes());
            isValid = false;
        }
        return isValid;
    }

    protected abstract void sendMessage(OqmMessage oqmMessage) throws Exception;

    @NotNull
    private DpsHeaders getHeaders(OqmMessage oqmMessage) {
        DpsHeaders headers = DpsHeaders.createFromMap(oqmMessage.getAttributes());
        headers.getHeaders().put("authorization", "Bearer " + tokenProvider.getIdToken());
        return headers;
    }
}
