package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.BrewMethodsDTO;
import com.example.coffeenotes.domain.catalog.BrewMethods;
import com.example.coffeenotes.feature.catalog.service.BrewMethodsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/brewMethods")
public class BrewMethodsController {

    private final BrewMethodsService brewMethodsService;

    public BrewMethodsController(BrewMethodsService brewMethodsService) {
        this.brewMethodsService = brewMethodsService;
    }

    @GetMapping("/listAll")
    public List<BrewMethodsDTO> allBrewMethods() {
        List<BrewMethods> brewMethods = brewMethodsService.listAllBrewMethods();
        return brewMethods.stream()
                .map(brewMethods -> {
                    BrewMethodsDTO dto = new BrewMethodsDTO();
                    dto.setName(brewMethods.getName());
                    dto.setDescription(brewMethods.getDescription());
                    return dto;
                })
                .toList();
    }

    @PostMapping("/createBrewMethods")
    public ResponseEntity<BrewMethodsDTO> add(@RequestBody BrewMethods brewMethods) {
        BrewMethods addedBrewMethods = this.brewMethodsService.add(brewMethods);
        BrewMethodsDTO dto = new BrewMethodsDTO();
        dto.setName(addedBrewMethods.getName());
        dto.setDescription(addedBrewMethods.getDescription());
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @DeleteMapping("/deleteBrewMethods/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id){
        brewMethodsService.delete(id);
    }

    @PutMapping("/editBrewMethods/{id}")
    public ResponseEntity<BrewMethodsDTO> updateBrewMethods(@PathVariable UUID id, @RequestBody BrewMethodsDTO body){
        BrewMethods updated = brewMethodsService.update(id, body);
        BrewMethodsDTO dto = new BrewMethodsDTO();
        dto.setName(updated.getName());
        dto.setDescription(updated.getDescription());
        return new ResponseEntity<>(dto,HttpStatus.OK);
    }
}
