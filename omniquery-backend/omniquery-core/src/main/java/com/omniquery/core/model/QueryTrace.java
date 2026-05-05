package com.omniquery.core.model;

public record QueryTrace(
    String phase,
    String message,
    Object detail
) {}
