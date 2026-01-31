package com.example.releasesystem.release.dto;

import com.example.releasesystem.release.domain.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private Role role;
}
