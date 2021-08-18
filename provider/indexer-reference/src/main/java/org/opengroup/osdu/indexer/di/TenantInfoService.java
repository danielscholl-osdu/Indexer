package org.opengroup.osdu.indexer.di;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.multitenancy.ITenantInfoService;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@RequestScope
@Component
public class TenantInfoService implements ITenantInfoService {

  @Inject
  private ITenantFactory tenantFactory;

  @Inject
  private DpsHeaders headers;

  @Override
  public TenantInfo getTenantInfo() {
    return tenantFactory.getTenantInfo(headers.getPartitionId());
  }

  @Override
  public List<TenantInfo> getAllTenantInfos() {
    return new ArrayList<>(tenantFactory.listTenantInfo());
  }
}
