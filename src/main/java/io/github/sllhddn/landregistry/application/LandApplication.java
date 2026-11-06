package io.github.sllhddn.landregistry.application;

import io.github.sllhddn.landregistry.common.ConflictException;
import io.github.sllhddn.landregistry.parcel.Parcel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "land_application")
public class LandApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "application_seq")
    @SequenceGenerator(name = "application_seq", sequenceName = "seq_land_application", allocationSize = 50)
    private Long id;

    @Column(name = "reference_no", nullable = false, unique = true, length = 30)
    private String referenceNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parcel_id", nullable = false)
    private Parcel parcel;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 20)
    private ApplicationType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "applicant_name", nullable = false, length = 250)
    private String applicantName;

    @Column(name = "fee_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "remarks", length = 4000)
    private String remarks;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /** Optimistic locking: two officers updating the same application cannot silently overwrite each other. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected LandApplication() {
        // for JPA
    }

    public LandApplication(String referenceNo, Parcel parcel, ApplicationType type, String applicantName,
                           BigDecimal feeAmount) {
        this.referenceNo = referenceNo;
        this.parcel = parcel;
        this.type = type;
        this.applicantName = applicantName;
        this.feeAmount = feeAmount;
        this.status = ApplicationStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    /** Domain rule lives in the entity, so no caller can skip it. */
    public void moveTo(ApplicationStatus next, String remarks) {
        if (!status.canMoveTo(next)) {
            throw new ConflictException("Cannot move application " + referenceNo + " from " + status + " to "
                    + next + "; allowed: " + status.allowedNext());
        }
        this.status = next;
        if (remarks != null && !remarks.isBlank()) {
            this.remarks = remarks;
        }
        if (next == ApplicationStatus.APPROVED || next == ApplicationStatus.REJECTED) {
            this.decidedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getReferenceNo() { return referenceNo; }
    public Parcel getParcel() { return parcel; }
    public ApplicationType getType() { return type; }
    public ApplicationStatus getStatus() { return status; }
    public String getApplicantName() { return applicantName; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public String getRemarks() { return remarks; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public long getVersion() { return version; }
}
