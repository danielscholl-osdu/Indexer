Feature: Indexing of the documents
  This feature deals to check for index deletion after schema deletion.

  Background:
    Given the schema is created with the following kind
      | kind                                    | index                                   | schemaFile      |
      | tenant1:testindex<timestamp>:well:1.0.0 | tenant1-testindex<timestamp>-well-1.0.0 | index_records_1 |

  Scenario Outline: Index creation and deletion in the Elastic Search
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I check that the index for <kind> has been created
    Then I should delete the records I created earlier
    Then I should delete the schema for <kind> I created earlier
    Then I should check that the index for <kind> has not been deleted
    Then I should to run cleanup of indexes for <kind> and <message>
    Then I should check that the index for <kind> has been deleted

    Examples:
      | kind                                      | recordFile        | acl                            | message                                                                                                                                                                                                                                                            |
      | "tenant1:testindex<timestamp>:well:1.0.0" | "index_records_1" | "data.default.viewers@tenant1" | "{"data":"[{\"id\":\"%s-d9033ae1-fb15-496c-9ba0-880fd1d2b2cf\",\"kind\":\"%s\",\"op\":\"purge_schema\"}]","attributes":{"account-id":"opendes","correlation-id":"b5a281bd-f59d-4db2-9939-b2d85036fc7e"},"messageId":"%s","publishTime":"2018-05-08T21:48:56.131Z"}"|
