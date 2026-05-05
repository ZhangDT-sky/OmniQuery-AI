package com.omniquery.sdk.model;

import java.util.List;

public record UserContext(
    String userId,
    String tenantId,
    List<String> roles
) {
    public static UserContext defaultUser() {
        return new UserContext("default-user", "tenant_a", List.of("user"));
    }
}
