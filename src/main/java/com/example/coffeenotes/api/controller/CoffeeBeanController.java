package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.bean.BeanResponseDTO;
import com.example.coffeenotes.api.dto.bean.CreateBeanRequestDTO;
import com.example.coffeenotes.domain.catalog.CoffeeBean;
import com.example.coffeenotes.feature.catalog.service.CoffeeBeanService;
import com.example.coffeenotes.util.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/coffeeBean")
public class CoffeeBeanController {

    private final CoffeeBeanService coffeeBeanService;

    public CoffeeBeanController(CoffeeBeanService coffeeBeanService) {
        this.coffeeBeanService = coffeeBeanService;
    }

    @PostMapping("/createCoffeeBean")
    public ResponseEntity<BeanResponseDTO> createBean(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateBeanRequestDTO dto) {
        UUID userId = JwtUtils.extractUserId(jwt);
        BeanResponseDTO created = coffeeBeanService.createBean(userId, dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/listCoffeeBean")
    public List<BeanResponseDTO> listBeans(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = JwtUtils.extractUserId(jwt);
        return coffeeBeanService.listBean(userId);
    }
}
