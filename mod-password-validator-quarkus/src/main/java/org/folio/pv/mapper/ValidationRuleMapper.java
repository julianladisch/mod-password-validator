package org.folio.pv.mapper;

import org.apache.commons.lang3.StringUtils;
import org.folio.pv.domain.dto.ValidationRule;
import org.folio.pv.domain.dto.ValidationRuleCollection;
import org.folio.pv.domain.entity.PasswordValidationRule;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueCheckStrategy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "cdi", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ValidationRuleMapper {

  @Mappings({
    @Mapping(target = "id", expression = "java(uuidToStringSafe(passwordValidationRule.getId()))"),
    @Mapping(target = "name", source = "name"),
    @Mapping(target = "type", expression = "java(ValidationRule.TypeEnum.fromValue(passwordValidationRule.getRuleType()))"),
    @Mapping(target = "validationType", expression = "java(ValidationRule.ValidationTypeEnum.fromValue(passwordValidationRule.getValidationType()))"),
    @Mapping(target = "state", expression = "java(ValidationRule.StateEnum.fromValue(passwordValidationRule.getRuleState()))"),
    @Mapping(target = "moduleName", source = "moduleName"),
    @Mapping(target = "implementationReference", source = "implementationReference"),
    @Mapping(target = "expression", source = "ruleExpression"),
    @Mapping(target = "description", source = "description"),
    @Mapping(target = "orderNo", source = "orderNo"),
    @Mapping(target = "errMessageId", source = "errMessageId"),
    @Mapping(target = "metadata.createdDate", source = "createdDate"),
    @Mapping(target = "metadata.updatedDate", source = "updatedDate"),
    @Mapping(target = "metadata.createdByUserId", expression = "java(passwordValidationRule.getCreatedByUserId() == null ? null : String.valueOf(passwordValidationRule.getCreatedByUserId()))"),
    @Mapping(target = "metadata.updatedByUserId", expression = "java(passwordValidationRule.getUpdatedByUserId() == null ? null : String.valueOf(passwordValidationRule.getUpdatedByUserId()))"),
    @Mapping(target = "metadata.createdByUsername", source = "createdByUsername"),
    @Mapping(target = "metadata.updatedByUsername", source = "updatedByUsername")
  })
  ValidationRule mapEntityToDto(PasswordValidationRule passwordValidationRule);


  @Mappings({
    @Mapping(target = "id", expression = "java(stringToUUIDSafe(validationRule.getId()))"),
    @Mapping(target = "ruleType", expression = "java(validationRule.getType() == null ? null : validationRule.getType().toString())"),
    @Mapping(target = "ruleState", expression = "java(validationRule.getState() == null ? null : validationRule.getState().toString())"),
    @Mapping(target = "validationType", expression = "java(validationRule.getValidationType() == null ? null : validationRule.getValidationType().toString())"),
    @Mapping(target = "createdByUserId", expression = "java(validationRule.getMetadata() == null ? null : stringToUUIDSafe(validationRule.getMetadata().getCreatedByUserId()))"),
    @Mapping(target = "updatedByUserId", expression = "java(validationRule.getMetadata() == null ? null : stringToUUIDSafe(validationRule.getMetadata().getUpdatedByUserId()))"),

  })
  @InheritInverseConfiguration
  PasswordValidationRule mapDtoToEntity(ValidationRule validationRule);

  @Mappings({})
  List<ValidationRule> mapEntitiesToDtos(Iterable<PasswordValidationRule> passwordValidationRuleList);

  @InheritInverseConfiguration
  List<PasswordValidationRule> mapDtosToEntities(List<ValidationRule> validationRuleList);

  default ValidationRuleCollection mapEntitiesToValidationRuleCollection(Iterable<PasswordValidationRule> passwordValidationRuleList) {
    return new ValidationRuleCollection().rules(mapEntitiesToDtos(passwordValidationRuleList));
  }

  default UUID stringToUUIDSafe(String uuid) {
    return (StringUtils.isBlank(uuid)) ? null : java.util.UUID.fromString(uuid);
  }

  default String uuidToStringSafe(UUID uuid) {
    return uuid != null ? uuid.toString() : null;
  }

  default OffsetDateTime timeStampToOffsetDateTime(Timestamp timestamp) {
    return timestamp != null ? OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp.getTime()), ZoneId.of("UTC")) : null;
  }

  default Timestamp offsetDateTimeToTimeStamp(OffsetDateTime offsetDateTime) {
    return offsetDateTime != null ? Timestamp.valueOf(offsetDateTime.toLocalDateTime()) : null;
  }
}
