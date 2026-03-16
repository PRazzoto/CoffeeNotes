package com.example.coffeenotes.api.controller;

import com.example.coffeenotes.api.dto.bean.BeanResponseDTO;
import com.example.coffeenotes.config.SecurityConfig;
import com.example.coffeenotes.feature.catalog.service.CoffeeBeanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoffeeBeanController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class CoffeeBeanControllerTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BEAN_ID_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID BEAN_ID_2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CoffeeBeanService coffeeBeanService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void createBean_returns201AndBody() throws Exception {
        BeanResponseDTO response = beanResponse(BEAN_ID_1, "Ethiopia", true, "Yirgacheffe");
        when(coffeeBeanService.createBean(eq(USER_ID), any())).thenReturn(response);

        mockMvc.perform(post("/api/coffeeBean/createCoffeeBean")
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Ethiopia",
                                  "global": true,
                                  "roaster": "April",
                                  "origin": "Yirgacheffe",
                                  "process": "Washed",
                                  "notes": "Floral"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(BEAN_ID_1.toString()))
                .andExpect(jsonPath("$.name").value("Ethiopia"))
                .andExpect(jsonPath("$.global").value(true))
                .andExpect(jsonPath("$.roaster").value("April"))
                .andExpect(jsonPath("$.origin").value("Yirgacheffe"))
                .andExpect(jsonPath("$.process").value("Washed"))
                .andExpect(jsonPath("$.notes").value("Floral"));
    }

    @Test
    void listBeans_returnsDtos() throws Exception {
        when(coffeeBeanService.listBeans(USER_ID)).thenReturn(List.of(
                beanResponse(BEAN_ID_1, "Kenya", false, "Nyeri"),
                beanResponse(BEAN_ID_2, "Colombia", true, "Huila")
        ));

        mockMvc.perform(get("/api/coffeeBean/listCoffeeBean")
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Kenya", "Colombia")))
                .andExpect(jsonPath("$[*].global", containsInAnyOrder(false, true)));
    }

    @Test
    void createBean_whenServiceThrows400_returns400() throws Exception {
        when(coffeeBeanService.createBean(eq(USER_ID), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required."));

        mockMvc.perform(post("/api/coffeeBean/createCoffeeBean")
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                        .contentType("application/json")
                        .content("""
                                {
                                  "roaster": "April"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBean_whenJwtMissing_returns401() throws Exception {
        mockMvc.perform(post("/api/coffeeBean/createCoffeeBean")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Ethiopia"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listBeans_whenJwtMissing_returns401() throws Exception {
        mockMvc.perform(get("/api/coffeeBean/listCoffeeBean"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listBeans_whenTokenSubjectInvalid_returns401() throws Exception {
        mockMvc.perform(get("/api/coffeeBean/listCoffeeBean")
                        .with(jwt().jwt(token -> token.subject("not-a-uuid"))))
                .andExpect(status().isUnauthorized());
    }

    private BeanResponseDTO beanResponse(UUID beanId, String name, boolean global, String origin) {
        BeanResponseDTO dto = new BeanResponseDTO();
        dto.setId(beanId);
        dto.setName(name);
        dto.setGlobal(global);
        dto.setRoaster("April");
        dto.setOrigin(origin);
        dto.setProcess("Washed");
        dto.setNotes("Floral");
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }
}
