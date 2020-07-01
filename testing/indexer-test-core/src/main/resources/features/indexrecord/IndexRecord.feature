Feature: Indexing of the documents
  This feature deals with validation of the documents in Elastic Search ingested with different kinds and attributes.

  Background:
    Given the schema is created with the following kind
      | kind                                    | index                                   | schemaFile      |
      | tenant1:testindex<timestamp>:well:1.0.0 | tenant1-testindex<timestamp>-well-1.0.0 | index_records_1 |
      | tenant1:testindex<timestamp>:well:2.0.0 | tenant1-testindex<timestamp>-well-2.0.0 | index_records_2 |
      | tenant1:testindex<timestamp>:well:3.0.0 | tenant1-testindex<timestamp>-well-3.0.0 | index_records_3 |

  Scenario Outline: Ingest the record and Index in the Elastic Search
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I should get the <number> documents for the <index> in the Elastic Search
    Then I should get the elastic <mapping> for the <type> and <index> in the Elastic Search

    Examples:
      | kind                                      | recordFile        | number | index                                     | type   | acl                                    | mapping                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
      | "tenant1:testindex<timestamp>:well:1.0.0" | "index_records_1" | 5      | "tenant1-testindex<timestamp>-well-1.0.0" | "well" | "data.default.viewers@opendes" | "{"mappings":{"well":{"dynamic":"false","properties":{"acl":{"properties":{"owners":{"type":"keyword"},"viewers":{"type":"keyword"}}},"ancestry":{"properties":{"parents":{"type":"keyword"}}},"data":{"properties":{"Basin":{"type":"text"},"Country":{"type":"text"},"County":{"type":"text"},"EmptyAttribute":{"type":"text"},"Established":{"type":"date"},"Field":{"type":"text"},"Location":{"type":"geo_point"},"OriginalOperator":{"type":"text"},"Rank":{"type":"integer"},"Score":{"type":"integer"},"State":{"type":"text"},"WellName":{"type":"text"},"WellStatus":{"type":"text"},"WellType":{"type":"text"},"DblArray":{"type":"double"}}},"id":{"type":"keyword"},"index":{"properties":{"lastUpdateTime":{"type":"date"},"statusCode":{"type":"integer"},"trace":{"type":"text"}}},"kind":{"type":"keyword"},"legal":{"properties":{"legaltags":{"type":"keyword"},"otherRelevantDataCountries":{"type":"keyword"},"status":{"type":"keyword"}}},"namespace":{"type":"keyword"},"type":{"type":"keyword"},"version":{"type":"long"},"x-acl":{"type":"keyword"}}}}}" |
      | "tenant1:testindex<timestamp>:well:3.0.0" | "index_records_1" | 5      | "tenant1-testindex<timestamp>-well-3.0.0" | "well" | "data.default.viewers@opendes" | "{"mappings":{"well":{"dynamic":"false","properties":{"acl":{"properties":{"owners":{"type":"keyword"},"viewers":{"type":"keyword"}}},"ancestry":{"properties":{"parents":{"type":"keyword"}}},"data":{"properties":{"Basin":{"type":"text"},"Country":{"type":"text"},"County":{"type":"text"},"EmptyAttribute":{"type":"text"},"Established":{"type":"date"},"Field":{"type":"text"},"Location":{"type":"geo_point"},"OriginalOperator":{"type":"text"},"Rank":{"type":"integer"},"Score":{"type":"integer"},"State":{"type":"text"},"WellName":{"type":"text"},"WellStatus":{"type":"text"},"WellType":{"type":"text"},"DblArray":{"type":"double"}}},"id":{"type":"keyword"},"index":{"properties":{"lastUpdateTime":{"type":"date"},"statusCode":{"type":"integer"},"trace":{"type":"text"}}},"kind":{"type":"keyword"},"legal":{"properties":{"legaltags":{"type":"keyword"},"otherRelevantDataCountries":{"type":"keyword"},"status":{"type":"keyword"}}},"namespace":{"type":"keyword"},"type":{"type":"keyword"},"version":{"type":"long"},"x-acl":{"type":"keyword"}}}}}" |

  Scenario Outline: Ingest the record and Index in the Elastic Search with bad attribute
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I should get the <number> documents for the <index> in the Elastic Search with out <skippedAttribute>

    Examples:
      | kind                                      | recordFile        | number | index                                     | skippedAttribute | acl                            |
      | "tenant1:testindex<timestamp>:well:2.0.0" | "index_records_2" | 4      | "tenant1-testindex<timestamp>-well-2.0.0" | "data.Location"  | "data.default.viewers@opendes" |
      | "tenant1:testindex<timestamp>:well:3.0.0" | "index_records_3" | 7      | "tenant1-testindex<timestamp>-well-3.0.0" | "data.GeoShape"  | "data.default.viewers@opendes" |
