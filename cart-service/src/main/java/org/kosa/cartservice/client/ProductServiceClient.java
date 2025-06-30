package org.kosa.cartservice.client;

import org.kosa.cartservice.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "http://localhost:8080")
public interface ProductServiceClient {

    @GetMapping("/api/products/{productId}")
    ProductDTO getProduct(@PathVariable("productId") Integer productId);
}