## 2021-05-21 v2.0.2
* MODPWD-65 Fix exposing env variables during startup

## 2021-03-25 v2.0.1
 * FOLSPRINGB-2 Reinstate "USER folio" in Dockerfile
 * MODPWD-59 Add admin health-check endpoint

## 2021-03-02 v2.0.0
 * MODPWD-54 Add personal data disclosure form
 * Module implementation with Spring framework
 * Use new api-lint FOLIO-2893

## 2020-11-04 v1.8.2
 * Update RMB to v31.1.5 and Vertx to 3.9.4

## 2020-10-23 v1.8.1
 * Fix logging issue
 * Update RMB to v31.1.2 and Vertx to 3.9.3

## 2020-10-06 v1.8.0
 * MODPWD-39 Migrate to JDK 11 and RMB 31.x

## 2020-06-11 v1.7.0
 * Update password complexity requirements (MODPWD-35)
 * Update RMB to v30.0.2 and Vertx to 3.9.1 (MODPWD-37)

## 2020-12-03 v1.6.0
 * Replace deprecated HttpClient with WebClient (MODPWD-29)

## 2019-04-12 v1.5.0
 * Update RMB version (MODPWD-28)
 * Use JVM features to manage container memory (MODPWD-27)
 * Fix security vulnerabilities reported in jackson-databind (MODPWD-26)
 * mod-password-validator does not purge tenant data (MODPWD-24)
 * Remove old MD metadata (FOLIO-2321)
 * Add LaunchDescriptor settings (FOLIO-2234)
 * Enable kube-deploy pipeline for platform-core modules (FOLIO-2256)

## 2019-09-10 v1.4.1
 * Fix security vulnerabilities reported in jackson-databind (MODPWD-22)

## 2019-07-23 v1.4.0
 * Change user password validation implementation (MODPWD-20)
 * Fix security vulnerabilities reported in jackson-databind

## 2019-06-11 v1.3.0
 * Add links to README additional info (FOLIO-473)
 * Fix security vulnerabilities reported in jackson-databind
 * Initial module metadata (FOLIO-2003)

## 2019-05-07 v1.2.0
 * Increase test coverage for mod-password-validator (MODPWD-16)

## 2019-03-14 v1.1.0
 * Update to RAML 1.0 and RMB23 (MODPWD-8)
 * Fix security vulnerabilities reported in jackson-databind (MODPWD-14)
 
 ## 2018-09-19 v1.0.1
 * Added default password validation rules
 * Added creation of the default rule list during module enabling phase for a tenant

 The password MUST:

|    Description                                 |  Invalid examples                 |
|------------------------------------------------|-----------------------------------|
| Contain minimum 8 characters                   | 'pasword'                         |
| Contain both lowercase and uppercase letters   | 'password', 'PASSWORD'            |
| Contain at least one numeric character         | 'password'                        |
| Contain at least one special character         | 'password'                        |
| NOT contain your username                      | 'pas<USER_NAME>sword'             |
| NOT contain a keyboard sequence                | 'qwerty12', '12345678', 'q1234567'|
| NOT contain the same character                 | 'password'                        |
| NOT contain whitespace                         | 'pas sword'                       |
## 2018-09-19 v1.0.0
 * Add schema description to create validation_rules table
 * Add endpoints /tenant/rules with GET, POST and PUT methods to manage rules for tenant
 * Add endpoint /validate for password validation
 * Implement Validation Engine and Validation Registry services
 * Set up endpoint permissions

 CRUD API for rules and password:

 | METHOD |             URL               | DESCRIPTION                                        |
 |--------|-------------------------------|----------------------------------------------------|
 | GET    | /tenant/rules                 | Get list of the rules                              |
 | POST   | /tenant/rules                 | Add a new rule to a tenant                         |
 | PUT    | /tenant/rules                 | Change a rule for a tenant                         |
 | GET    | /tenant/rules/{ruleId}        | Get a rule by id                                   |
 | POST   | /password/validate            | Validate user password                             |

## 2018-09-04 v0.0.1
 * Initial module setup
