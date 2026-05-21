package com.ax.template.authblueprint.apikey;

import java.util.List;

public record ApiKeyListResponse(List<ApiKeyResponse> items, long totalElements) {}
