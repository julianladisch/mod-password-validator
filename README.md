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

The main purposes of the module are providing a validation flow for user password and storing
Rules for the tenant.

The module supports following requests:

 `GET` request to `/password/validators?type={type}`, which just returns the list of the rules applied to the current
tenant.

 `POST` `/password/validate` to validate user password which accepts any JSON structure, and returns validation result.

## API

Module provides next API:


| Interface                            | Description                                                                            |
|--------------------------------------|----------------------------------------------------------------------------------------|
| POST /validate                       | validates a user credentials provided within the request body                          |
| POST /tenant/rules                   | adds a rule to a tenant                                                                |
| PUT /tenant/rules                    | enables/disables/changes rul                                                           |
| GET /tenant/rules                    | returns all rules for a tenant                                                         |
| GET /tenant/rules/{ruleId}           | returns a particular rule                                                              |


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

