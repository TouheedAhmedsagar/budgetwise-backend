package com.budget.controller;

import com.budget.model.MonthlyBudget;
import com.budget.model.User;
import com.budget.repository.*;
import com.budget.service.EmailService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * REST API – Monthly Budget
 *
 * POST /api/budget/set             → { month, year, totalBudget }
 * GET  /api/budget/status          → ?month=&year=
 */

@Tag(name="Budget Wise REST Operation",
description="Used for Controlling the Budget")
@RestController
@RequestMapping("/api/budget")
public class BudgetController {

    @Autowired MonthlyBudgetRepository budgetRepo;
    @Autowired ExpenseRepository       expRepo;
    @Autowired UserRepository          userRepo;
    @Autowired EmailService            emailService;

    @Value("${app.budget.warning-threshold:0.80}")
    private double warningThreshold;

    // Track last alert sent to avoid repeated emails
    // Key = userId_month_year, Value = last alertStatus emailed
    private final Map<String, String> lastEmailSent = new HashMap<>();

    /* ── SET / UPDATE budget ────────────────────────────── */
    @Operation(summary="Set Budget",description="It is Used for the Setting the new Budget")
    @PostMapping("/set")
    public ResponseEntity<?> set(@Valid @RequestBody BudgetReq req,
                                  Authentication auth) {
        User user = resolve(auth);
        var existing = budgetRepo.findByUserIdAndMonthAndYear(
            user.getId(), req.month, req.year);

        MonthlyBudget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setTotalBudget(req.totalBudget);
        } else {
            budget = MonthlyBudget.builder()
                .user(user).month(req.month)
                .year(req.year).totalBudget(req.totalBudget)
                .build();
        }
        budgetRepo.save(budget);

        BigDecimal spent = expRepo.sumByUserMonthYear(
            user.getId(), req.month, req.year);
        return ResponseEntity.ok(buildStatus(budget, spent, true, user));
    }

    /* ── GET status ─────────────────────────────────────── */
    
    @Operation(summary="Set Budget",description="It is Used for the Getting the Budget details")
    @GetMapping("/status")
    public ResponseEntity<?> status(@RequestParam int month,
                                     @RequestParam int year,
                                     Authentication auth) {
        User user = resolve(auth);
        var opt = budgetRepo.findByUserIdAndMonthAndYear(
            user.getId(), month, year);

        if (opt.isEmpty())
            return ResponseEntity.ok(Map.of(
                "hasBudget", false,
                "message", "No budget set for this month yet."));

        BigDecimal spent = expRepo.sumByUserMonthYear(
            user.getId(), month, year);
        return ResponseEntity.ok(
            buildStatus(opt.get(), spent, true, user));
    }

    /* ── Build status map + trigger email if needed ─────── */
    public Map<String, Object> buildStatus(MonthlyBudget budget,
                                            BigDecimal spent,
                                            boolean hasBudget,
                                            User user) {
        BigDecimal total     = budget.getTotalBudget();
        BigDecimal remaining = total.subtract(spent);
        double pct = total.compareTo(BigDecimal.ZERO) == 0 ? 0
            : spent.divide(total, 4, RoundingMode.HALF_UP)
                   .doubleValue() * 100;

        String alertStatus, alertMessage;

        if (pct >= 100) {
            alertStatus  = "EXCEEDED";
            alertMessage = String.format(
                "🚨 Budget Exceeded! You spent Rs.%.2f — Rs.%.2f over your Rs.%.2f limit!",
                spent, spent.subtract(total), total);

            // Send EXCEEDED email (only once per month)
            triggerEmail("EXCEEDED", user, budget, spent, total, pct);

        } else if (pct >= warningThreshold * 100) {
            alertStatus  = "WARNING";
            alertMessage = String.format(
                "⚠️ Warning! You've used %.1f%% of your budget. Only Rs.%.2f left.",
                pct, remaining);

            // Send WARNING email (only once per month)
            triggerEmail("WARNING", user, budget, spent, total, pct);

        } else {
            alertStatus  = "SAFE";
            alertMessage = String.format(
                "✅ On track! Rs.%.2f remaining out of Rs.%.2f.",
                remaining, total);
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hasBudget",      hasBudget);
        m.put("budgetId",       budget.getId());
        m.put("month",          budget.getMonth());
        m.put("year",           budget.getYear());
        m.put("totalBudget",    total);
        m.put("totalSpent",     spent);
        m.put("remaining",      remaining);
        m.put("percentageUsed", Math.round(pct * 10.0) / 10.0);
        m.put("alertStatus",    alertStatus);
        m.put("alertMessage",   alertMessage);
        return m;
    }

    /* ── Email trigger — sends only when status changes ─── */
    private void triggerEmail(String newStatus, User user,
                               MonthlyBudget budget, BigDecimal spent,
                               BigDecimal total, double pct) {

        String key      = user.getId() + "_" + budget.getMonth()
                          + "_" + budget.getYear();
        String lastSent = lastEmailSent.get(key);

        // Don't resend same alert for same month
        if (newStatus.equals(lastSent)) return;

        lastEmailSent.put(key, newStatus);

        String monthName = getMonthName(budget.getMonth());
        int    year      = budget.getYear();

        if ("EXCEEDED".equals(newStatus)) {
            BigDecimal overspent = spent.subtract(total);
            emailService.sendExceededEmail(
                user.getEmail(), user.getUsername(),
                spent, total, overspent, monthName, year);

        } else if ("WARNING".equals(newStatus)) {
            emailService.sendWarningEmail(
                user.getEmail(), user.getUsername(),
                spent, total, pct, monthName, year);
        }
    }

    private String getMonthName(int month) {
        String[] months = {"", "January", "February", "March", "April",
                           "May", "June", "July", "August", "September",
                           "October", "November", "December"};
        return months[month];
    }

    private User resolve(Authentication auth) {
        return userRepo.findByUsername(auth.getName()).orElseThrow();
    }

    @Data
    static class BudgetReq {
        @NotNull @Min(1) @Max(12)   Integer    month;
        @NotNull @Min(2000)         Integer    year;
        @NotNull @DecimalMin("1.0") BigDecimal totalBudget;
    }
}
