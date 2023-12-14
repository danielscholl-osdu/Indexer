Feature: Fetch info about maven build and git repository.

  @default
  Scenario: Verify version info endpoint content
    When I send get request to version info endpoint
    Then I should get version info in response
