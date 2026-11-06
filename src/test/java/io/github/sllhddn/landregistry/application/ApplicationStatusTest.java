package io.github.sllhddn.landregistry.application;

import io.github.sllhddn.landregistry.common.ConflictException;
import io.github.sllhddn.landregistry.parcel.Parcel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationStatusTest {

    @ParameterizedTest
    @CsvSource({
            "SUBMITTED, UNDER_REVIEW, true",
            "SUBMITTED, WITHDRAWN, true",
            "SUBMITTED, APPROVED, false",
            "UNDER_REVIEW, APPROVED, true",
            "UNDER_REVIEW, REJECTED, true",
            "APPROVED, REGISTERED, true",
            "APPROVED, SUBMITTED, false",
            "REJECTED, UNDER_REVIEW, false",
            "REGISTERED, WITHDRAWN, false"
    })
    void transitions(ApplicationStatus from, ApplicationStatus to, boolean allowed) {
        assertThat(from.canMoveTo(to)).isEqualTo(allowed);
    }

    @Test
    void finalStatesHaveNoNextStep() {
        assertThat(ApplicationStatus.REJECTED.isFinal()).isTrue();
        assertThat(ApplicationStatus.REGISTERED.isFinal()).isTrue();
        assertThat(ApplicationStatus.WITHDRAWN.isFinal()).isTrue();
        assertThat(ApplicationStatus.UNDER_REVIEW.isFinal()).isFalse();
    }

    @Test
    void entityRejectsIllegalMoveAndStampsDecision() {
        Parcel parcel = new Parcel("LOT-1", "District A", new BigDecimal("100.5"), "FREEHOLD");
        LandApplication app = new LandApplication("APP/1", parcel, ApplicationType.TRANSFER, "Ali", BigDecimal.TEN);

        assertThatThrownBy(() -> app.moveTo(ApplicationStatus.APPROVED, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("SUBMITTED to APPROVED");

        app.moveTo(ApplicationStatus.UNDER_REVIEW, null);
        assertThat(app.getDecidedAt()).isNull();
        app.moveTo(ApplicationStatus.APPROVED, "documents complete");
        assertThat(app.getDecidedAt()).isNotNull();
        assertThat(app.getRemarks()).isEqualTo("documents complete");
    }
}
