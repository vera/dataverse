## Rate Limiting using JCache (with Hazelcast as provided by Payara)
The option to rate limit has been added to prevent users from over taxing the system either deliberately or by runaway automated processes.
Rate limiting can be configured on a tier level with tier 0 being reserved for guest users and tiers 1-any for authenticated users.
Superuser accounts are exempt from rate limiting.
Rate limits can be imposed on command APIs by configuring the tier, the command, and the hourly limit in the database.
Two database settings configure the rate limiting.
Note: If either of these settings exist in the database rate limiting will be enabled.
If neither setting exists rate limiting is disabled.

`:RateLimitingDefaultCapacityTiers` is a comma separated list of default values for each tier.
In the following example, the default for tier `0` (guest users) is set to 10,000 calls per command per hour and tier `1` (authenticated users) is set to 20,000 calls per command per hour.
Tiers not specified in this setting will default to `-1` (No Limit). I.e., -d "10000" is equivalent to -d "10000,-1,-1,..."
`curl http://localhost:8080/api/admin/settings/:RateLimitingDefaultCapacityTiers -X PUT -d '10000,20000'`

`:RateLimitingCapacityByTierAndAction` is a JSON object specifying the rate by tier and a list of actions (commands).
This allows for more control over the rate limit of individual API command calls.
In the following example, calls made by a guest user (tier 0) for API `GetLatestPublishedDatasetVersionCommand` is further limited to only 10 calls per hour, while an authenticated user (tier 1) will be able to make 30 calls per hour to the same API.
`curl http://localhost:8080/api/admin/settings/:RateLimitingCapacityByTierAndAction -X PUT -d '[{"tier": 0, "limitPerHour": 10, "actions": ["GetLatestPublishedDatasetVersionCommand", "GetPrivateUrlCommand", "GetDatasetCommand", "GetLatestAccessibleDatasetVersionCommand"]}, {"tier": 0, "limitPerHour": 1, "actions": ["CreateGuestbookResponseCommand", "UpdateDatasetVersionCommand", "DestroyDatasetCommand", "DeleteDataFileCommand", "FinalizeDatasetPublicationCommand", "PublishDatasetCommand"]}, {"tier": 1, "limitPerHour": 30, "actions": ["CreateGuestbookResponseCommand", "GetLatestPublishedDatasetVersionCommand", "GetPrivateUrlCommand", "GetDatasetCommand", "GetLatestAccessibleDatasetVersionCommand", "UpdateDatasetVersionCommand", "DestroyDatasetCommand", "DeleteDataFileCommand", "FinalizeDatasetPublicationCommand", "PublishDatasetCommand"]}]'`

Hazelcast is configured in Payara and should not need any changes for this feature