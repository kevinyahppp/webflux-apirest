package com.webflux.apirest.handler;

import com.webflux.apirest.models.documents.Category;
import com.webflux.apirest.models.documents.Product;
import com.webflux.apirest.models.services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

@Component
public class ProductHandler {
    @Autowired
    private ProductService productService;
    @Value("${config.uploads.path}")
    private String filepath;
    @Autowired
    private Validator validator;
    public Mono<ServerResponse> list(ServerRequest serverRequest) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(productService.findAll(), Product.class);
    }

    public Mono<ServerResponse> view(ServerRequest serverRequest) {
        return productService.findById(serverRequest.pathVariable("id"))
                .flatMap(product -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(product)))
                        .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> create(ServerRequest serverRequest) {
        Mono<Product> productMono = serverRequest.bodyToMono(Product.class);
        return  productMono.flatMap(product -> {
            Errors errors = new BeanPropertyBindingResult(product, Product.class.getName());
            validator.validate(product, errors);
            if (errors.hasErrors()) {
                return Flux.fromIterable(errors.getFieldErrors())
                        .map(fieldError -> "The field ".concat(fieldError.getField()).concat(" ")
                                .concat(fieldError.getDefaultMessage())).collectList()
                        .flatMap(strings -> ServerResponse.badRequest().body(BodyInserters.fromValue(strings)));
            } else {
                if (product.getCreateAt() == null) {
                    product.setCreateAt(new Date());
                }
                return productService.save(product).
                        flatMap(productDb -> ServerResponse.created(URI.create("/api/v2/products/".concat(productDb.getId())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue(productDb)));
            }
        });
    }

    public Mono<ServerResponse> edit(ServerRequest serverRequest) {
        Mono<Product> product = serverRequest.bodyToMono(Product.class);
        String id = serverRequest.pathVariable("id");
        Mono<Product> productDb = productService.findById(id);

        return productDb.zipWith(product, (db, req) -> {
            db.setName(req.getName());
            db.setPrice(req.getPrice());
            db.setCategory(req.getCategory());
            return db;
        }).flatMap(product1 -> ServerResponse.created(URI.create("/api/v2/products/".concat(product1.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .body(productService.save(product1), Product.class))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> delete(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");
        Mono<Product> productDb = productService.findById(id);

        return productDb.flatMap(product -> productService.delete(
                product).then(ServerResponse.noContent().build()))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> upload(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");

        return serverRequest.multipartData().map(multipart -> multipart.toSingleValueMap().get("filePart"))
                .cast(FilePart.class)
                .flatMap(filePart -> productService.findById(id).flatMap(product -> {
                    product.setPicture(UUID.randomUUID().toString().concat("-").concat(filePart.filename())
                            .replace(" ", "")
                            .replace(":", "")
                            .replace("\\", ""));
                    return filePart.transferTo(new File(filepath.concat(product.getPicture()))).then(
                            productService.save(product));
                })).flatMap(product -> ServerResponse.created(URI.create("/api/v2/products/".concat(product.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(product)))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> createWithPicture(ServerRequest serverRequest) {
        Mono<Product> productMono = serverRequest.multipartData().map(multipart -> {
            FormFieldPart name = (FormFieldPart) multipart.toSingleValueMap().get("name");
            FormFieldPart price = (FormFieldPart) multipart.toSingleValueMap().get("price");
            FormFieldPart categoryId = (FormFieldPart) multipart.toSingleValueMap().get("category.id");
            FormFieldPart categoryName = (FormFieldPart) multipart.toSingleValueMap().get("category.name");
            Category category = new Category(categoryId.value(), categoryName.value());
            return new Product(name.value(), Double.parseDouble(price.value()), category);
        });

        return serverRequest.multipartData().map(multipart -> multipart.toSingleValueMap().get("filePart"))
                .cast(FilePart.class)
                .flatMap(filePart -> productMono.flatMap(product -> {
                    product.setPicture(UUID.randomUUID().toString().concat("-").concat(filePart.filename())
                            .replace(" ", "")
                            .replace(":", "")
                            .replace("\\", ""));
                    product.setCreateAt(new Date());
                    return filePart.transferTo(new File(filepath.concat(product.getPicture()))).then(
                            productService.save(product));
                })).flatMap(product -> ServerResponse.created(URI.create("/api/v2/products/".concat(product.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(product)));
    }
}
