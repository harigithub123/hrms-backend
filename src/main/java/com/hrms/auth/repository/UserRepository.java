package com.hrms.auth.repository;

import com.hrms.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByEmployeeId(Long employeeId);

    /** Used for sign-in when the user types their work email instead of employee id. */
    List<User> findByEmailIgnoreCase(String email);
}
