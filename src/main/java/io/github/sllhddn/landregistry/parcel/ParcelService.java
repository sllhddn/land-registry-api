package io.github.sllhddn.landregistry.parcel;

import io.github.sllhddn.landregistry.common.ConflictException;
import io.github.sllhddn.landregistry.common.NotFoundException;
import io.github.sllhddn.landregistry.parcel.ParcelDtos.CreateParcelRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ParcelService {

    private final ParcelRepository repository;

    public ParcelService(ParcelRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Parcel create(CreateParcelRequest req) {
        String lotNo = req.lotNo().trim().toUpperCase();
        if (repository.existsByLotNo(lotNo)) {
            throw new ConflictException("Parcel with lot no " + lotNo + " already exists");
        }
        return repository.save(new Parcel(lotNo, req.district().trim(), req.areaSqm(), req.tenure()));
    }

    public Parcel get(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Parcel " + id + " not found"));
    }

    public Page<Parcel> list(Pageable pageable) {
        return repository.findAll(pageable);
    }
}
