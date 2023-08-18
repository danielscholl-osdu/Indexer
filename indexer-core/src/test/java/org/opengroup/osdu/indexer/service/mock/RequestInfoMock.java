package org.opengroup.osdu.indexer.service.mock;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RequestInfoMock implements IRequestInfo {
    @Override
    public DpsHeaders getHeaders() {
        return null;
    }

    @Override
    public String getPartitionId() {
        return null;
    }

    @Override
    public Map<String, String> getHeadersMap() {
        return null;
    }

    @Override
    public Map<String, String> getHeadersMapWithDwdAuthZ() {
        return null;
    }

    @Override
    public DpsHeaders getHeadersWithDwdAuthZ() {
        return null;
    }

    @Override
    public boolean isCronRequest() {
        return false;
    }

    @Override
    public boolean isTaskQueueRequest() {
        return false;
    }
}
