package com.budget.controller;

import com.budget.model.Expense;
import com.budget.model.User;
import com.budget.repository.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API – Expenses
 *
 * POST   /api/expenses            Add expense → { expense, budgetStatus }
 * GET    /api/expenses            List        → ?month=&year= (optional)
 * PUT    /api/expenses/{id}       Update
 * DELETE /api/expenses/{id}       Delete
 */
@Tag(name="Budget Wise REST Operation",
description="Used for Controlling the Expenses")
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired ExpenseRepository       expRepo;
    @Autowired MonthlyBudgetRepository budgetRepo;
    @Autowired UserRepository          userRepo;
    @Autowired BudgetController        budgetCtrl;

    /* ── ADD ──────────────────────────────────────────────── */
    
    @Operation(summary="Add Expenses",description="It is Used for the Adding the expenses")
    @PostMapping
    public ResponseEntity<?> add(@Valid @RequestBody ExpenseReq req,
                                  Authentication auth) {
        User user = resolve(auth);

        Expense exp = Expense.builder()
            .user(user)
            .title(req.title)
            .amount(req.amount)
            .category(req.category != null ? req.category : "General")
            .note(req.note)
            .expenseDate(req.expenseDate != null
                ? req.expenseDate : LocalDate.now())
            .build();
        expRepo.save(exp);

        // Check budget status → triggers email if needed
        int m = exp.getExpenseDate().getMonthValue();
        int y = exp.getExpenseDate().getYear();

        var budgetOpt = budgetRepo.findByUserIdAndMonthAndYear(
            user.getId(), m, y);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("expense", toMap(exp));

        if (budgetOpt.isPresent()) {
            BigDecimal spent = expRepo.sumByUserMonthYear(
                user.getId(), m, y);
            // buildStatus also triggers email if threshold crossed
            Map<String, Object> status = budgetCtrl.buildStatus(
                budgetOpt.get(), spent, true, user);
            response.put("budgetStatus", status);
        } else {
            response.put("budgetStatus", null);
        }

        return ResponseEntity.ok(response);
    }

    /* ── LIST ─────────────────────────────────────────────── */
    @Operation(summary="Get Budget",description="It is Used for the Getting the details of the expenses")
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Authentication auth) {

        User user = resolve(auth);
        List<Expense> list = (month != null && year != null)
            ? expRepo.findByUserMonthYear(user.getId(), month, year)
            : expRepo.findByUserIdOrderByExpenseDateDesc(user.getId());

        return ResponseEntity.ok(
            list.stream().map(this::toMap).collect(Collectors.toList()));
    }

    /* ── UPDATE ───────────────────────────────────────────── */
    @Operation(summary="Update expenses",description="It is Used for the updating the expenes")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                     @Valid @RequestBody ExpenseReq req,
                                     Authentication auth) {
        User user = resolve(auth);
        Expense exp = expRepo.findById(id).orElseThrow();

        if (!exp.getUser().getId().equals(user.getId()))
            return ResponseEntity.status(403)
                .body(Map.of("message", "Forbidden"));

        exp.setTitle(req.title);
        exp.setAmount(req.amount);
        exp.setCategory(req.category != null ? req.category : "General");
        exp.setNote(req.note);
        if (req.expenseDate != null) exp.setExpenseDate(req.expenseDate);
        expRepo.save(exp);

        return ResponseEntity.ok(toMap(exp));
    }

    /* ── DELETE ───────────────────────────────────────────── */
    @Operation(summary="Delete Budget",description="It is Used for deleting the expenses")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                     Authentication auth) {
        User user = resolve(auth);
        Expense exp = expRepo.findById(id).orElseThrow();

        if (!exp.getUser().getId().equals(user.getId()))
            return ResponseEntity.status(403)
                .body(Map.of("message", "Forbidden"));

        expRepo.delete(exp);
        return ResponseEntity.ok(Map.of("message", "Expense deleted"));
    }

    /* ── Helpers ──────────────────────────────────────────── */
    private Map<String, Object> toMap(Expense e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          e.getId());
        m.put("title",       e.getTitle());
        m.put("amount",      e.getAmount());
        m.put("category",    e.getCategory());
        m.put("note",        e.getNote());
        m.put("expenseDate", e.getExpenseDate().toString());
        return m;
    }

    private User resolve(Authentication auth) {
        return userRepo.findByUsername(auth.getName()).orElseThrow();
    }

    @Data
    static class ExpenseReq {
        @NotBlank @Size(max = 100) String title;
        @NotNull @DecimalMin("0.01") BigDecimal amount;
        String    category;
        String    note;
        LocalDate expenseDate;
    }
}
