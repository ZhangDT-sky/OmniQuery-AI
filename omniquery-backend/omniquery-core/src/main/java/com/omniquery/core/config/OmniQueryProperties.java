package com.omniquery.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

@ConfigurationProperties(prefix = "omniquery")
public class OmniQueryProperties {

    private final Security security = new Security();
    private final Session session = new Session();

    public Security security() {
        return security;
    }

    public Security getSecurity() {
        return security;
    }

    public Session session() {
        return session;
    }

    public Session getSession() {
        return session;
    }

    public static class Security {
        private int maxRows = 100;
        private int maxJoins = 3;
        private int queryTimeoutSeconds = 10;
        private Set<String> dangerousFunctions = new HashSet<>(Set.of("sleep", "benchmark", "load_file"));

        public int getMaxRows() {
            return maxRows;
        }

        public void setMaxRows(int maxRows) {
            this.maxRows = maxRows;
        }

        public int getMaxJoins() {
            return maxJoins;
        }

        public void setMaxJoins(int maxJoins) {
            this.maxJoins = maxJoins;
        }

        public int getQueryTimeoutSeconds() {
            return queryTimeoutSeconds;
        }

        public void setQueryTimeoutSeconds(int queryTimeoutSeconds) {
            this.queryTimeoutSeconds = queryTimeoutSeconds;
        }

        public Set<String> getDangerousFunctions() {
            return dangerousFunctions;
        }

        public void setDangerousFunctions(Set<String> dangerousFunctions) {
            this.dangerousFunctions = dangerousFunctions;
        }
    }

    public static class Session {
        private int maxTurns = 10;
        private int ttlMinutes = 30;

        public int getMaxTurns() {
            return maxTurns;
        }

        public void setMaxTurns(int maxTurns) {
            this.maxTurns = maxTurns;
        }

        public int getTtlMinutes() {
            return ttlMinutes;
        }

        public void setTtlMinutes(int ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }
    }
}
