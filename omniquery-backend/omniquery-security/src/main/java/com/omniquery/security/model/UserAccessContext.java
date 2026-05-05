package com.omniquery.security.model;

import java.util.Set;

public record UserAccessContext(String userId, String tenantId, Set<String> roles) {
    public boolean isAdmin() {
        return roles != null && roles.contains("admin");
    }
}
