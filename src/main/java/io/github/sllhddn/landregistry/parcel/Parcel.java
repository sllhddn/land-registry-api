package io.github.sllhddn.landregistry.parcel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parcel")
public class Parcel {

    /*
     * allocationSize MUST equal the sequence's INCREMENT BY (see V1__init.sql).
     * Hibernate reserves a block of 50 ids per database call. If the two values
     * disagree, the ids Hibernate hands out can overlap with ids the sequence
     * gives to other writers (older Hibernate versions, scripts) -> duplicate keys.
     * Keeping them equal, and validated at startup, avoids that class of bug.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "parcel_seq")
    @SequenceGenerator(name = "parcel_seq", sequenceName = "seq_parcel", allocationSize = 50)
    private Long id;

    @Column(name = "lot_no", nullable = false, unique = true, length = 30)
    private String lotNo;

    @Column(name = "district", nullable = false, length = 60)
    private String district;

    @Column(name = "area_sqm", nullable = false, precision = 16, scale = 4)
    private BigDecimal areaSqm;

    @Column(name = "tenure", nullable = false, length = 20)
    private String tenure;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Parcel() {
        // for JPA
    }

    public Parcel(String lotNo, String district, BigDecimal areaSqm, String tenure) {
        this.lotNo = lotNo;
        this.district = district;
        this.areaSqm = areaSqm;
        this.tenure = tenure;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getLotNo() { return lotNo; }
    public String getDistrict() { return district; }
    public BigDecimal getAreaSqm() { return areaSqm; }
    public String getTenure() { return tenure; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
