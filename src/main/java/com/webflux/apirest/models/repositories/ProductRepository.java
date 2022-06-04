package com.webflux.apirest.models.repositories;

import com.webflux.apirest.models.documents.Product;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface ProductRepository extends ReactiveMongoRepository<Product, String> {
    Mono<Product> findByName(String name);

    @Query("{ 'name': ?0 }")
    Mono<Product> getByName(String name);
}
