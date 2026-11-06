package io.github.sllhddn.landregistry.application;

import io.github.sllhddn.landregistry.application.ApplicationDtos.CreateApplicationRequest;
import io.github.sllhddn.landregistry.application.LandApplicationRepository.StatusCount;
import io.github.sllhddn.landregistry.common.ConflictException;
import io.github.sllhddn.landregistry.common.NotFoundException;
import io.github.sllhddn.landregistry.parcel.Parcel;
import io.github.sllhddn.landregistry.parcel.ParcelService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LandApplicationService {

    private static final Set<ApplicationStatus> OPEN_STATUSES = EnumSet.of(
            ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED);

    private final LandApplicationRepository repository;
    private final ParcelService parcelService;

    public LandApplicationService(LandApplicationRepository repository, ParcelService parcelService) {
        this.repository = repository;
        this.parcelService = parcelService;
    }

    @Transactional
    public LandApplication submit(CreateApplicationRequest req) {
        Parcel parcel = parcelService.get(req.parcelId());
        if (repository.existsByParcel_IdAndTypeAndStatusIn(parcel.getId(), req.type(), OPEN_STATUSES)) {
            throw new ConflictException("Parcel " + parcel.getLotNo() + " already has an open " + req.type()
                    + " application");
        }
        String ref = "APP/" + Year.now() + "/" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return repository.save(new LandApplication(ref, parcel, req.type(), req.applicantName().trim(),
                req.feeAmount()));
    }

    public LandApplication get(Long id) {
        return repository.findWithParcelById(id)
                .orElseThrow(() -> new NotFoundException("Application " + id + " not found"));
    }

    public Page<LandApplication> list(ApplicationStatus status, Pageable pageable) {
        return status == null ? repository.findAllBy(pageable) : repository.findByStatus(status, pageable);
    }

    @Transactional
    public LandApplication transition(Long id, ApplicationStatus next, String remarks) {
        LandApplication app = get(id);
        app.moveTo(next, remarks);
        return app; // dirty checking flushes the change at commit
    }

    public List<StatusCount> summary() {
        return repository.countByStatus();
    }
}
