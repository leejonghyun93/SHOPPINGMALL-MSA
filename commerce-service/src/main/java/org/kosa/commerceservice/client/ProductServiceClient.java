package org.kosa.commerceservice.client;


import org.kosa.commerceservice.dto.product.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "commerce-service", url = "${commerce-service.url:http://localhost:8090}")
public interface ProductServiceClient {
    @GetMapping("/api/products/{productId}")
    ProductDTO getProduct(@PathVariable("productId") Integer productId);
}
