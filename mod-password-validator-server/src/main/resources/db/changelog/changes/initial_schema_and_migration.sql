CREATE EXTENSION IF NOT EXISTS pgcrypto SCHEMA public;

CREATE TYPE RuleType as ENUM ('RegExp', 'Programmatic');

CREATE TYPE RuleValidationType as ENUM ('Soft', 'Strong');

CREATE TYPE RuleState as ENUM ('Enabled', 'Disabled');

CREATE TABLE ValidationRules
(
    id                       UUID PRIMARY KEY default gen_random_uuid(),
    name                     VARCHAR(256)
        CONSTRAINT unq_ValidationRules_Name UNIQUE NOT NULL,
    rule_type                RuleType              NOT NULL,
    rule_state               RuleState             NOT NULL,
    validation_type          RuleValidationType    NOT NULL,
    order_no                 INTEGER,
    rule_expression          TEXT,
    implementation_reference TEXT,
    module_name              VARCHAR(128),
    description              TEXT,
    err_message_id           VARCHAR(128)          NOT NULL,
    created_date             TIMESTAMP        default now(),
    created_by_user_id       UUID,
    created_by_username      VARCHAR(100),
    updated_date             TIMESTAMP,
    updated_by_user_id       UUID,
    updated_by_username      VARCHAR(100)

);

do $$
    <<copy_existing_rules>>
    begin
        insert into ValidationRules (id, name, rule_type, rule_state, validation_type, order_no, rule_expression, implementation_reference,
                                     module_name, description,
                                     err_message_id)
        select (jsonb ->> 'id')::uuid                           id,
               jsonb ->> 'name'                    as           name,
               (jsonb ->> 'type')::RuleType                     ruleType,
               (jsonb ->> 'state')::RuleState                   ruleState,
               (jsonb ->> 'validationType')::RuleValidationType validationType,
               (jsonb ->> 'orderNo')::INTEGER                   orderNo,
               jsonb ->> 'expression'              as           expression,
               jsonb ->> 'implementationReference' as           implementationReference,
               jsonb ->> 'moduleName'                           moduleName,
               jsonb ->> 'description'                          description,
               jsonb ->> 'errMessageId'                         errMessageId
        from validation_rules;
    exception
        when others then null;
    end copy_existing_rules $$;
