package com.ax.template.authblueprint.apikey;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateApiKeyRequest(
    @Size(max = 128) String name,
    List<ApiKeyScope> scopes,
    @Min(1) @Max(3650) Integer expiresInDays
) {}
