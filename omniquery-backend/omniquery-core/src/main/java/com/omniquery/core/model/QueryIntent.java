package com.omniquery.core.model;

import com.omniquery.sdk.model.UserContext;

public record QueryIntent(
    String originalQuestion,
    String normalizedQuestion,
    String dialect,
    UserContext userContext
) {}
