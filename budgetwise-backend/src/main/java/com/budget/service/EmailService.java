package com.budget.service;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ── Warning Email (80% reached) ────────────────────────
    @Async
    public void sendWarningEmail(String toEmail, String username,
                                  BigDecimal spent, BigDecimal total,
                                  double percentage, String month,
                                  int year) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("BudgetWise Alert: You have used "
                    + Math.round(percentage) + "% of your budget!");

            String body =
                "Hi " + username + ",\n\n"
                + "You have used " + Math.round(percentage)
                + "% of your budget for " + month + " " + year + ".\n\n"
                + "Monthly Budget : Rs. "
                + String.format("%.2f", total) + "\n"
                + "Amount Spent   : Rs. "
                + String.format("%.2f", spent) + "\n"
                + "Remaining      : Rs. "
                + String.format("%.2f", total.subtract(spent)) + "\n\n"
                + "Please slow down your spending"
                + " to stay within budget.\n\n"
                + "----------------------------------------\n"
                + "Created by : Touheed Ahmed\n"
                + "Email      : touhidsagar2002@gmail.com\n"
                + "----------------------------------------\n"
                + "This is an automated alert from BudgetWise.";

            message.setText(body);
            mailSender.send(message);
            System.out.println("Warning email sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("Failed to send warning email: "
                    + e.getMessage());
        }
    }

    // ── Exceeded Email (100%+ reached) ─────────────────────
    @Async
    public void sendExceededEmail(String toEmail, String username,
                                   BigDecimal spent, BigDecimal total,
                                   BigDecimal overspent,
                                   String month, int year) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("BudgetWise Alert: Budget EXCEEDED for "
                    + month + " " + year + "!");

            String body =
                "Hi " + username + ",\n\n"
                + "Your budget for " + month + " " + year
                + " has been EXCEEDED!\n\n"
                + "Monthly Budget : Rs. "
                + String.format("%.2f", total) + "\n"
                + "Total Spent    : Rs. "
                + String.format("%.2f", spent) + "\n"
                + "Overspent By   : Rs. "
                + String.format("%.2f", overspent) + "\n\n"
                + "Take immediate action"
                + " to control your expenses.\n\n"
                + "----------------------------------------\n"
                + "Created by : Touheed Ahmed\n"
                + "Email      : touhidsagar2002@gmail.com\n"
                + "----------------------------------------\n"
                + "This is an automated alert from BudgetWise.";

            message.setText(body);
            mailSender.send(message);
            System.out.println("Exceeded email sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("Failed to send exceeded email: "
                    + e.getMessage());
        }
    }
}