package org.opengroup.osdu.indexer.provider.gcp.web.util;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class AppExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Object> handleAppExceptions(AppException e) {
        return this.getErrorResponse(e);
    }

    private ResponseEntity<Object> getErrorResponse(AppException e) {

        String exceptionMsg = Objects.nonNull(e.getOriginalException())
            ? e.getOriginalException().getMessage()
            : e.getError().getMessage();

        Integer errorCode = e.getError().getCode();

        if (errorCode > 499) {
            log.error(exceptionMsg, e.getOriginalException());
        } else {
            log.warn(exceptionMsg, e.getOriginalException());
        }

        HttpStatus status = Objects.nonNull(HttpStatus.resolve(errorCode))
            ? HttpStatus.resolve(errorCode)
            : resolveNotSupportedStatus(errorCode);

        return new ResponseEntity<>(e.getError(), status);
    }

    //Currently not all codes provided from core can be resolved by HttpStatus
    //example org.opengroup.osdu.core.common.model.http.RequestStatus have not supported by HttpStatus codes
    private HttpStatus resolveNotSupportedStatus(int statusCode) {
        if (statusCode > 99 && statusCode < 200) {
            return HttpStatus.CONTINUE;
        }
        if (statusCode > 199 && statusCode < 300) {
            return HttpStatus.NO_CONTENT;
        }
        if (statusCode > 299 && statusCode < 400) {
            return HttpStatus.MULTIPLE_CHOICES;
        }
        if (statusCode > 399 && statusCode < 500) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
