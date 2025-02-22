package org.opengroup.osdu.response;

import lombok.Data;

import javax.ws.rs.core.MultivaluedMap;

@Data
public abstract class ResponseBase {
    private int responseCode;
    private MultivaluedMap<String, String> headers;
}