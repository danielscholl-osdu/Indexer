package org.opengroup.osdu.indexer.di;

import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.gcp.multitenancy.credentials.DatastoreCredential;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class DatastoreCredentialsCacheFactory extends
    AbstractFactoryBean<ICache<String, DatastoreCredential>> {

  @Override
  public Class<?> getObjectType() {
    return ICache.class;
  }

  @Override
  protected ICache<String, DatastoreCredential> createInstance() throws Exception {
    return new VmCache<>(5 * 60, 20);
  }
}
