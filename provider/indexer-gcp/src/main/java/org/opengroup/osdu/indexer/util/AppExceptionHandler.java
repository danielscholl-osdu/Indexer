package org.opengroup.osdu.indexer.util;

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

		if (e.getError().getCode() > 499) {
			log.error(exceptionMsg, e.getOriginalException());
		} else {
			log.warn(exceptionMsg, e.getOriginalException());
		}

		return new ResponseEntity<>(e.getError(), HttpStatus.resolve(e.getError().getCode()));
	}
}
