package org.kosa.commerceservice.service.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.dto.product.ProductDTO;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedProductService {

    private final ProductService productService;

    public ProductDTO getProductDetail(Integer productId) {
        ProductDTO product = productService.getProductById(productId);
        if (product == null) {
            return null;
        }
        productService.attachImagesToProduct(product);
        return product;
    }

    public List<ProductDTO> getProductList(int limit) {
        List<ProductDTO> products = productService.getAllProducts(limit);
        productService.attachMainImagesToProducts(products);
        return products;
    }

    public List<ProductDTO> getProductsByCategory(Integer categoryId, int limit) {
        List<ProductDTO> products = productService.getProductsByCategory(categoryId, limit);
        productService.attachMainImagesToProducts(products);
        return products;
    }

    public List<ProductDTO> getRelatedProducts(Integer productId, int limit) {
        List<ProductDTO> products = productService.getRelatedProducts(productId, limit);
        productService.attachMainImagesToProducts(products);
        return products;
    }

    public List<ProductDTO> getProductsByHost(Long hostId, int limit) {
        List<ProductDTO> products = productService.getProductsByHost(hostId, limit);
        productService.attachMainImagesToProducts(products);
        return products;
    }

    public List<ProductDTO> getProductsInStock(int limit) {
        List<ProductDTO> products = productService.getProductsInStock(limit);
        productService.attachMainImagesToProducts(products);
        return products;
    }

    public List<ProductDTO> getDiscountedProducts(int limit) {
        List<ProductDTO> products = productService.getDiscountedProducts(limit);
        productService.attachMainImagesToProducts(products);
        return products;
    }

    public List<ProductDTO> getPopularProducts(int limit) {
        List<ProductDTO> products = productService.getPopularProducts(limit);
        productService.attachMainImagesToProducts(products);
        return products;
    }

    public void attachMainImagesToProducts(List<ProductDTO> products) {
        productService.attachMainImagesToProducts(products);
    }

    public void attachAllImagesToProduct(ProductDTO product) {
        productService.attachImagesToProduct(product);
    }
}