package org.opengroup.osdu.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ErrorResponseMock extends ResponseBase {
    private List<String> errors;
    private String code;
    private String reason;
    private String message;
}