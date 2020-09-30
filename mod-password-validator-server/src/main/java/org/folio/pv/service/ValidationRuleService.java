package org.folio.pv.service;

import org.folio.pv.domain.dto.ValidationRule;
import org.folio.pv.domain.dto.ValidationRuleCollection;
import org.folio.pv.mapper.ValidationRuleMapper;
import org.folio.pv.repository.ValidationRuleRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ValidationRuleService {

  private final ValidationRuleMapper validationRuleMapper;
  private final ValidationRuleRepository validationRuleRepository;

  @Autowired
  public ValidationRuleService(ValidationRuleMapper validationRuleMapper, ValidationRuleRepository validationRuleRepository) {
    this.validationRuleMapper = validationRuleMapper;
    this.validationRuleRepository = validationRuleRepository;
  }

  public ValidationRule getValidationRuleById(String ruleId) {
    var id = UUID.fromString(ruleId);

    return validationRuleRepository.findById(id).map(validationRuleMapper::mapEntityToDto).orElse(null);
  }

  public ValidationRuleCollection getValidationRules(Integer offset, Integer limit, String orderBy) {
    var validationRuleList = validationRuleRepository.findAll(new OffsetRequest(offset, limit));
    return validationRuleMapper.mapEntitiesToValidationRuleCollection(validationRuleList);
  }

  public ValidationRule createOrUpdateValidationRule(ValidationRule validationRule) {
    var rule = validationRuleMapper.mapDtoToEntity(validationRule);
    if (rule.getId() == null) {
      if (rule.getCreatedDate() == null) {
        rule.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
      }
    } else {
      rule = validationRuleRepository.getOne(rule.getId()).copyForUpdate(rule);
    }
    return validationRuleMapper.mapEntityToDto(validationRuleRepository.save(rule));
  }

  public ValidationRule storeValidationRule(ValidationRule validationRule) {
    var rule = validationRuleMapper.mapDtoToEntity(validationRule);
    return validationRuleMapper.mapEntityToDto(validationRuleRepository.save(rule));
  }
}
