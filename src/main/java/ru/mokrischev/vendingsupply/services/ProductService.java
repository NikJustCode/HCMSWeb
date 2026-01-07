package ru.mokrischev.vendingsupply.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mokrischev.vendingsupply.model.entity.Product;
import ru.mokrischev.vendingsupply.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public void save(Product product) {
        if (product.getUnit() == null || product.getUnit().isEmpty()) {
            product.setUnit("шт");
        }
        productRepository.save(product);
    }

    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    public void restore(Long id) {
        Product product = findById(id);
        if (product != null) {
            product.setActive(true);
            save(product);
        }
    }

    public void softDelete(Long id) {
        Product product = findById(id);
        if (product != null) {
            product.setActive(false);
            save(product);
        }
    }

    public Product findById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public List<Product> findAllActive() {
        return productRepository.findByActiveTrue();
    }

}
