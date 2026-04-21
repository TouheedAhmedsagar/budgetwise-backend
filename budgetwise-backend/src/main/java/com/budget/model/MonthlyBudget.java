package com.budget.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "monthly_budgets")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MonthlyBudget {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false) private Integer month;
    @Column(nullable = false) private Integer year;

    @Column(name = "total_budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalBudget;

    @Column(name = "created_at") private LocalDate createdAt;

    @PrePersist public void prePersist() { this.createdAt = LocalDate.now(); }
}
