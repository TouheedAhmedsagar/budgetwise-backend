package com.budget.repository;

import com.budget.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // ── Get all expenses for a user (latest first) ──────────
    List<Expense> findByUserIdOrderByExpenseDateDesc(Long userId);

    // ── Get expenses filtered by month & year ───────────────
    @Query(value = "SELECT * FROM expenses WHERE user_id = :uid " +
                   "AND EXTRACT(MONTH FROM expense_date) = :m " +
                   "AND EXTRACT(YEAR  FROM expense_date) = :y " +
                   "ORDER BY expense_date DESC",
           nativeQuery = true)
    List<Expense> findByUserMonthYear(@Param("uid") Long uid,
                                      @Param("m") int m,
                                      @Param("y") int y);

    // ── Sum of expenses for a given month & year ─────────────
    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE user_id = :uid " +
                   "AND EXTRACT(MONTH FROM expense_date) = :m " +
                   "AND EXTRACT(YEAR  FROM expense_date) = :y",
           nativeQuery = true)
    BigDecimal sumByUserMonthYear(@Param("uid") Long uid,
                                  @Param("m") int m,
                                  @Param("y") int y);
}