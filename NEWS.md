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
