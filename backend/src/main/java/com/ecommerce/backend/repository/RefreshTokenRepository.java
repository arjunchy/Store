package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT r FROM RefreshToken r WHERE r.token = :token")
    Optional<RefreshToken> findByTokenForUpdate(@org.springframework.data.repository.query.Param("token") String token);

    Optional<RefreshToken> findByToken(String token);

    long countByUserUserIdAndRevokedFalse(String userId);

    java.util.List<RefreshToken> findByUserUserId(String userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM RefreshToken r WHERE r.user.userId = :userId")
    void deleteAllByUserId(@org.springframework.data.repository.query.Param("userId") String userId);
}
