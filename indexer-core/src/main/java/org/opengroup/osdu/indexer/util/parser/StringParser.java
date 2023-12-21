package org.opengroup.osdu.indexer.util.parser;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class StringParser {

    public String parseString(String attributeName, Object attributeVal) {
        return attributeVal == null ? null : String.valueOf(attributeVal);
    }

}
