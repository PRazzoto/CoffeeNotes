package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.bean.BeanResponseDTO;
import com.example.coffeenotes.api.dto.bean.CreateBeanRequestDTO;
import com.example.coffeenotes.domain.catalog.CoffeeBean;
import com.example.coffeenotes.domain.user.User;
import com.example.coffeenotes.feature.catalog.repository.CoffeeBeanRepository;
import com.example.coffeenotes.feature.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CoffeeBeanService {
    private final CoffeeBeanRepository coffeeBeanRepository;
    private final UserRepository userRepository;

    public BeanResponseDTO createBean(UUID userId, CreateBeanRequestDTO dto) {
        if(dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body must not be null.");
        }
        if(dto.getName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required.");
        }
        String name = dto.getName().trim();
        if(name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required.");
        }

        if(userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UserId is missing.");
        }
        CoffeeBean bean = new CoffeeBean();
        bean.setName(name);
        // User-created beans are always private to avoid public catalog sprawl.
        bean.setGlobal(false);
        bean.setRoaster(dto.getRoaster());
        bean.setOrigin(dto.getOrigin());
        bean.setProcess(dto.getProcess());
        bean.setNotes(dto.getNotes());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        bean.setOwner(user);

        CoffeeBean repoResponse = coffeeBeanRepository.save(bean);
        BeanResponseDTO ans = new BeanResponseDTO();
        ans.setId(repoResponse.getId());
        ans.setName(repoResponse.getName());
        ans.setGlobal(repoResponse.isGlobal());
        ans.setRoaster(repoResponse.getRoaster());
        ans.setOrigin(repoResponse.getOrigin());
        ans.setProcess(repoResponse.getProcess());
        ans.setNotes(repoResponse.getNotes());
        ans.setCreatedAt(repoResponse.getCreatedAt());
        ans.setUpdatedAt(repoResponse.getUpdatedAt());
        return ans;
    }


    public List<BeanResponseDTO> listBeans(UUID userId) {
        if(userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UserId is missing.");
        }
        List<CoffeeBean> beans = coffeeBeanRepository.findVisibleBeans(userId);

        return beans.stream().map(bean -> {
            BeanResponseDTO dto = new BeanResponseDTO();
            dto.setId(bean.getId());
            dto.setName(bean.getName());
            dto.setRoaster(bean.getRoaster());
            dto.setGlobal(bean.isGlobal());
            dto.setNotes(bean.getNotes());
            dto.setProcess(bean.getProcess());
            dto.setOrigin(bean.getOrigin());
            dto.setCreatedAt(bean.getCreatedAt());
            dto.setUpdatedAt(bean.getUpdatedAt());
            return dto;
        }).toList();

    }
}
