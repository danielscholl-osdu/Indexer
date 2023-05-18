package org.opengroup.osdu.util.conf;

import java.util.Optional;

public class AnthosConfig {

    private static final String GROUP_ID_VARIABLE = "GROUP_ID";
    private static final String ENTITLEMENTS_DOMAIN_VARIABLE = "ENTITLEMENTS_DOMAIN";

    public static void updateEntitlementsDomainVariable() {
        String groupId = Optional.ofNullable(System.getProperty(GROUP_ID_VARIABLE, System.getenv(GROUP_ID_VARIABLE)))
                .orElse("group");
        System.setProperty(ENTITLEMENTS_DOMAIN_VARIABLE, groupId);
    }
}
