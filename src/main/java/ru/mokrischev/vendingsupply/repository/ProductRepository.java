package ru.mokrischev.vendingsupply.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mokrischev.vendingsupply.model.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    java.util.List<Product> findByActiveTrue();
}
