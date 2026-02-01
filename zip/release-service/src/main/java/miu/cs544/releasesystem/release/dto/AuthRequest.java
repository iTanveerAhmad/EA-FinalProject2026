package miu.cs544.releasesystem.release.dto;

import miu.cs544.releasesystem.release.domain.Role;
import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}
