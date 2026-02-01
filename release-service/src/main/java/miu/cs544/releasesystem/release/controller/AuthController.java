package miu.cs544.releasesystem.release.controller;

import miu.cs544.releasesystem.release.domain.User;
import miu.cs544.releasesystem.release.dto.AuthRequest;
import miu.cs544.releasesystem.release.dto.AuthResponse;
import miu.cs544.releasesystem.release.dto.RegisterRequest;
import miu.cs544.releasesystem.release.dto.UserInfo;
import miu.cs544.releasesystem.release.repository.UserRepository;
import miu.cs544.releasesystem.release.security.JwtService;
import miu.cs544.releasesystem.release.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * Register a new user with a specified role (ADMIN or DEVELOPER).
     * Password is stored using bcrypt encryption.
     * Returns a JWT token for immediate login.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        // prevent duplicate users in DB during tests or repeated runs
        var existing = userRepository.findFirstByUsername(request.getUsername());
        User user;
        if (existing.isPresent()) {
            user = existing.get();
        } else {
            user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(request.getRole());
            userRepository.save(user);
        }

        // Auto-login using injected CustomUserDetailsService
        var jwtToken = jwtService.generateToken(customUserDetailsService.loadUserByUsername(user.getUsername()));
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
        var user = userRepository.findFirstByUsername(request.getUsername())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(customUserDetailsService.loadUserByUsername(user.getUsername()));
        return ResponseEntity.ok(new AuthResponse(jwtToken));
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfo> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).build();
        }
        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.replace("ROLE_", ""))
                .findFirst()
                .orElse("DEVELOPER");
        return ResponseEntity.ok(new UserInfo(auth.getName(), role));
    }
}
