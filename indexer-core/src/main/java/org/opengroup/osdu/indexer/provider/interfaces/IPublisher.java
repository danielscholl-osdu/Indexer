package org.opengroup.osdu.indexer.provider.interfaces;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;

public interface IPublisher {

    public void publishStatusChangedTagsToTopic(DpsHeaders headers, JobStatus indexerBatchStatus) throws Exception;
}
