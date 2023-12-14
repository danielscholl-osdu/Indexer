Feature: Indexing of the documents
  This feature deals with validation of the documents in Elastic Search ingested with different kinds and attributes.

  Background:
    Given the schema is created with the following kind
      | kind                                                | index                                               | schemaFile                     |
      | tenant1:indexer:test-data--Integration:1.0.2        | tenant1-indexer-test-data--integration-1.0.2        | index_records_1                |
      | tenant1:indexer:test-data--Integration:2.0.1        | tenant1-indexer-test-data--integration-2.0.1        | index_records_2                |
      | tenant1:indexer:test-data--Integration:3.0.1        | tenant1-indexer-test-data--integration-3.0.1        | index_records_3                |
      | tenant1:wks:master-data--Wellbore:2.0.3             | tenant1-wks-master-data--wellbore-2.0.3             | r3-index_record_wks_master     |
      | tenant1:wks:ArraysOfObjectsTestCollection:4.0.0     | tenant1-wks-arraysofobjectstestcollection-4.0.0     | r3-index_record_arrayofobjects |
      | tenant1:indexer:test-mapping--Sync:2.0.0            | tenant1-indexer-test-mapping--sync-2.0.0            | index_record_sync_mapping      |
      | tenant1:indexer:test-update-data--Integration:1.0.1 | tenant1-indexer-test-update-data--integration-1.0.1 | index_update_records_kind_v1   |
      | tenant1:indexer:test-update-data--Integration:2.0.1 | tenant1-indexer-test-update-data--integration-2.0.1 | index_update_records_kind_v2   |
      | tenant1:indexer:virtual-properties-Integration:1.0.0 | tenant1-indexer-virtual-properties-integration-1.0.0 | index_record_virtual_properties   |
      | tenant1:indexer:decimation-Integration:1.0.0        | tenant1-indexer-decimation-integration-1.0.0        | index_record_seismic_survey    |
      | osdu:wks:reference-data--IndexPropertyPathConfiguration:1.0.0 | osdu-wks-reference-data--indexpropertypathconfiguration-1.0.0 | osdu_wks_IndexPropertyPathConfiguration_v1 |
      | test:indexer:index-property--Wellbore:1.0.0         | test-indexer-index-property--wellbore-1.0.0         | index-property-wellbore_v1     |
      | test:indexer:index-property--WellLog:1.0.0          | test-indexer-index-property--welllog-1.0.0          | index-property-welllog_v1      |

  @indexer-extended
  Scenario Outline: Prepare the index property configuration records and clean up index of the extended kinds in the Elastic Search
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I should get the <number> documents for the <index> in the Elastic Search
    Then I clean up the index of the extended kinds <extendedKinds> in the Elastic Search
    Then I set starting stateful scenarios

    Examples:
      | kind                                                            | recordFile                                   | number | index                                                           | acl                            | extendedKinds                                                                              |
      | "osdu:wks:reference-data--IndexPropertyPathConfiguration:1.0.0" | "osdu_wks_IndexPropertyPathConfiguration_v1" | 2      | "osdu-wks-reference-data--indexpropertypathconfiguration-1.0.0" | "data.default.viewers@tenant1" | "test:indexer:index-property--Wellbore:1.0.0,test:indexer:index-property--WellLog:1.0.0"  |

  @indexer-extended
  Scenario Outline: Ingest the records of the extended kinds, Index in the Elastic Search and Search string field
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I should be able to search <number> record with index <index> by extended data field <field> and value <value>

    Examples:
      | kind                                           | recordFile                    | number | index                                           | acl                            |  field               | value           |
      | "test:indexer:index-property--Wellbore:1.0.0"  | "index-property-wellbore_v1"  | 1      |  "test-indexer-index-property--wellbore-1.0.0"  | "data.default.viewers@tenant1" | "data.WellUWI"       | "123454321"     |
      | "test:indexer:index-property--WellLog:1.0.0"   | "index-property-welllog_v1"   | 1      |  "test-indexer-index-property--welllog-1.0.0"   | "data.default.viewers@tenant1" | "data.WellboreName"  | "Facility_123"  |

  @indexer-extended
  Scenario Outline: Ingest the records of the extended kinds, Index in the Elastic Search and Search spatial field
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I should be able search <number> documents for the <index> by bounding box query with points (<top_left_latitude>, <top_left_longitude>) and  (<bottom_right_latitude>, <bottom_right_longitude>) on field <field>

    Examples:
      | kind                                           | recordFile                    | number | index                                           | acl                            |  field                 | top_left_latitude | top_left_longitude | bottom_right_latitude | bottom_right_longitude |
      | "test:indexer:index-property--Wellbore:1.0.0"  | "index-property-wellbore_v1"  | 1      |  "test-indexer-index-property--wellbore-1.0.0"  | "data.default.viewers@tenant1" | "data.Location"        | 30                | -96                | 29                    | -95                    |
      | "test:indexer:index-property--WellLog:1.0.0"   | "index-property-welllog_v1"   | 1      |  "test-indexer-index-property--welllog-1.0.0"   | "data.default.viewers@tenant1" | "data.SpatialLocation" | 30                | -96                | 29                    | -95                    |

  @indexer-extended
  Scenario: End Stateful Scenarios
    Then I set ending stateful scenarios

  @default
  Scenario Outline: Ingest the record and Index in the Elastic Search
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I should get the <number> documents for the <index> in the Elastic Search
    Then I should get the elastic <mapping> for the <kind> and <index> in the Elastic Search
    Then I can validate indexed meta attributes for the <index> and given <kind>

    Examples:
      | kind                                           | recordFile               | number | index                                          | acl                            | mapping                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
      | "tenant1:indexer:test-data--Integration:1.0.2" | "index_records_schema_1" | 5      | "tenant1-indexer-test-data--integration-1.0.2" | "data.default.viewers@tenant1" | "{"mappings":{"well":{"dynamic":"false","properties":{"acl":{"properties":{"owners":{"type":"keyword"},"viewers":{"type":"keyword"}}},"ancestry":{"properties":{"parents":{"type":"keyword"}}},"data":{"properties":{"Basin":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"Country":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"County":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"EmptyAttribute":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"Established":{"type":"date"},"Field":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"Location":{"type":"geo_point"},"OriginalOperator":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"Rank":{"type":"integer"},"Score":{"type":"integer"},"State":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"WellName":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"WellStatus":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"WellType":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"DblArray":{"type":"double"},"TextArray":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}}}},"id":{"type":"keyword"},"index":{"properties":{"lastUpdateTime":{"type":"date"},"statusCode":{"type":"integer"},"trace":{"type":"text"}}},"kind":{"type":"keyword"},"legal":{"properties":{"legaltags":{"type":"keyword"},"otherRelevantDataCountries":{"type":"keyword"},"status":{"type":"keyword"}}},"namespace":{"type":"keyword"},"type":{"type":"keyword"},"authority":{"type":"constant_keyword","value":"<authority-id>"},"createTime":{"type":"date"},"createUser":{"type":"keyword"},"modifyTime":{"type":"date"},"modifyUser":{"type":"keyword"},"source":{"type":"constant_keyword","value":"<source-id>"},"version":{"type":"long"},"x-acl":{"type":"keyword"}}}}}" |
      | "tenant1:indexer:test-data--Integration:3.0.1" | "index_records_schema_3" | 7      | "tenant1-indexer-test-data--integration-3.0.1" | "data.default.viewers@tenant1" | "{"mappings":{"well":{"dynamic":"false","properties":{"acl":{"properties":{"owners":{"type":"keyword"},"viewers":{"type":"keyword"}}},"ancestry":{"properties":{"parents":{"type":"keyword"}}},"data":{"properties":{"Basin":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"Country":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"County":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"EmptyAttribute":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"Established":{"type":"date"},"Field":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"Location":{"type":"geo_point"},"OriginalOperator":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"Rank":{"type":"integer"},"Score":{"type":"integer"},"State":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"WellName":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"WellStatus":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"WellType":{"type":"text","fields":{"keyword":{"type":"keyword","null_value":"null","ignore_above":256}}},"DblArray":{"type":"double"}}},"id":{"type":"keyword"},"index":{"properties":{"lastUpdateTime":{"type":"date"},"statusCode":{"type":"integer"},"trace":{"type":"text"}}},"kind":{"type":"keyword"},"legal":{"properties":{"legaltags":{"type":"keyword"},"otherRelevantDataCountries":{"type":"keyword"},"status":{"type":"keyword"}}},"namespace":{"type":"keyword"},"type":{"type":"keyword"},"authority":{"type":"constant_keyword","value":"<authority-id>"},"createTime":{"type":"date"},"createUser":{"type":"keyword"},"modifyTime":{"type":"date"},"modifyUser":{"type":"keyword"},"source":{"type":"constant_keyword","value":"<source-id>"},"version":{"type":"long"},"x-acl":{"type":"keyword"}}}}}"                                                                                                            |

  @default
  Scenario Outline: Ingest the record and Index in the Elastic Search with bad attribute
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I should get the <number> documents for the <index> in the Elastic Search with out <skippedAttribute>

    Examples:
      | kind                                           | recordFile        | number | index                                          | skippedAttribute      | acl                            |
      | "tenant1:indexer:test-data--Integration:2.0.1" | "index_records_2" | 4      | "tenant1-indexer-test-data--integration-2.0.1" | "data.Location"       | "data.default.viewers@tenant1" |
      | "tenant1:indexer:test-data--Integration:2.0.1" | "index_records_2" | 1      | "tenant1-indexer-test-data--integration-2.0.1" | "data.InvalidInteger" | "data.default.viewers@tenant1" |

  @default
  Scenario Outline: Ingest the record and Index in the Elastic Search with tags
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I should be able to search <number> record with index <index> by tag <tagKey> and value <tagValue>

    Examples:
      | kind                                           | recordFile               | index                                          | acl                            | tagKey    | tagValue    | number |
      | "tenant1:indexer:test-data--Integration:1.0.2" | "index_records_schema_1" | "tenant1-indexer-test-data--integration-1.0.2" | "data.default.viewers@tenant1" | "testtag" | "testvalue" | 5      |

  @default
  Scenario Outline: Ingest the r3-record with geo-shape and Index in the Elastic Search
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I should be able search <number> documents for the <index> by bounding box query with points (<top_left_latitude>, <top_left_longitude>) and  (<bottom_right_latitude>, <bottom_right_longitude>) on field <field>

    Examples:
      | kind                                      | recordFile                   | number | index                                     | acl                            | field                                   | top_left_latitude | top_left_longitude | bottom_right_latitude | bottom_right_longitude |
      | "tenant1:wks:master-data--Wellbore:2.0.3" | "r3-index_record_wks_master" | 1      | "tenant1-wks-master-data--wellbore-2.0.3" | "data.default.viewers@tenant1" | "data.SpatialLocation.Wgs84Coordinates" | 52                | -100               | 0                     | 100                    |

  @default
  Scenario Outline: Ingest records with geo-shape and Index with virtual properties in the Elastic Search
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I should be able search <number> documents for the <index> by bounding box query with points (<top_left_latitude>, <top_left_longitude>) and  (<bottom_right_latitude>, <bottom_right_longitude>) on field <field>

    Examples:
      | kind                                                    | recordFile                        | number | index                                                  | acl                            | field                                                      | top_left_latitude | top_left_longitude | bottom_right_latitude | bottom_right_longitude |
      | "tenant1:indexer:virtual-properties-Integration:1.0.0"  | "index_record_virtual_properties" | 3      | "tenant1-indexer-virtual-properties-integration-1.0.0" | "data.default.viewers@tenant1" | "data.VirtualProperties.DefaultLocation.Wgs84Coordinates"  | 90                | -180               | -90                   | 180                    |
      | "tenant1:indexer:decimation-Integration:1.0.0"          | "index_record_seismic_survey"     | 1       | "tenant1-indexer-decimation-integration-1.0.0"         | "data.default.viewers@tenant1" | "data.VirtualProperties.DefaultLocation.Wgs84Coordinates"  | 90                | -180               | -90                   | 180                    |

  @default
  Scenario Outline: Ingest the r3-record with arrays of objects and hints in schema and Index in the Elastic Search
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I should be able search <number> documents for the <index> by nested <path> and properties (<first_nested_field>, <first_nested_value>) and  (<second_nested_field>, <second_nested_value>)
    Then I should be able search <number> documents for the <index> by flattened inner properties (<flattened_inner_field>, <flattened_inner_value>)
    Then I should get <object_inner_field> in response, without hints in schema for the <index> that present in the <recordFile> with <acl> for a given <kind>

    Examples:
      | kind                                              | recordFile                       | number | index                                             | acl                            | path              | first_nested_field           | first_nested_value | second_nested_field          | second_nested_value | flattened_inner_field           | flattened_inner_value | object_inner_field |
      | "tenant1:wks:ArraysOfObjectsTestCollection:4.0.0" | "r3-index_record_arrayofobjects" | 1      | "tenant1-wks-arraysofobjectstestcollection-4.0.0" | "data.default.viewers@tenant1" | "data.NestedTest" | "data.NestedTest.NumberTest" | 12345              | "data.NestedTest.StringTest" | "test string"       | "data.FlattenedTest.StringTest" | "test string"         | "ObjectTest"       |

  @as-ingested-coordinates
  Scenario Outline: Ingest the record and Index in the Elastic with AsIngestedCoordinates properties
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I should be able search <number> documents for the <index> by flattened inner properties (<flattened_inner_field>, <flattened_inner_value>)
    Then I should get <inner_field> in search response for the <index>

    Examples:
      | kind                                                   | recordFile                        | number | index                                                  | acl                            | flattened_inner_field                                                                 | flattened_inner_value                                                     | inner_field                                                                  |
      | "tenant1:wks:master-data--Wellbore:2.0.3"              | "r3-index_record_wks_master"      | 1      | "tenant1-wks-master-data--wellbore-2.0.3"              | "data.default.viewers@tenant1" | "data.ProjectedBottomHoleLocation.AsIngestedCoordinates.CoordinateReferenceSystemID"  | "osdu:reference-data--CoordinateReferenceSystem:ED_1950_UTM_Zone_31N:"    | "ProjectedBottomHoleLocation.AsIngestedCoordinates.persistableReferenceCrs"  |
      | "tenant1:wks:master-data--Wellbore:2.0.3"              | "r3-index_record_wks_master"      | 1      | "tenant1-wks-master-data--wellbore-2.0.3"              | "data.default.viewers@tenant1" | "data.SpatialLocation.AsIngestedCoordinates.CoordinateReferenceSystemID"              | "osdu:reference-data--CoordinateReferenceSystem:ED_1950_UTM_Zone_31N:"    | "SpatialLocation.AsIngestedCoordinates.persistableReferenceCrs"              |
      | "tenant1:indexer:virtual-properties-Integration:1.0.0" | "index_record_virtual_properties" | 1      | "tenant1-indexer-virtual-properties-integration-1.0.0" | "data.default.viewers@tenant1" | "data.ProjectedBottomHoleLocation.AsIngestedCoordinates.CoordinateReferenceSystemID"  | "osdu:reference-data--CoordinateReferenceSystem:WGS_1984_World_Mercator:" | "ProjectedBottomHoleLocation.AsIngestedCoordinates.persistableReferenceCrs"  |
      | "tenant1:indexer:virtual-properties-Integration:1.0.0" | "index_record_virtual_properties" | 2      | "tenant1-indexer-virtual-properties-integration-1.0.0" | "data.default.viewers@tenant1" | "data.GeographicBottomHoleLocation.AsIngestedCoordinates.CoordinateReferenceSystemID" | "osdu:reference-data--CoordinateReferenceSystem:WGS_1984_World_Mercator:" | "GeographicBottomHoleLocation.AsIngestedCoordinates.persistableReferenceCrs" |
      | "tenant1:indexer:virtual-properties-Integration:1.0.0" | "index_record_virtual_properties" | 3      | "tenant1-indexer-virtual-properties-integration-1.0.0" | "data.default.viewers@tenant1" | "data.SpatialLocation.AsIngestedCoordinates.CoordinateReferenceSystemID"              | "osdu:reference-data--CoordinateReferenceSystem:WGS_1984_World_Mercator:" | "SpatialLocation.AsIngestedCoordinates.persistableReferenceCrs"              |

  @as-ingested-coordinates
  Scenario Outline: Ingest the record and Index in the Elastic with AsIngestedCoordinates properties and search by coordinates
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I should be able search <number> documents for the <index> by bounding box query with points (<topPointX>, <bottomPointX>) on field <pointX> and points (<topPointY>, <bottomPointY>) on field <pointY>

    Examples:
      | kind                                                   | recordFile                        | number | index                                                  | acl                            | pointX                                                                | topPointX | bottomPointX | pointY                                                                | topPointY | bottomPointY |
      | "tenant1:wks:master-data--Wellbore:2.0.3"              | "r3-index_record_wks_master"      | 1      | "tenant1-wks-master-data--wellbore-2.0.3"              | "data.default.viewers@tenant1" | "data.ProjectedBottomHoleLocation.AsIngestedCoordinates.FirstPoint.X" | 710000    | 700000       | "data.ProjectedBottomHoleLocation.AsIngestedCoordinates.FirstPoint.Y" | 5800000   | 5700000      |
      | "tenant1:indexer:virtual-properties-Integration:1.0.0" | "index_record_virtual_properties" | 1      | "tenant1-indexer-virtual-properties-integration-1.0.0" | "data.default.viewers@tenant1" | "data.ProjectedBottomHoleLocation.AsIngestedCoordinates.FirstPoint.X" | 2600000   | 2500000      | "data.ProjectedBottomHoleLocation.AsIngestedCoordinates.FirstPoint.Y" | -3500000  | -3600000     |
      | "tenant1:indexer:decimation-Integration:1.0.0"         | "index_record_seismic_survey"     | 1      | "tenant1-indexer-decimation-integration-1.0.0"         | "data.default.viewers@tenant1" | "data.SpatialLocation.AsIngestedCoordinates.FirstPoint.X"             | 1151940   | 1151930      | "data.SpatialLocation.AsIngestedCoordinates.FirstPoint.Y"             | 4843810   | 4843800      |

  @default
  Scenario Outline: Synchronize meta attribute mapping on existing indexed kind
    When I create index with <mappingFile> for a given <index> and <kind>
    And  I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I can validate indexed meta attributes for the <index> and given <kind>

    Examples:
      | kind                                       | index                                      | recordFile                  | mappingFile                 | acl                            |
      | "tenant1:indexer:test-mapping--Sync:2.0.0" | "tenant1-indexer-test-mapping--sync-2.0.0" | "index_record_sync_mapping" | "index_record_sync_mapping" | "data.default.viewers@tenant1" |

  @default
  Scenario Outline: Ingest the record and Index in the Elastic Search
    When I ingest records with the <recordFile> with <acl> for a given <kind_v1>
    Then I should get the 1 documents for the <index_v1> in the Elastic Search
    Then I ingest records with the <recordFile> with <acl> for a given <kind_v2>
    Then I should get the 1 documents for the <index_v2> in the Elastic Search
    Then I should not get any documents for the <index_v1> in the Elastic Search

    Examples:
      | kind_v1                                               | index_v1                                              | recordFile                     | acl                            | kind_v2                                               | index_v2                                              |
      | "tenant1:indexer:test-update-data--Integration:1.0.1" | "tenant1-indexer-test-update-data--integration-1.0.1" | "index_update_records_kind_v1" | "data.default.viewers@tenant1" | "tenant1:indexer:test-update-data--Integration:2.0.1" | "tenant1-indexer-test-update-data--integration-2.0.1" |
