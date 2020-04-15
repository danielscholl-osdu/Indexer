package org.opengroup.osdu.indexer.middleware;

import lombok.extern.java.Log;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.provider.interfaces.IAuthorizationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import javax.inject.Inject;

@Log
@Component("authorizationFilter")
@RequestScope
public class AuthorizationFilter {

    @Inject
    private IAuthorizationService authorizationService;

    private DpsHeaders headers;

    @Inject
    AuthorizationFilter(DpsHeaders headers) {
        this.headers = headers;
    }

    public boolean hasPermission(String... requiredRoles) {
        AuthorizationResponse authResponse = authorizationService.authorizeAny(headers, requiredRoles);
        headers.put(DpsHeaders.USER_EMAIL, authResponse.getUser());
        return true;
    }
}
