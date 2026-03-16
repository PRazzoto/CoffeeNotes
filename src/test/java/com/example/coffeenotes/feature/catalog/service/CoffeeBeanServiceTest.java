package com.example.coffeenotes.feature.catalog.service;

import com.example.coffeenotes.api.dto.bean.BeanResponseDTO;
import com.example.coffeenotes.api.dto.bean.CreateBeanRequestDTO;
import com.example.coffeenotes.domain.catalog.CoffeeBean;
import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.domain.user.User;
import com.example.coffeenotes.feature.catalog.repository.CoffeeBeanRepository;
import com.example.coffeenotes.feature.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoffeeBeanServiceTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BEAN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 3, 16, 10, 30);
    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 3, 16, 10, 45);

    @Mock
    private CoffeeBeanRepository coffeeBeanRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CoffeeBeanService coffeeBeanService;

    @Test
    void createBean_whenBodyNull_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> coffeeBeanService.createBean(USER_ID, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).findById(any());
        verify(coffeeBeanRepository, never()).save(any());
    }

    @Test
    void createBean_whenNameNull_throws400() {
        CreateBeanRequestDTO dto = new CreateBeanRequestDTO();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> coffeeBeanService.createBean(USER_ID, dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).findById(any());
        verify(coffeeBeanRepository, never()).save(any());
    }

    @Test
    void createBean_whenNameBlankAfterTrim_throws400() {
        CreateBeanRequestDTO dto = new CreateBeanRequestDTO();
        dto.setName("   ");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> coffeeBeanService.createBean(USER_ID, dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).findById(any());
        verify(coffeeBeanRepository, never()).save(any());
    }

    @Test
    void createBean_whenUserIdNull_throws400() {
        CreateBeanRequestDTO dto = validCreateDto();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> coffeeBeanService.createBean(null, dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).findById(any());
        verify(coffeeBeanRepository, never()).save(any());
    }

    @Test
    void createBean_whenUserMissing_throws404() {
        CreateBeanRequestDTO dto = validCreateDto();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> coffeeBeanService.createBean(USER_ID, dto)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(coffeeBeanRepository, never()).save(any());
    }

    @Test
    void createBean_whenValid_persistsTrimmedNameAndMapsResponse() {
        CreateBeanRequestDTO dto = validCreateDto();
        dto.setName("  Ethiopia  ");
        User owner = user(USER_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(coffeeBeanRepository.save(any(CoffeeBean.class))).thenAnswer(invocation -> {
            CoffeeBean bean = invocation.getArgument(0);
            bean.setId(BEAN_ID);
            bean.setCreatedAt(CREATED_AT);
            bean.setUpdatedAt(UPDATED_AT);
            return bean;
        });

        BeanResponseDTO out = coffeeBeanService.createBean(USER_ID, dto);

        ArgumentCaptor<CoffeeBean> captor = ArgumentCaptor.forClass(CoffeeBean.class);
        verify(coffeeBeanRepository).save(captor.capture());
        CoffeeBean saved = captor.getValue();

        assertEquals("Ethiopia", saved.getName());
        assertTrue(saved.isGlobal());
        assertEquals("April", saved.getRoaster());
        assertEquals("Yirgacheffe", saved.getOrigin());
        assertEquals("Washed", saved.getProcess());
        assertEquals("Floral and citrus", saved.getNotes());
        assertEquals(owner, saved.getOwner());

        assertEquals(BEAN_ID, out.getId());
        assertEquals("Ethiopia", out.getName());
        assertTrue(out.isGlobal());
        assertEquals("April", out.getRoaster());
        assertEquals("Yirgacheffe", out.getOrigin());
        assertEquals("Washed", out.getProcess());
        assertEquals("Floral and citrus", out.getNotes());
        assertEquals(CREATED_AT, out.getCreatedAt());
        assertEquals(UPDATED_AT, out.getUpdatedAt());
    }

    @Test
    void listBeans_whenUserIdNull_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> coffeeBeanService.listBeans(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(coffeeBeanRepository, never()).findVisibleBeans(any());
    }

    @Test
    void listBeans_whenNoBeans_returnsEmptyList() {
        when(coffeeBeanRepository.findVisibleBeans(USER_ID)).thenReturn(List.of());

        List<BeanResponseDTO> out = coffeeBeanService.listBeans(USER_ID);

        assertTrue(out.isEmpty());
    }

    @Test
    void listBeans_whenVisibleBeansExist_mapsAllFields() {
        CoffeeBean ownBean = bean(BEAN_ID, "Kenya", false);
        ownBean.setRoaster("Dak");
        ownBean.setOrigin("Nyeri");
        ownBean.setProcess("Washed");
        ownBean.setNotes("Berry");
        ownBean.setCreatedAt(CREATED_AT);
        ownBean.setUpdatedAt(UPDATED_AT);

        CoffeeBean globalBean = bean(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "Colombia", true);
        globalBean.setCreatedAt(CREATED_AT.plusDays(1));
        globalBean.setUpdatedAt(UPDATED_AT.plusDays(1));

        when(coffeeBeanRepository.findVisibleBeans(USER_ID)).thenReturn(List.of(ownBean, globalBean));

        List<BeanResponseDTO> out = coffeeBeanService.listBeans(USER_ID);

        assertEquals(2, out.size());

        BeanResponseDTO first = out.get(0);
        assertEquals(BEAN_ID, first.getId());
        assertEquals("Kenya", first.getName());
        assertFalse(first.isGlobal());
        assertEquals("Dak", first.getRoaster());
        assertEquals("Nyeri", first.getOrigin());
        assertEquals("Washed", first.getProcess());
        assertEquals("Berry", first.getNotes());
        assertEquals(CREATED_AT, first.getCreatedAt());
        assertEquals(UPDATED_AT, first.getUpdatedAt());

        BeanResponseDTO second = out.get(1);
        assertTrue(second.isGlobal());
        assertEquals("Colombia", second.getName());
    }

    private CreateBeanRequestDTO validCreateDto() {
        CreateBeanRequestDTO dto = new CreateBeanRequestDTO();
        dto.setName("Ethiopia");
        dto.setGlobal(true);
        dto.setRoaster("April");
        dto.setOrigin("Yirgacheffe");
        dto.setProcess("Washed");
        dto.setNotes("Floral and citrus");
        return dto;
    }

    private User user(UUID id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@test.com");
        user.setPasswordHash("hash");
        user.setDisplayName("User");
        user.setRole(Role.USER);
        return user;
    }

    private CoffeeBean bean(UUID id, String name, boolean global) {
        CoffeeBean bean = new CoffeeBean();
        bean.setId(id);
        bean.setName(name);
        bean.setGlobal(global);
        return bean;
    }
}
