package com.packersmovers.marketplace.controller;

import com.packersmovers.marketplace.common.response.ApiResponse;
import com.packersmovers.marketplace.repository.ServiceAreaRepository;
import com.packersmovers.marketplace.repository.ServiceCategoryRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Read-only reference data the customer form / provider registration form needs to render dropdowns. */
@RestController
@RequestMapping("/api/public/catalog")
@RequiredArgsConstructor
@Tag(name = "Public Catalog", description = "Service categories and service areas for form dropdowns")
public class PublicCatalogController {

    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ServiceAreaRepository serviceAreaRepository;

    @GetMapping("/service-categories")
    public ApiResponse<List<Map<String, Object>>> serviceCategories() {
        var result = serviceCategoryRepository.findByActiveTrue().stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName()))
                .toList();
        return ApiResponse.success(result);
    }

    @GetMapping("/service-areas")
    public ApiResponse<List<Map<String, Object>>> serviceAreas() {
        var result = serviceAreaRepository.findByActiveTrue().stream()
                .map(a -> Map.<String, Object>of(
                        "id", a.getId(), "cityOrRegion", a.getCityOrRegion(), "state", a.getState() == null ? "" : a.getState()))
                .toList();
        return ApiResponse.success(result);
    }
}
