package org.folio.pv.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.pv.domain.entity.PasswordValidationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ValidationRuleRepository extends JpaRepository<PasswordValidationRule, UUID> {
  Optional<PasswordValidationRule> findById(UUID id);

  List<PasswordValidationRule> findByRuleState(String ruleState);
}
