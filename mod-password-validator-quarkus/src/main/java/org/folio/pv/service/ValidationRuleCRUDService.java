package org.folio.pv.service;

import org.folio.pv.domain.dto.ValidationRule;
import org.folio.pv.domain.dto.ValidationRuleCollection;
import org.folio.pv.domain.entity.PasswordValidationRule;
import org.folio.pv.mapper.ValidationRuleMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class ValidationRuleCRUDService {

  private EntityManager entityManager;

  private ValidationRuleMapper validationRuleMapper;

  @Inject
  public void init(EntityManager entityManager, ValidationRuleMapper validationRuleMapper) {
    this.entityManager = entityManager;
    this.validationRuleMapper = validationRuleMapper;
  }

  public ValidationRule getTenantRuleById(String validationRuleId) {
    var id = UUID.fromString(validationRuleId);
    var passwordValidationRule = entityManager.find(PasswordValidationRule.class, id);
    return validationRuleMapper.mapEntityToDto(passwordValidationRule);
  }

  public ValidationRuleCollection getValidationRules(Integer offset, Integer limit, String orderBy) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<PasswordValidationRule> criteriaQuery = builder.createQuery(PasswordValidationRule.class);

    var from = criteriaQuery.from(PasswordValidationRule.class);
    criteriaQuery.select(from);

    var query = entityManager.createQuery(criteriaQuery);
    query.setFirstResult(offset);
    query.setMaxResults(limit);

    var passwordValidationRules = query.getResultList();
    return validationRuleMapper.mapEntitiesToValidationRuleCollection(passwordValidationRules);
  }

  @Transactional
  public ValidationRule createOrUpdateValidationRule(ValidationRule validationRule) {
    var rule = validationRuleMapper.mapDtoToEntity(validationRule);

    if (rule.getId() == null) {
      if (rule.getCreatedDate() == null) {
        rule.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
      }
      entityManager.persist(rule);
    } else {
      var existingRule = entityManager.find(PasswordValidationRule.class, rule.getId());
      if (existingRule != null) {
        rule = entityManager.merge(existingRule.copyForUpdate(rule));
      } else {
        entityManager.persist(rule);
      }
    }

    return validationRuleMapper.mapEntityToDto(rule);
  }
}
