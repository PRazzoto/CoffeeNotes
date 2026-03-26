package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.grinder.GrinderCatalogItemDTO;
import com.example.coffeenotes.api.dto.grinder.GrinderConversionRequestDTO;
import com.example.coffeenotes.api.dto.grinder.GrinderConversionResponseDTO;
import com.example.coffeenotes.feature.catalog.service.GrinderConversionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/grinder-conversion")
public class GrinderConversionController {
    private final GrinderConversionService grinderConversionService;

    public GrinderConversionController(GrinderConversionService grinderConversionService) {
        this.grinderConversionService = grinderConversionService;
    }

    @GetMapping("/grinders")
    public List<GrinderCatalogItemDTO> listSupportedGrinders() {
        return grinderConversionService.listSupportedGrinders();
    }

    @PostMapping("/convert")
    public GrinderConversionResponseDTO convert(@RequestBody GrinderConversionRequestDTO body) {
        return grinderConversionService.convert(body);
    }
}

