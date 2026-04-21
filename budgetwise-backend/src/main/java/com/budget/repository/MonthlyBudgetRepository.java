package com.budget.repository;
import com.budget.model.MonthlyBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, Long> {
    Optional<MonthlyBudget> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);
}
