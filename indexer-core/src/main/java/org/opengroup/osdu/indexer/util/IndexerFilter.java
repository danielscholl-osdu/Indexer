
package org.opengroup.osdu.indexer.util;

import java.io.IOException;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;
import org.opengroup.osdu.core.common.http.ResponseHeadersFactory;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log
@Component
public class IndexerFilter implements Filter {

    private final DpsHeaders dpsHeaders;
    private ResponseHeadersFactory responseHeadersFactory = new ResponseHeadersFactory();

    // defaults to * for any front-end, string must be comma-delimited if more than one domain
    @Value("${ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS:*}")
    String ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS;

    @Autowired
    public IndexerFilter(DpsHeaders dpsHeaders) {
        this.dpsHeaders = dpsHeaders;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        dpsHeaders.addCorrelationIdIfMissing();

        setResponseHeaders(httpResponse);

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void setResponseHeaders(HttpServletResponse httpServletResponse) {
        Map<String, String> responseHeaders = responseHeadersFactory.getResponseHeaders(ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS);
        for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
            httpServletResponse.addHeader(header.getKey(), header.getValue());
        }
        httpServletResponse.addHeader(DpsHeaders.CORRELATION_ID, this.dpsHeaders.getCorrelationId());
    }

    @Override
    public void destroy() {
    }

}
