package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    List<User> findAllActive();

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsActiveByEmail(@Param("email") String email);

    List<User> findTop5ByOrderByCreatedAtDesc();

    @Query(value = "SELECT * FROM users", nativeQuery = true)
    List<User> findAllIncludingDeleted();

    @Query(value = "SELECT * FROM users WHERE user_id = :id", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@Param("id") String id);

    @Query(value = "SELECT * FROM users WHERE (deleted_at IS NULL OR :includeDeleted = true) AND (:search IS NULL OR :search = '' OR email LIKE CONCAT('%', :search, '%') OR username LIKE CONCAT('%', :search, '%') OR user_id LIKE CONCAT('%', :search, '%'))", nativeQuery = true, countQuery = "SELECT count(*) FROM users WHERE (deleted_at IS NULL OR :includeDeleted = true) AND (:search IS NULL OR :search = '' OR email LIKE CONCAT('%', :search, '%') OR username LIKE CONCAT('%', :search, '%') OR user_id LIKE CONCAT('%', :search, '%'))")
    Page<User> findByAdminFilterNative(@Param("search") String search, @Param("includeDeleted") boolean includeDeleted, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM users WHERE user_id = :id", nativeQuery = true)
    void hardDeleteById(@Param("id") String id);
}
