// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexer.util;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.inject.Inject;

import com.google.common.base.Strings;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

import org.opengroup.osdu.core.gcp.model.CloudTaskHeaders;
import org.opengroup.osdu.core.gcp.util.TraceIdExtractor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@Primary
public class DpsHeaderFactoryGcp extends DpsHeaders {

    @Inject
    public DpsHeaderFactoryGcp(HttpServletRequest request) {

        Map<String, String> headers = Collections
                .list(request.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(h -> h, request::getHeader));

        String traceContext = headers.get(CloudTaskHeaders.CLOUD_TRACE_CONTEXT);

        if(!Strings.isNullOrEmpty(traceContext)){
            headers.put(CloudTaskHeaders.TRACE_ID, TraceIdExtractor.getTraceId(traceContext));
        }

        this.addFromMap(headers);

        // Add Correlation ID if missing
        this.addCorrelationIdIfMissing();
    }
}