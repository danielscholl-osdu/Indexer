package org.opengroup.osdu.indexer.middleware;

import com.google.common.base.Strings;
import lombok.extern.java.Log;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.http.ResponseHeaders;
import org.opengroup.osdu.core.common.model.search.DeploymentEnvironment;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Log
@Component
public class IndexFilter implements Filter {

    @Inject
    private DpsHeaders dpsHeaders;

    @Inject
    private IRequestInfo requestInfo;

    @Value("${DEPLOYMENT_ENVIRONMENT}")
    private String DEPLOYMENT_ENVIRONMENT;

    private FilterConfig filterConfig;

    private static final String PATH_SWAGGER = "/swagger.json";
    private static final String PATH_TASK_HANDLERS = "task-handlers";
    private static final String PATH_CRON_HANDLERS = "cron-handlers";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String uri = httpRequest.getRequestURI().toLowerCase();

        if (httpRequest.getMethod().equalsIgnoreCase(HttpMethod.POST.name()) && uri.contains(PATH_TASK_HANDLERS)) {
            if (DeploymentEnvironment.valueOf(DEPLOYMENT_ENVIRONMENT) != DeploymentEnvironment.LOCAL) {
                checkWorkerApiAccess(requestInfo);
            }
        }

        if (httpRequest.getMethod().equalsIgnoreCase(HttpMethod.GET.name()) && uri.contains(PATH_CRON_HANDLERS)) {
            checkWorkerApiAccess(requestInfo);
        }

//        if (!httpRequest.isSecure()) {
//            throw new AppException(302, "Redirect", "HTTP is not supported. Use HTTPS.");
//        }

        filterChain.doFilter(servletRequest, servletResponse);

        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        Map<String, List<Object>> standardHeaders = ResponseHeaders.STANDARD_RESPONSE_HEADERS;
        for (Map.Entry<String, List<Object>> header : standardHeaders.entrySet()) {
            httpResponse.addHeader(header.getKey(), header.getValue().toString());
        }
        if (httpResponse.getHeader(DpsHeaders.CORRELATION_ID) == null) {
            httpResponse.addHeader(DpsHeaders.CORRELATION_ID, dpsHeaders.getCorrelationId());
        }

    }

    @Override
    public void destroy() {
    }

    private void checkWorkerApiAccess(IRequestInfo requestInfo) {
        if (requestInfo.isTaskQueueRequest()) return;
        throw AppException.createForbidden("invalid user agent, AppEngine Task Queue only");
    }

    private List<String> validateAccountId(DpsHeaders requestHeaders) {
        String accountHeader = requestHeaders.getPartitionIdWithFallbackToAccountId();
        String debuggingInfo = String.format("%s:%s", DpsHeaders.DATA_PARTITION_ID, accountHeader);

        if (Strings.isNullOrEmpty(accountHeader)) {
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Bad request", "invalid or empty data partition", debuggingInfo);
        }

        List<String> dataPartitions = Arrays.asList(accountHeader.trim().split("\\s*,\\s*"));
        if (dataPartitions.isEmpty() || dataPartitions.size() > 1) {
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Bad request", "invalid or empty data partition", debuggingInfo);
        }
        return dataPartitions;
    }

}
