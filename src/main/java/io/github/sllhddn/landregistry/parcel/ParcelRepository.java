package io.github.sllhddn.landregistry.parcel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParcelRepository extends JpaRepository<Parcel, Long> {

    Optional<Parcel> findByLotNo(String lotNo);

    boolean existsByLotNo(String lotNo);
}
