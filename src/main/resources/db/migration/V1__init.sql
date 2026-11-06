-- Schema is owned by Flyway; Hibernate runs with ddl-auto=validate.
-- Sequence INCREMENT BY must match @SequenceGenerator(allocationSize) in the entities.

CREATE SEQUENCE seq_parcel START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE seq_land_application START WITH 1 INCREMENT BY 50;

CREATE TABLE parcel (
    id          BIGINT        NOT NULL,
    lot_no      VARCHAR(30)   NOT NULL,
    district    VARCHAR(60)   NOT NULL,
    area_sqm    NUMERIC(16,4) NOT NULL,
    tenure      VARCHAR(20)   NOT NULL,
    created_at  TIMESTAMP     NOT NULL,
    CONSTRAINT pk_parcel PRIMARY KEY (id),
    CONSTRAINT uk_parcel_lot_no UNIQUE (lot_no),
    CONSTRAINT ck_parcel_tenure CHECK (tenure IN ('FREEHOLD', 'LEASEHOLD')),
    CONSTRAINT ck_parcel_area CHECK (area_sqm > 0)
);

CREATE TABLE land_application (
    id              BIGINT         NOT NULL,
    reference_no    VARCHAR(30)    NOT NULL,
    parcel_id       BIGINT         NOT NULL,
    type            VARCHAR(20)    NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    applicant_name  VARCHAR(250)   NOT NULL,
    fee_amount      NUMERIC(12,2)  NOT NULL,
    remarks         VARCHAR(4000),
    submitted_at    TIMESTAMP      NOT NULL,
    decided_at      TIMESTAMP,
    version         BIGINT         DEFAULT 0 NOT NULL,
    CONSTRAINT pk_land_application PRIMARY KEY (id),
    CONSTRAINT uk_land_application_ref UNIQUE (reference_no),
    CONSTRAINT fk_land_application_parcel FOREIGN KEY (parcel_id) REFERENCES parcel (id),
    CONSTRAINT ck_land_application_status
        CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'REGISTERED', 'WITHDRAWN')),
    CONSTRAINT ck_land_application_type
        CHECK (type IN ('TRANSFER', 'CHARGE', 'LEASE', 'CAVEAT', 'SUBDIVISION'))
);

CREATE INDEX ix_land_application_parcel ON land_application (parcel_id);
CREATE INDEX ix_land_application_status ON land_application (status);
