package io.github.sllhddn.landregistry.parcel;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ParcelDtos {

    private ParcelDtos() {
    }

    public record CreateParcelRequest(
            @NotBlank @Size(max = 30) String lotNo,
            @NotBlank @Size(max = 60) String district,
            // validation mirrors the column: numeric(16,4) -> 12 integer digits, 4 decimals
            @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 12, fraction = 4) BigDecimal areaSqm,
            @NotBlank @Pattern(regexp = "FREEHOLD|LEASEHOLD") String tenure) {
    }

    public record ParcelResponse(Long id, String lotNo, String district, BigDecimal areaSqm, String tenure,
                                 LocalDateTime createdAt) {

        static ParcelResponse from(Parcel p) {
            return new ParcelResponse(p.getId(), p.getLotNo(), p.getDistrict(), p.getAreaSqm(), p.getTenure(),
                    p.getCreatedAt());
        }
    }
}
