# mod-password-validator

Copyright (C) 2018 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

<!-- ../../okapi/doc/md2toc -l 2 -h 4 README.md -->
* [Introduction](#introduction)
* [Compiling](#compiling)
* [Docker](#docker)
* [Installing the module](#installing-the-module)
* [Deploying the module](#deploying-the-module)

## Introduction

The module provides a default rule set for a tenant and functionality to manage them via REST API and 
also allows to use a validation flow for a user password.

 The module supports following rules for a password
  
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

## API

Module provides next API:

 | METHOD |  URL                          | DESCRIPTION                                                       |
 |--------|-------------------------------|-------------------------------------------------------------------|
 | GET    | /tenant/rules                 | Get list of the rules                                             |
 | POST   | /tenant/rules                 | Add a new rule to a tenant                                        |
 | PUT    | /tenant/rules                 | Change a rule for a tenant                                        |
 | GET    | /tenant/rules/{ruleId}        | Returns a particular rule by id                                   |
 | POST   | /password/validate            | Validates a user credentials provided within the request body     |

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

