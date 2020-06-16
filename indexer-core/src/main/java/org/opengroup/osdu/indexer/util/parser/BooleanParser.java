package org.opengroup.osdu.indexer.util.parser;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class BooleanParser {

    public boolean parseBoolean(String attributeName, Object attributeVal) {
        String val = attributeVal == null ? null : String.valueOf(attributeVal);
        return Boolean.parseBoolean(val);
    }

}
