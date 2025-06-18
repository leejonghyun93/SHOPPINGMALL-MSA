package org.kosa.cartservice.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "category-service", url = "${category.service.url:http://localhost:8080}")
public interface CategoryServiceFeignClient {

    @GetMapping("/api/categories/{parentCategoryId}/children-ids")
    List<String> getChildrenCategoryIds(@PathVariable("parentCategoryId") String parentCategoryId);

    @GetMapping("/api/categories/{categoryId}/exists")
    Boolean categoryExists(@PathVariable("categoryId") String categoryId);
}