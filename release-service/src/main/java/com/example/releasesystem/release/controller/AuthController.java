package com.example.releasesystem.release.controller;

import com.example.releasesystem.release.domain.User;
import com.example.releasesystem.release.dto.AuthRequest;
import com.example.releasesystem.release.dto.AuthResponse;
import com.example.releasesystem.release.dto.RegisterRequest;
import com.example.releasesystem.release.repository.UserRepository;
import com.example.releasesystem.release.security.JwtService;
import com.example.releasesystem.release.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuthController handles user authentication and registration.
 * Implements JWT-based security with two roles: ADMIN and DEVELOPER.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Register a new user with a specified role (ADMIN or DEVELOPER).
     * Password is stored using bcrypt encryption.
     * Returns a JWT token for immediate login.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        var user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        userRepository.save(user);

        // Auto-login
        var jwtToken = jwtService.generateToken(new CustomUserDetailsService(userRepository).loadUserByUsername(user.getUsername()));
        return ResponseEntity.ok(new AuthResponse(jwtToken));
    }

    /**
     * Authenticate a user with credentials and return a JWT token.
     * Token TTL is 10 hours by default.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(new CustomUserDetailsService(userRepository).loadUserByUsername(user.getUsername()));
        return ResponseEntity.ok(new AuthResponse(jwtToken));
    }
}
