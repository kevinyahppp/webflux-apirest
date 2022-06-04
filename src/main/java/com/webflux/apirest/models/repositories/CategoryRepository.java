package com.webflux.apirest.models.repositories;

import com.webflux.apirest.models.documents.Category;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface CategoryRepository extends ReactiveMongoRepository<Category, String> {
    Mono<Category> findByName(String name);
}
