package org.opengroup.osdu.indexer.model;

public class Constants {
    // It should be moved to core common later
    public static final String ANCESTRY_KINDS = "ancestry_kinds";

    // Specifications using kind as key is not partition safe if the specifications are per data partition
    public static final int SPEC_CACHE_EXPIRATION = 600;
    public static final int SPEC_MAX_CACHE_SIZE = 2000;

    // Data id itself is partition safe
    public static final int DATA_CACHE_EXPIRATION = 120;
    public static final int DATA_MAX_CACHE_SIZE = 1000;
}
