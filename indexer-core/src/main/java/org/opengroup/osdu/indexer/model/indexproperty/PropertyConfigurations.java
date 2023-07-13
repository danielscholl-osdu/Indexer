/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.model.indexproperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PropertyConfigurations {
    @JsonProperty("Name")
    private String name;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("AttributionAuthority")
    private String attributionAuthority;

    @JsonProperty("Configurations")
    private List<PropertyConfiguration> configurations;

    public List<String> getUniqueRelatedObjectKinds() {
        if(configurations == null || configurations.isEmpty())
            return new ArrayList<>();

        Set<String> relatedObjectKinds = new HashSet<>();
        for(PropertyConfiguration configuration : configurations) {
            String relatedObjectKind = configuration.getRelatedObjectKind();
            if(!Strings.isNullOrEmpty(relatedObjectKind)) {
                relatedObjectKinds.add(relatedObjectKind);
            }
        }
        return new ArrayList<>(relatedObjectKinds);
    }

    public boolean hasValidCode() {
        // It is just basic test to detect mistake
        if(Strings.isNullOrEmpty(this.code)) {
            return false;
        }

        String[] parts = this.code.split(":");
        if(parts.length != 4) {
            return false;
        }
        // Version must be ended with dot and major version only
        // e.g. "Code": "osdu:wks:master-data--Well:1."
        String version = parts[3];
        return (version.length() > 1 && version.indexOf(".") == version.length() - 1);
    }

    public boolean hasValidConfigurations() {
        if(configurations == null || configurations.isEmpty()) {
            return false;
        }

        return (configurations.stream().filter(config -> config.isValid()).count() > 0);
    }

    public boolean hasInvalidConfigurations() {
        if(configurations == null || configurations.isEmpty()) {
            return false;
        }

        return (configurations.stream().filter(config -> !config.isValid()).count() > 0);
    }

    public boolean isValid() {
        return hasValidCode() && hasValidConfigurations();
    }
}
