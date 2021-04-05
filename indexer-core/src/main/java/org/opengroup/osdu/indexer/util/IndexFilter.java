
package org.opengroup.osdu.indexer.util;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Log
@Component
public class IndexFilter implements Filter {

    private final DpsHeaders dpsHeaders;

    @Autowired
    public IndexFilter(DpsHeaders dpsHeaders) {
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
        httpResponse.addHeader(DpsHeaders.CORRELATION_ID, dpsHeaders.getCorrelationId());

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
    }

}
