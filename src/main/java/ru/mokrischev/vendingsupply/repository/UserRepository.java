package ru.mokrischev.vendingsupply.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mokrischev.vendingsupply.model.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    java.util.List<User> findByRole(ru.mokrischev.vendingsupply.model.enums.Role role);
}
