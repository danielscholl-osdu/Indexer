
package org.opengroup.osdu.indexer.ibm.di;

import org.opengroup.osdu.core.common.entitlements.EntitlementsAPIConfig;
import org.opengroup.osdu.core.common.entitlements.EntitlementsFactory;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@Lazy
public class EntitlementsClientFactory extends AbstractFactoryBean<IEntitlementsFactory> {

	@Value("${AUTHORIZE_API}")
	private String AUTHORIZE_API;

	@Value("${AUTHORIZE_API_KEY:}")
	private String AUTHORIZE_API_KEY;

	@Autowired
	private HttpResponseBodyMapper mapper;

	@Override
	protected IEntitlementsFactory createInstance() throws Exception {

		return new EntitlementsFactory(EntitlementsAPIConfig
				.builder()
				.rootUrl(AUTHORIZE_API)
				.apiKey(AUTHORIZE_API_KEY)
				.build(),
				mapper);
	}

	@Override
	public Class<?> getObjectType() {
		return IEntitlementsFactory.class;
	}
}