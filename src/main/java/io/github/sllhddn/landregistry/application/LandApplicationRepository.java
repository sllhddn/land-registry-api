package io.github.sllhddn.landregistry.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LandApplicationRepository extends JpaRepository<LandApplication, Long> {

    // fetch the parcel in the same query -> avoids N+1 selects when listing
    @EntityGraph(attributePaths = "parcel")
    Page<LandApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "parcel")
    Page<LandApplication> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = "parcel")
    Optional<LandApplication> findWithParcelById(Long id);

    boolean existsByParcel_IdAndTypeAndStatusIn(Long parcelId, ApplicationType type,
                                               Collection<ApplicationStatus> statuses);

    @Query("select a.status as status, count(a) as total from LandApplication a group by a.status order by a.status")
    List<StatusCount> countByStatus();

    interface StatusCount {
        ApplicationStatus getStatus();

        long getTotal();
    }
}
