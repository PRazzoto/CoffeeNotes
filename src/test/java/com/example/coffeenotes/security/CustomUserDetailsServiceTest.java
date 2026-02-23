package com.example.coffeenotes.security;

import com.example.coffeenotes.domain.catalog.Role;
import com.example.coffeenotes.domain.user.User;
import com.example.coffeenotes.feature.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_whenFound_returnsUserDetailsWithRole() {
        User user = new User(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "test@coffee.com",
                "hashed-password",
                "Test User",
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(userRepository.findByEmail("test@coffee.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("test@coffee.com");

        assertEquals("test@coffee.com", details.getUsername());
        assertEquals("hashed-password", details.getPassword());
        assertEquals(1, details.getAuthorities().size());
        assertEquals("ROLE_USER", details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void loadUserByUsername_whenNotFound_throwsUsernameNotFound() {
        when(userRepository.findByEmail("missing@coffee.com")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("missing@coffee.com")
        );
    }
}

