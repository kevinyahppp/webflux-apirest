package com.webflux.apirest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webflux.apirest.models.documents.Category;
import com.webflux.apirest.models.documents.Product;
import com.webflux.apirest.models.services.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

// @AutoConfigureWebTestClient is required for SpringBootTest.WebEnvironment.MOCK for simulated test
// instead of SpringBootTest.WebEnvironment.RANDOM_PORT for real test
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class ApirestApplicationTests {

	@Autowired
	private WebTestClient webTestClient;
	@Autowired
	private ProductService productService;
	@Value("${config.base.endpoint}")
	private String endpoint;

	@Test
	void listTest() {
		webTestClient.get()
				.uri(endpoint)
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBodyList(Product.class)
				.consumeWith(response -> {
					List<Product> products = response.getResponseBody();
					products.forEach(product -> log.info(product.getName()));
					Assertions.assertTrue(products.size() > 0);
				});
				//.hasSize(9);
	}

	@Test
	void viewTest() {
		Product product = productService.findByName("TV").block();

		webTestClient.get()
				.uri(endpoint.concat("/{id}"), Collections.singletonMap("id",
						product.getId()))
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody(Product.class)
				.consumeWith(response -> {
					Product product1 = response.getResponseBody();
					Assertions.assertEquals(product1.getName(), "TV");
					Assertions.assertTrue(product1.getId().length() > 0);
				});
//				.expectBody()
//				.jsonPath("$.id").isNotEmpty()
//				.jsonPath("$.name").isEqualTo("TV");
	}

	@Test
	void createTest() {
		Category category = productService.findByCategoryName("Electronic").block();
		Product product = new Product("TV", 250.0, category);

		webTestClient.post()
				.uri(endpoint)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(product), Product.class)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.id").isNotEmpty()
				.jsonPath("$.name").isEqualTo("TV")
				.jsonPath("$.category.name").isEqualTo("Electronic");
	}

	@Test
	void createForNormalVersionTest() {
		Category category = productService.findByCategoryName("Electronic").block();
		Product product = new Product("TV", 250.0, category);

		webTestClient.post()
				.uri(endpoint.replace("/v2", ""))
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(product), Product.class)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.product.id").isNotEmpty()
				.jsonPath("$.product.name").isEqualTo("TV")
				.jsonPath("$.product.category.name").isEqualTo("Electronic");
	}

	@Test
	void create2Test() {
		Category category = productService.findByCategoryName("Electronic").block();
		Product product = new Product("TV", 250.0, category);

		webTestClient.post()
				.uri(endpoint)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(product), Product.class)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody(Product.class)
				.consumeWith(response -> {
					Product product1 = response.getResponseBody();
					Assertions.assertEquals(product1.getName(), "TV");
					Assertions.assertEquals(product1.getCategory().getName(), "Electronic");
					Assertions.assertTrue(product1.getId().length() > 0);
				});
	}

	@Test
	void create2ForNormalVersionTest() {
		Category category = productService.findByCategoryName("Electronic").block();
		Product product = new Product("TV", 250.0, category);

		webTestClient.post()
				.uri(endpoint.replace("/v2", ""))
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(product), Product.class)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody(new ParameterizedTypeReference<LinkedHashMap<String, Object>>() {
				})
				.consumeWith(response -> {
					Object object = response.getResponseBody().get("product");
					Product product1 = new ObjectMapper().convertValue(object, Product.class);
					Assertions.assertEquals(product1.getName(), "TV");
					Assertions.assertEquals(product1.getCategory().getName(), "Electronic");
					Assertions.assertTrue(product1.getId().length() > 0);
				});
	}

	@Test
	void editTest() {
		Product product = productService.findByName("TV").block();
		Category category = productService.findByCategoryName("Electronic").block();
		Product productEdited = new Product("Headphones", 350.0, category);

		webTestClient.put()
				.uri(endpoint.concat("/{id}"), Collections.singletonMap("id",
						product.getId()))
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(productEdited), Product.class)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.id").isNotEmpty()
				.jsonPath("$.name").isEqualTo("Headphones")
				.jsonPath("$.category.name").isEqualTo("Electronic");
	}

	@Test
	void deleteTest() {
		Product product = productService.findByName("Mouse").block();
		webTestClient.delete()
				.uri(endpoint.concat("/{id}"), Collections.singletonMap("id",
						product.getId()))
				.exchange()
				.expectStatus().isNoContent()
				.expectBody().isEmpty();

		webTestClient.delete()
				.uri(endpoint.concat("/{id}"), Collections.singletonMap("id",
						product.getId()))
				.exchange()
				.expectStatus().isNotFound()
				.expectBody().isEmpty();
	}
}
