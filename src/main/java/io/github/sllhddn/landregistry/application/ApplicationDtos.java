package io.github.sllhddn.landregistry.application;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    public record CreateApplicationRequest(
            @NotNull Long parcelId,
            @NotNull ApplicationType type,
            @NotBlank @Size(max = 250) String applicantName,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal feeAmount) {
    }

    public record TransitionRequest(@NotNull ApplicationStatus status, @Size(max = 4000) String remarks) {
    }

    public record ApplicationResponse(Long id, String referenceNo, Long parcelId, String lotNo, ApplicationType type,
                                      ApplicationStatus status, String applicantName, BigDecimal feeAmount,
                                      String remarks, LocalDateTime submittedAt, LocalDateTime decidedAt) {

        static ApplicationResponse from(LandApplication a) {
            return new ApplicationResponse(a.getId(), a.getReferenceNo(), a.getParcel().getId(),
                    a.getParcel().getLotNo(), a.getType(), a.getStatus(), a.getApplicantName(), a.getFeeAmount(),
                    a.getRemarks(), a.getSubmittedAt(), a.getDecidedAt());
        }
    }
}
