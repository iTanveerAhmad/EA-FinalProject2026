package com.example.releasesystem.release.dto;

import com.example.releasesystem.release.domain.Role;
import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}
