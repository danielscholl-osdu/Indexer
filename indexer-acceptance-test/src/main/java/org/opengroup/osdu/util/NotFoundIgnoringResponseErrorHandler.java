package org.opengroup.osdu.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;

public class NotFoundIgnoringResponseErrorHandler extends DefaultResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return super.hasError(response) && response.getRawStatusCode() != HttpStatus.NOT_FOUND.value();
    }

    @Override
    protected boolean hasError(HttpStatusCode statusCode) {
        return super.hasError(statusCode) && statusCode != HttpStatus.NOT_FOUND;
    }
}
