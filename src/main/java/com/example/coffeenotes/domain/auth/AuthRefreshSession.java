package com.example.coffeenotes.domain.auth;


import com.example.coffeenotes.domain.catalog.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_refresh_sessions", schema = "coffeenotes")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthRefreshSession {

    private @Id
    @GeneratedValue UUID id;

   @ManyToOne(fetch = FetchType.LAZY, optional = false)
   @JoinColumn(name = "user_id", nullable = false)
   private User user;

   @Column(name = "token_hash", nullable = false)
   private String tokenHash;

   @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private String ip;

    @Column(name = "user_agent")
    private String userAgent;
}
