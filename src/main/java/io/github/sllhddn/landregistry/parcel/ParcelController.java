package io.github.sllhddn.landregistry.parcel;

import io.github.sllhddn.landregistry.common.PageResponse;
import io.github.sllhddn.landregistry.parcel.ParcelDtos.CreateParcelRequest;
import io.github.sllhddn.landregistry.parcel.ParcelDtos.ParcelResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/parcels")
public class ParcelController {

    private final ParcelService service;

    public ParcelController(ParcelService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ParcelResponse> create(@Valid @RequestBody CreateParcelRequest request) {
        Parcel saved = service.create(request);
        return ResponseEntity.created(URI.create("/api/parcels/" + saved.getId())).body(ParcelResponse.from(saved));
    }

    @GetMapping("/{id}")
    public ParcelResponse get(@PathVariable Long id) {
        return ParcelResponse.from(service.get(id));
    }

    @GetMapping
    public PageResponse<ParcelResponse> list(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PageResponse.of(service.list(pageable).map(ParcelResponse::from));
    }
}
