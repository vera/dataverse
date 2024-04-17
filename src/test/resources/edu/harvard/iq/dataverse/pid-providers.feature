Feature: Moving datasets between dataverses with different PID Providers

Background:
  Given a dataverse with alias "dv-permalink" configured with a Permalink PID provider
  And a dataverse with alias "dv-doi" configured with a DOI PID provider

Scenario: Moving unpublished dataset to dataverse with another PID provider
  Given an unpublished dataset in the dataverse with alias "dv-permalink"
  When I move that dataset to the dataverse with alias "dv-doi"
  And I publish that dataset
  Then the dataset's PID should be a DOI

Scenario: Moving published dataset to dataverse with another PID provider
  Given an unpublished dataset in the dataverse with alias "dv-permalink"
  When I publish that dataset
  And I move that dataset to the dataverse with alias "dv-doi"
  Then the dataset's PID should be a Permalink