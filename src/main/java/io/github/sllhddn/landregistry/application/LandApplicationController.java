package io.github.sllhddn.landregistry.application;

import io.github.sllhddn.landregistry.application.ApplicationDtos.ApplicationResponse;
import io.github.sllhddn.landregistry.application.ApplicationDtos.CreateApplicationRequest;
import io.github.sllhddn.landregistry.application.ApplicationDtos.TransitionRequest;
import io.github.sllhddn.landregistry.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class LandApplicationController {

    private final LandApplicationService service;

    public LandApplicationController(LandApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> submit(@Valid @RequestBody CreateApplicationRequest request) {
        LandApplication saved = service.submit(request);
        return ResponseEntity.created(URI.create("/api/applications/" + saved.getId()))
                .body(ApplicationResponse.from(saved));
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable Long id) {
        return ApplicationResponse.from(service.get(id));
    }

    @GetMapping
    public PageResponse<ApplicationResponse> list(
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 20, sort = "submittedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(service.list(status, pageable).map(ApplicationResponse::from));
    }

    @PostMapping("/{id}/transitions")
    public ApplicationResponse transition(@PathVariable Long id, @Valid @RequestBody TransitionRequest request) {
        return ApplicationResponse.from(service.transition(id, request.status(), request.remarks()));
    }

    @GetMapping("/summary")
    public Map<ApplicationStatus, Long> summary() {
        Map<ApplicationStatus, Long> result = new LinkedHashMap<>();
        service.summary().forEach(c -> result.put(c.getStatus(), c.getTotal()));
        return result;
    }
}
