package com.budget.controller;

import com.budget.model.User;
import com.budget.repository.UserRepository;
import com.budget.security.JwtUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * REST API – Authentication
 *
 * POST /api/auth/register  → { username, email, password }
 * POST /api/auth/login     → { username, password }
 *                           ← { token, username, userId }
 */

@Tag(name="Budget Wise REST API Register/Login",
description="used for Register/Login")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired AuthenticationManager authManager;
    @Autowired UserRepository         userRepo;
    @Autowired PasswordEncoder        encoder;
    @Autowired JwtUtils               jwt;

    /* ── REGISTER ───────────────────────────────────────── */
    @Operation(summary="Register User",description="It is Used for the Registration of the new User")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterReq req) {

        if (userRepo.existsByUsername(req.username))
            return ResponseEntity.badRequest().body(Map.of("message","Username already taken"));
        if (userRepo.existsByEmail(req.email))
            return ResponseEntity.badRequest().body(Map.of("message","Email already registered"));

        userRepo.save(User.builder()
            .username(req.username)
            .email(req.email)
            .password(encoder.encode(req.password))
            .build());

        return ResponseEntity.ok(Map.of("message","Registration successful! Please login."));
    }

    /* ── LOGIN ──────────────────────────────────────────── */
    @Operation(summary="Login User",description="It is Used for Login of the User")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginReq req) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username, req.password));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message","Invalid username or password"));
        }

        User user  = userRepo.findByUsername(req.username).orElseThrow();
        String tok = jwt.generate(req.username);

        return ResponseEntity.ok(Map.of(
            "token",    tok,
            "username", user.getUsername(),
            "userId",   user.getId()
        ));
    }

    /* ── Inner request DTOs ─────────────────────────────── */
    @Data static class RegisterReq {
        @NotBlank @Size(min=3,max=50) String username;
        @NotBlank @Email              String email;
        @NotBlank @Size(min=6)        String password;
    }
    @Data static class LoginReq {
        @NotBlank String username;
        @NotBlank String password;
    }
}
