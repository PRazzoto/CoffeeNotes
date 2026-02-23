package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.catalog.BrewMethodsDTO;
import com.example.coffeenotes.domain.catalog.BrewMethods;
import com.example.coffeenotes.feature.catalog.repository.BrewMethodsRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class BrewMethodsService {
    private final BrewMethodsRepository brewMethodsRepository;

    public List<BrewMethods> listAllBrewMethods() {
        return brewMethodsRepository.findAll();
    }

    public BrewMethods add(BrewMethods brewMethods) {
        if(brewMethods.getName() == null || brewMethods.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        return brewMethodsRepository.save(brewMethods);
    }

    public void delete(UUID id){
        if(!brewMethodsRepository.existsById(id)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "BrewMethods not found");
        }

        brewMethodsRepository.deleteById(id);
    }

    public BrewMethods update(UUID id, BrewMethodsDTO body){
        BrewMethods existing = brewMethodsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BrewMethods not found"));
        if(body.getName()==null && body.getDescription()==null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name or Description is required");
        }
        if(body.getName() != null) {
            if(body.getName().isBlank()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must not be blank");
            }
            existing.setName(body.getName());
        }
        if(body.getDescription() != null) {
            if(body.getDescription().isBlank()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description must not be blank");
            }
            existing.setDescription(body.getDescription());
        }
        return brewMethodsRepository.save(existing);
    }

}
