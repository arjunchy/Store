package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, String> {

    @Query("SELECT a FROM Address a WHERE a.user.userId = :userId")
    List<Address> findByUserId(@Param("userId") String userId);

    boolean existsByUserUserId(String userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.userId = :userId AND a.isDefault = true")
    int unsetOtherDefaults(@Param("userId") String userId);
}
