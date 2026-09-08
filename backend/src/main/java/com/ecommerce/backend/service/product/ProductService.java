package com.ecommerce.backend.service.product;

import com.ecommerce.backend.dto.request.ProductRequest;
import com.ecommerce.backend.dto.response.ProductDetailResponse;
import com.ecommerce.backend.dto.response.ProductResponse;
import com.ecommerce.backend.dto.ProductSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    ProductResponse getById(String id);

    Page<ProductResponse> getAll(Pageable pageable);

    Page<ProductResponse> getByCategory(String categoryId, Pageable pageable);

    Page<ProductResponse> search(ProductSearchCriteria criteria, Pageable pageable);

    ProductResponse update(String id, ProductRequest request);

    void delete(String id);

    ProductResponse createProductWithImages(ProductRequest request, List<MultipartFile> files);

    ProductResponse updateProductWithImages(String id, ProductRequest request, List<MultipartFile> files);

    ProductDetailResponse getProductDetail(String id);

    long getTotalProducts();

    long getOutOfStockProducts();
}
