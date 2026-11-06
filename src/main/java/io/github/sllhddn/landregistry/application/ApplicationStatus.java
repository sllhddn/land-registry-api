package io.github.sllhddn.landregistry.application;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a land dealing application.
 *
 * SUBMITTED -> UNDER_REVIEW -> APPROVED -> REGISTERED
 *     |             |        \
 *     +-> WITHDRAWN +-> REJECTED
 */
public enum ApplicationStatus {
    SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, REGISTERED, WITHDRAWN;

    public Set<ApplicationStatus> allowedNext() {
        return switch (this) {
            case SUBMITTED -> EnumSet.of(UNDER_REVIEW, WITHDRAWN);
            case UNDER_REVIEW -> EnumSet.of(APPROVED, REJECTED, WITHDRAWN);
            case APPROVED -> EnumSet.of(REGISTERED);
            case REJECTED, REGISTERED, WITHDRAWN -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }

    public boolean canMoveTo(ApplicationStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isFinal() {
        return allowedNext().isEmpty();
    }
}
