package com.webflux.apirest.controllers;

import com.webflux.apirest.models.documents.Product;
import com.webflux.apirest.models.services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    @Autowired
    private ProductService productService;
    @Value("${config.uploads.path}")
    private String filepath;

    @GetMapping
    public Mono<ResponseEntity<Flux<Product>>> list() {
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(productService.findAll())
        );
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Product>> view(@PathVariable String id) {
        return productService.findById(id).map(product -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(product)
        ).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> create(@Valid @RequestBody Mono<Product> monoProduct) {
        Map<String, Object> stringObjectMap = new HashMap<>();
        return monoProduct.flatMap(product -> {
            if (product.getCreateAt() == null) {
                product.setCreateAt(new Date());
            }
            return productService.save(product).map(product1 -> {
                stringObjectMap.put("product", product1);
                stringObjectMap.put("message", "Product successfully created");
                stringObjectMap.put("timestamp", new Date());
                return ResponseEntity.created(
                        URI.create("/api/products/".concat(product1.getId()))
                ).contentType(MediaType.APPLICATION_JSON).body(stringObjectMap);
            });
        }).onErrorResume(throwable -> {
            return Mono.just(throwable).cast(WebExchangeBindException.class)
                    .flatMap(e -> Mono.just(e.getFieldErrors()))
                    .flatMapMany(Flux::fromIterable)
                    .map(fieldError -> "The flied ".concat(fieldError.getField()).concat(" ").concat(fieldError.getDefaultMessage()))
                    .collectList()
                    .flatMap(strings -> {
                        stringObjectMap.put("errors", strings);
                        stringObjectMap.put("timestamp", new Date());
                        stringObjectMap.put("status", HttpStatus.BAD_REQUEST.value());
                        return Mono.just(ResponseEntity.badRequest().body(stringObjectMap));
                    });
        });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Product>> edit(@RequestBody Product product, @PathVariable String id) {
        Mono<Product> productMono = productService.findById(id);
        return productMono.flatMap(product1 -> {
            product1.setName(product.getName());
            product1.setPrice(product.getPrice());
            product1.setCategory(product.getCategory());
           return productService.save(product1);
        }).map(product1 -> ResponseEntity.created(URI.create("/api/products/".concat(product1.getId())))
                .contentType(MediaType.APPLICATION_JSON).body(product1))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return productService.findById(id).flatMap(product -> productService.delete(product)
                        .then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT))))
                .defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/upload/{id}")
    public Mono<ResponseEntity<Product>> upload(@PathVariable String id,
                                                @RequestPart FilePart filePart) {
        return productService.findById(id).flatMap(product -> {
            product.setPicture(UUID.randomUUID().toString().concat("-")
                    .concat(filePart.filename())
                    .replace(" ", "")
                    .replace(":", "")
                    .replace("\\", ""));
            return filePart.transferTo(new File(filepath.concat(product.getPicture())))
                    .then(productService.save(product));
        }).map(product -> ResponseEntity.ok(product)).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/v2")
    public Mono<ResponseEntity<Product>> createWithPicture(Product product, @RequestPart FilePart filePart) {
        if (product.getCreateAt() == null) {
            product.setCreateAt(new Date());
        }
        product.setPicture(UUID.randomUUID().toString().concat("-")
                .concat(filePart.filename())
                .replace(" ", "")
                .replace(":", "")
                .replace("\\", ""));
        return filePart.transferTo(new File(filepath.concat(product.getPicture())))
                .then(productService.save(product)).map(product1 -> ResponseEntity.created(
                URI.create("/api/products/".concat(product1.getId()))
        ).contentType(MediaType.APPLICATION_JSON).body(product1));
    }
}
