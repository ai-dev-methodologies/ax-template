package com.ax.template.authblueprint.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 12, max = 128)
    private String password;

    /**
     * Optional role used by black-box compliance tests (Payment blueprint) to obtain a
     * token for ADMIN/MANAGER users without injecting UserRepository. Honored only when
     * auth.signup.allow-role-override=true (default true in templates; operators may
     * disable in production). Defaults to MEMBER when absent.
     */
    private String role;

    public SignupRequest() {}

    public SignupRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
