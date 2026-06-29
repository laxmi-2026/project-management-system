package com.pms.repository;

import com.pms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA generates the implementation of this interface
 * automatically at startup — we never write the actual SQL for these
 * simple lookups, Spring builds it from the method name itself.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}