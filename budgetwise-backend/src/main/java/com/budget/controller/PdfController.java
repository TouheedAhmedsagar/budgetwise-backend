package com.budget.controller;

import com.budget.model.Expense;
import com.budget.model.MonthlyBudget;
import com.budget.model.User;
import com.budget.repository.ExpenseRepository;
import com.budget.repository.MonthlyBudgetRepository;
import com.budget.repository.UserRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST API - PDF Report
 * GET /api/pdf/download?month=4&year=2025
 */

@Tag(name="Budget Wise REST Operation",
description="Used for Controlling the Bdget")
@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Autowired MonthlyBudgetRepository budgetRepo;
    @Autowired ExpenseRepository       expRepo;
    @Autowired UserRepository          userRepo;

    private static final DeviceRgb DARK    = new DeviceRgb(13, 17, 23);
    private static final DeviceRgb ACCENT  = new DeviceRgb(240, 165, 0);
    private static final DeviceRgb GREEN   = new DeviceRgb(63, 185, 80);
    private static final DeviceRgb RED     = new DeviceRgb(248, 81, 73);
    private static final DeviceRgb GRAY    = new DeviceRgb(139, 148, 158);
    private static final DeviceRgb LIGHTBG = new DeviceRgb(240, 246, 252);

    private final String[] MONTH_NAMES = {
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    @Operation(summary="Download",description="It is Used for the downloading the pdf")
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadPdf(
            @RequestParam int month,
            @RequestParam int year,
            Authentication auth) {

        User user = userRepo.findByUsername(auth.getName()).orElseThrow();

        Optional<MonthlyBudget> budgetOpt =
            budgetRepo.findByUserIdAndMonthAndYear(user.getId(), month, year);

        // ✅ Using java.util.List — NO iText List import
        java.util.List<Expense> expenses =
            expRepo.findByUserMonthYear(user.getId(), month, year);

        byte[] pdfBytes = generatePdf(user, budgetOpt, expenses, month, year);

        String filename = "BudgetWise_" + MONTH_NAMES[month] + "_" + year + ".pdf";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfBytes);
    }

    private byte[] generatePdf(User user,
                                 Optional<MonthlyBudget> budgetOpt,
                                 java.util.List<Expense> expenses,
                                 int month, int year) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter   writer   = new PdfWriter(baos);
            PdfDocument pdfDoc   = new PdfDocument(writer);
            Document    document = new Document(pdfDoc);
            document.setMargins(40, 50, 40, 50);

            String monthYear = MONTH_NAMES[month] + " " + year;

            // ── HEADER ────────────────────────────────────────
            document.add(new Paragraph("BudgetWise")
                .setFontSize(28).setBold()
                .setFontColor(DARK)
                .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(
                "Monthly Budget Report - " + monthYear)
                .setFontSize(14).setFontColor(GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5));

            document.add(new Paragraph(
                "Generated for: " + user.getUsername()
                + "  |  " + user.getEmail())
                .setFontSize(10).setFontColor(GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

            document.add(new LineSeparator(new SolidLine(1f)));
            document.add(new Paragraph(" "));

            // ── BUDGET SUMMARY ────────────────────────────────
            if (budgetOpt.isPresent()) {
                MonthlyBudget budget = budgetOpt.get();
                BigDecimal total     = budget.getTotalBudget();
                BigDecimal spent     = expenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal remaining = total.subtract(spent);
                double pct = total.compareTo(BigDecimal.ZERO) == 0 ? 0
                    : spent.divide(total, 4, RoundingMode.HALF_UP)
                           .doubleValue() * 100;

                document.add(sectionTitle("Budget Summary"));

                Table summaryTable = new Table(
                    UnitValue.createPercentArray(new float[]{60, 40}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(20);

                addSummaryRow(summaryTable, "Monthly Budget",
                    "Rs. " + String.format("%.2f", total), DARK, false);
                addSummaryRow(summaryTable, "Total Spent",
                    "Rs. " + String.format("%.2f", spent), RED, false);
                addSummaryRow(summaryTable, "Remaining Balance",
                    "Rs. " + String.format("%.2f", remaining),
                    remaining.compareTo(BigDecimal.ZERO) >= 0
                        ? GREEN : RED, false);
                addSummaryRow(summaryTable, "Budget Used",
                    String.format("%.1f%%", pct),
                    pct >= 100 ? RED : pct >= 80 ? ACCENT : GREEN, true);

                document.add(summaryTable);

                // Status box
                String statusText;
                DeviceRgb statusColor;
                if (pct >= 100) {
                    statusText  = "BUDGET EXCEEDED - Overspent by Rs. "
                        + String.format("%.2f", spent.subtract(total));
                    statusColor = RED;
                } else if (pct >= 80) {
                    statusText  = "WARNING - You have used "
                        + String.format("%.1f", pct) + "% of your budget";
                    statusColor = ACCENT;
                } else {
                    statusText  = "ON TRACK - You are within your budget";
                    statusColor = GREEN;
                }

                document.add(new Paragraph(statusText)
                    .setFontSize(12).setBold()
                    .setFontColor(ColorConstants.WHITE)
                    .setBackgroundColor(statusColor)
                    .setPadding(12)
                    .setBorderRadius(new BorderRadius(8))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(25));

            } else {
                document.add(new Paragraph(
                    "No budget was set for " + monthYear)
                    .setFontColor(GRAY).setItalic()
                    .setMarginBottom(20));
            }

            // ── CATEGORY SUMMARY ──────────────────────────────
            document.add(sectionTitle("Category Wise Summary"));

            if (expenses.isEmpty()) {
                document.add(new Paragraph(
                    "No expenses recorded for " + monthYear)
                    .setFontColor(GRAY).setItalic()
                    .setMarginBottom(20));
            } else {
                Map<String, BigDecimal> catMap = new LinkedHashMap<>();
                for (Expense e : expenses) {
                    catMap.merge(e.getCategory(),
                        e.getAmount(), BigDecimal::add);
                }

                BigDecimal grandTotal = expenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                Table catTable = new Table(
                    UnitValue.createPercentArray(new float[]{50, 25, 25}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(20);

                catTable.addHeaderCell(headerCell("Category"));
                catTable.addHeaderCell(headerCell("Amount (Rs.)"));
                catTable.addHeaderCell(headerCell("% of Total"));

                int rowIndex = 0;
                for (Map.Entry<String, BigDecimal> entry
                        : catMap.entrySet()) {

                    double catPct =
                        grandTotal.compareTo(BigDecimal.ZERO) == 0 ? 0
                        : entry.getValue()
                               .divide(grandTotal, 4, RoundingMode.HALF_UP)
                               .doubleValue() * 100;

                    boolean isEven = rowIndex % 2 == 0;
                    catTable.addCell(dataCell(
                        entry.getKey(), isEven, TextAlignment.LEFT));
                    catTable.addCell(dataCell(
                        String.format("%.2f", entry.getValue()),
                        isEven, TextAlignment.RIGHT));
                    catTable.addCell(dataCell(
                        String.format("%.1f%%", catPct),
                        isEven, TextAlignment.CENTER));
                    rowIndex++;
                }

                catTable.addCell(totalCell("TOTAL"));
                catTable.addCell(totalCell(
                    String.format("%.2f", grandTotal)));
                catTable.addCell(totalCell("100%"));
                document.add(catTable);
            }

            // ── ALL EXPENSES LIST ─────────────────────────────
            document.add(sectionTitle("All Expenses"));

            if (expenses.isEmpty()) {
                document.add(new Paragraph("No expenses for this month.")
                    .setFontColor(GRAY).setItalic()
                    .setMarginBottom(20));
            } else {
                Table expTable = new Table(
                    UnitValue.createPercentArray(
                        new float[]{30, 20, 20, 15, 15}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(20);

                expTable.addHeaderCell(headerCell("Title"));
                expTable.addHeaderCell(headerCell("Category"));
                expTable.addHeaderCell(headerCell("Amount (Rs.)"));
                expTable.addHeaderCell(headerCell("Date"));
                expTable.addHeaderCell(headerCell("Note"));

                int idx = 0;
                for (Expense e : expenses) {
                    boolean isEven = idx % 2 == 0;
                    expTable.addCell(dataCell(
                        e.getTitle(), isEven, TextAlignment.LEFT));
                    expTable.addCell(dataCell(
                        e.getCategory(), isEven, TextAlignment.CENTER));
                    expTable.addCell(dataCell(
                        String.format("%.2f", e.getAmount()),
                        isEven, TextAlignment.RIGHT));
                    expTable.addCell(dataCell(
                        e.getExpenseDate().toString(),
                        isEven, TextAlignment.CENTER));
                    expTable.addCell(dataCell(
                        e.getNote() != null ? e.getNote() : "-",
                        isEven, TextAlignment.LEFT));
                    idx++;
                }
                document.add(expTable);
            }

            // ── FOOTER ────────────────────────────────────────
            document.add(new LineSeparator(new SolidLine(0.5f)));

            document.add(new Paragraph(
                "Generated by BudgetWise  |  " + LocalDate.now())
                .setFontSize(9).setFontColor(GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10));

            // ✅ Creator Credit
            document.add(new Paragraph(
                "Created by : Touheed Ahmed  |  "
                + "touhidsagar2002@gmail.com")
                .setFontSize(9).setBold()
                .setFontColor(ACCENT)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(4));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    // ── Helper Methods ─────────────────────────────────────
    private Paragraph sectionTitle(String text) {
        return new Paragraph(text)
            .setFontSize(14).setBold()
            .setFontColor(DARK)
            .setMarginTop(10).setMarginBottom(10);
    }

    private void addSummaryRow(Table table, String label,
                                String value, DeviceRgb valueColor,
                                boolean isLast) {
        Cell labelCell = new Cell()
            .add(new Paragraph(label)
                .setFontSize(12).setFontColor(GRAY))
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(
                isLast ? LIGHTBG : ColorConstants.WHITE)
            .setPaddingTop(8).setPaddingBottom(8);

        Cell valueCell = new Cell()
            .add(new Paragraph(value)
                .setFontSize(13).setBold()
                .setFontColor(valueColor))
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(
                isLast ? LIGHTBG : ColorConstants.WHITE)
            .setTextAlignment(TextAlignment.RIGHT)
            .setPaddingTop(8).setPaddingBottom(8);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private Cell headerCell(String text) {
        return new Cell()
            .add(new Paragraph(text)
                .setBold().setFontSize(11)
                .setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(DARK)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(8);
    }

    private Cell dataCell(String text, boolean isEven,
                           TextAlignment align) {
        return new Cell()
            .add(new Paragraph(text).setFontSize(10))
            .setBackgroundColor(
                isEven ? LIGHTBG : ColorConstants.WHITE)
            .setTextAlignment(align)
            .setPadding(7);
    }

    private Cell totalCell(String text) {
        return new Cell()
            .add(new Paragraph(text)
                .setBold().setFontSize(11)
                .setFontColor(DARK))
            .setBackgroundColor(ACCENT)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(8);
    }
}