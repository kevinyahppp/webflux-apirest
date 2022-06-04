package com.webflux.apirest;

import com.webflux.apirest.handler.ProductHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterFunctionConfig {
    @Bean
    public RouterFunction<ServerResponse> routes(ProductHandler productHandler) {
        return route(GET("/api/v2/products").or(GET("/api/v3/products")), productHandler::list)
                .andRoute(GET("/api/v2/products/{id}"), productHandler::view)
                .andRoute(RequestPredicates.POST("/api/v2/products"), productHandler::create)
                .andRoute(RequestPredicates.PUT("/api/v2/products/{id}"), productHandler::edit)
                .andRoute(RequestPredicates.DELETE("/api/v2/products/{id}"), productHandler::delete)
                .andRoute(RequestPredicates.POST("/api/v2/products/upload/{id}"), productHandler::upload)
                .andRoute(RequestPredicates.POST("/api/v2/products/create"), productHandler::createWithPicture);
    }
}
