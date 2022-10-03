package org.opengroup.osdu.indexer.service;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.IPartitionFactory;
import org.opengroup.osdu.core.common.partition.IPartitionProvider;
import org.opengroup.osdu.core.common.partition.PartitionException;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PartitionServiceImpl implements IPartitionService {
    @Autowired
    private DpsHeaders headers;

    @Autowired
    private IPartitionFactory factory;

    @Autowired
    private IServiceAccountJwtClient tokenService;

    @Override
    public PartitionInfo getPartitionInfo() {
        try {
            DpsHeaders partitionHeaders = DpsHeaders.createFromMap(headers.getHeaders());
            partitionHeaders.put(DpsHeaders.AUTHORIZATION, this.tokenService.getIdToken(this.headers.getPartitionId()));

            IPartitionProvider serviceClient = this.factory.create(partitionHeaders);
            PartitionInfo partitionInfo = serviceClient.get(this.headers.getPartitionId());
            return partitionInfo;
        } catch (PartitionException e) {
            throw new AppException(HttpStatus.SC_FORBIDDEN, "Service unavailable", String.format("Error getting partition info for data-partition: %s", this.headers.getPartitionId()), e);
        }
    }
}
