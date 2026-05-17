package com.ax.template.authblueprint.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordResetDto {

    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 12, max = 128)
    private String newPassword;

    public PasswordResetDto() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
