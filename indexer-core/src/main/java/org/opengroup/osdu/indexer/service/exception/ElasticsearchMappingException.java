package org.opengroup.osdu.indexer.service.exception;

import lombok.Getter;

@Getter
public class ElasticsearchMappingException extends RuntimeException {
    private Integer status;
    public ElasticsearchMappingException(final String message, Integer status) {
        super(message);
        this.status = status;
    }

}
