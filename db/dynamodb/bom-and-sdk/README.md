# DynamoDB test via `spring-cloud-aws-dependencies` and the AWS SDK (`v2`)

since part of the functionality of this application depends on having injected credentials,
the application also has:

* an infrastructure configuration (with [OpenTofu](https://opentofu.org/)) in `src/infra`
* a smoke test (which does not run by default)
