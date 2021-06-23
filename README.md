# mod-password-validator

Copyright (C) 2018-2021 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

<!-- ../../okapi/doc/md2toc -l 2 -h 4 README.md -->
* [Introduction](#introduction)
* [Compiling](#compiling)
* [Docker](#docker)
* [Installing the module](#installing-the-module)
* [Deploying the module](#deploying-the-module)
* [Additional information](#additional-information)

## Introduction

The module provides a default rule set for a tenant and functionality to manage them via REST API and
also allows to use a validation flow for a user password.

The module supports following rules for a password

The password MUST:

 |    Description                                 |  Invalid examples                 | Default  |
 |------------------------------------------------|-----------------------------------|----------|
 | Contain minimum 8 characters                   | 'pasword'                         | enabled  |
 | Contain both lowercase and uppercase letters   | 'password', 'PASSWORD'            | enabled  |
 | Contain at least one numeric character         | 'password'                        | enabled  |
 | Contain at least one special character         | 'password'                        | enabled  |
 | NOT contain your username                      | 'pas<USER_NAME>sword'             | enabled  |
 | NOT contain a keyboard sequence                | 'qwerty12', '12345678', 'q1234567'| disabled |
 | NOT contain the same character                 | 'password'                        | disabled |
 | NOT contain whitespace                         | 'pas sword'                       | disabled |
 | NOT contain two consecutive whitespaces        | 'Michael &bnsp; &nbsp; Jordan'    | enabled  |

## API

Module provides next API:

 | METHOD |  URL                          | DESCRIPTION                                                       |
 |--------|-------------------------------|-------------------------------------------------------------------|
 | GET    | /tenant/rules                 | Get list of the rules                                             |
 | POST   | /tenant/rules                 | Add a new rule to a tenant                                        |
 | PUT    | /tenant/rules                 | Change a rule for a tenant                                        |
 | GET    | /tenant/rules/{ruleId}        | Returns a particular rule by id                                   |
 | POST   | /password/validate            | Validates a user credentials provided within the request body     |

The initial rules are in
https://github.com/folio-org/mod-password-validator/blob/master/src/main/resources/db/changelog/changes/v1.9.0/populate-initial-rules.sql

## Compiling

```
   mvn install
```

See that it says "BUILD SUCCESS" near the end.

## Docker

Build the docker container with:

```
   docker build -t mod-password-validator .
```

Test that it runs with:

```
   docker run -t -i -p 8081:8081 mod-password-validator
```

## Installing the module

Follow the guide of
[Deploying Modules](https://github.com/folio-org/okapi/blob/master/doc/guide.md#example-1-deploying-and-using-a-simple-module)
sections of the Okapi Guide and Reference, which describe the process in detail.

First of all you need a running Okapi instance.
(Note that [specifying](../README.md#setting-things-up) an explicit 'okapiurl' might be needed.)

```
   cd .../okapi
   java -jar okapi-core/target/okapi-core-fat.jar dev
```

We need to declare the module to Okapi:

```
curl -w '\n' -X POST -D -   \
   -H "Content-type: application/json"   \
   -d @target/ModuleDescriptor.json \
   http://localhost:9130/_/proxy/modules
```

That ModuleDescriptor tells Okapi what the module is called, what services it
provides, and how to deploy it.

## Deploying the module

Next we need to deploy the module. There is a deployment descriptor in
`target/DeploymentDescriptor.json`. It tells Okapi to start the module on 'localhost'.

Deploy it via Okapi discovery:

```
curl -w '\n' -D - -s \
  -X POST \
  -H "Content-type: application/json" \
  -d @target/DeploymentDescriptor.json  \
  http://localhost:9130/_/discovery/modules
```

Then we need to enable the module for the tenant:

```
curl -w '\n' -X POST -D -   \
    -H "Content-type: application/json"   \
    -d @target/TenantModuleDescriptor.json \
    http://localhost:9130/_/proxy/tenants/<tenant_name>/modules
```

## Additional information

### Issue tracker

See project [MODPWD](https://issues.folio.org/browse/MODPWD)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker/).

### ModuleDescriptor

See the built `target/ModuleDescriptor.json` for the interfaces that this module
requires and provides, the permissions, and the additional module metadata.

### API documentation

This module's [API documentation](https://dev.folio.org/reference/api/#mod-password-validator).

### Code analysis

[SonarQube analysis](https://sonarcloud.io/dashboard?id=org.folio%3Amod-password-validator).

### Download and configuration

The built artifacts for this module are available.
See [configuration](https://dev.folio.org/download/artifacts) for repository access,
and the [Docker image](https://hub.docker.com/r/folioorg/mod-password-validator/).

