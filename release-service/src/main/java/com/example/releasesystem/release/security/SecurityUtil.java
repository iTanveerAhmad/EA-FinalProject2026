package com.example.releasesystem.release.security;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Utility class for extracting security information from the current request context.
 * Used by controllers to retrieve the authenticated developer's ID from JWT.
 */
@UtilityClass
public class SecurityUtil {

    /**
     * Extracts the current authenticated user's username from the security context.
     * In production, this represents the developer's ID.
     *
     * @return The username/developer ID of the authenticated user
     * @throws IllegalStateException if no authentication is found in the context
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found in security context");
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        
        throw new IllegalStateException("Cannot extract username from principal: " + principal.getClass());
    }

    /**
     * Checks if the current user has a specific role.
     *
     * @param role The role to check (e.g., "ROLE_ADMIN", "ROLE_DEVELOPER")
     * @return true if the user has the role, false otherwise
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(role));
    }
}
