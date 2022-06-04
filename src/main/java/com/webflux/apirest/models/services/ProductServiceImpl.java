package com.webflux.apirest.models.services;

import com.webflux.apirest.models.documents.Category;
import com.webflux.apirest.models.documents.Product;
import com.webflux.apirest.models.repositories.CategoryRepository;
import com.webflux.apirest.models.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public Flux<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public Mono<Product> findById(String id) {
        return productRepository.findById(id);
    }

    @Override
    public Mono<Product> save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Mono<Void> delete(Product product) {
        return productRepository.delete(product);
    }

    @Override
    public Flux<Product> findAllWithUpperCaseName() {
        return productRepository.findAll()
                .map(product -> {
                    product.setName(product.getName().toUpperCase());
                    return product;
                });
    }

    @Override
    public Flux<Product> findAllWithUpperCaseNameAndRepeat(Integer repeat) {
        return productRepository.findAll()
                .map(product -> {
                    product.setName(product.getName().toUpperCase());
                    return product;
                }).repeat(5000);
    }

    @Override
    public Flux<Category> findAllCategory() {
        return categoryRepository.findAll();
    }

    @Override
    public Mono<Category> findCategoryById(String id) {
        return categoryRepository.findById(id);
    }

    @Override
    public Mono<Category> saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    public Mono<Product> findByName(String name) {
//        return productRepository.getByName(name);
        return productRepository.findByName(name);
    }

    @Override
    public Mono<Category> findByCategoryName(String name) {
        return categoryRepository.findByName(name);
    }
}
