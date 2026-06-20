package com.suprsyncr.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprsyncr.auth.service.AuthService;
import com.suprsyncr.common.dto.PageResponse;
import com.suprsyncr.common.exception.ResourceNotFoundException;
import com.suprsyncr.common.exception.ValidationException;
import com.suprsyncr.product.dto.*;
import com.suprsyncr.product.entity.Product;
import com.suprsyncr.product.entity.ProductCategory;
import com.suprsyncr.product.entity.ProductStatus;
import com.suprsyncr.product.entity.ProductVariant;
import com.suprsyncr.product.repository.ProductCategoryRepository;
import com.suprsyncr.product.repository.ProductRepository;
import com.suprsyncr.product.repository.ProductVariantRepository;
import com.suprsyncr.seller.entity.Seller;
import com.suprsyncr.seller.repository.SellerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of ProductService.
 */
@Service
@Transactional
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final SellerRepository sellerRepository;
    private final AuthService authService;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;
    
    public ProductServiceImpl(
        ProductRepository productRepository,
        ProductCategoryRepository categoryRepository,
        ProductVariantRepository variantRepository,
        SellerRepository sellerRepository,
        AuthService authService,
        S3Service s3Service,
        ObjectMapper objectMapper
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.variantRepository = variantRepository;
        this.sellerRepository = sellerRepository;
        this.authService = authService;
        this.s3Service = s3Service;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public ProductDto createProduct(CreateProductRequest request) {
        // Get current seller
        Seller seller = getCurrentSeller();
        
        // Validate SKU uniqueness within seller
        if (productRepository.findBySellerIdAndSku(seller.getId(), request.sku()).isPresent()) {
            throw new ValidationException("Product with SKU " + request.sku() + " already exists");
        }
        
        // Validate variant SKUs if present
        if (request.variants() != null && !request.variants().isEmpty()) {
            List<String> variantSkus = request.variants().stream()
                .map(CreateVariantRequest::sku)
                .toList();
            
            // Check for duplicate SKUs within the request
            if (variantSkus.size() != variantSkus.stream().distinct().count()) {
                throw new ValidationException("Duplicate variant SKUs found in request");
            }
        }
        
        // Create product
        Product product = new Product();
        product.setSeller(seller);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setSku(request.sku());
        product.setBasePrice(request.basePrice());
        product.setStatus(ProductStatus.DRAFT);
        product.setBrand(request.brand());
        product.setWeight(request.weight());
        product.setLength(request.length());
        product.setWidth(request.width());
        product.setHeight(request.height());
        
        // Set category if provided
        if (request.categoryId() != null) {
            ProductCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        }
        
        // Create variants if provided
        if (request.variants() != null && !request.variants().isEmpty()) {
            List<ProductVariant> variants = new ArrayList<>();
            for (CreateVariantRequest variantRequest : request.variants()) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setSku(variantRequest.sku());
                variant.setVariantName(variantRequest.variantName());
                variant.setPrice(variantRequest.price());
                variant.setImageUrl(variantRequest.imageUrl());
                
                // Convert attributes map to JSON string
                if (variantRequest.attributes() != null) {
                    try {
                        variant.setAttributes(objectMapper.writeValueAsString(variantRequest.attributes()));
                    } catch (JsonProcessingException e) {
                        throw new ValidationException("Invalid variant attributes format");
                    }
                }
                
                variants.add(variant);
            }
            product.setVariants(variants);
        }
        
        // Save product
        product = productRepository.save(product);
        
        return toDto(product);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductDto getProduct(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        // Verify ownership
        Seller seller = getCurrentSeller();
        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new ResourceNotFoundException("Product not found");
        }
        
        return toDto(product);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductDto> getProducts(int page, int size, String search, Long categoryId, ProductStatus status) {
        // Limit page size to 100
        if (size > 100) {
            size = 100;
        }
        
        Seller seller = getCurrentSeller();
        Pageable pageable = PageRequest.of(page, size);

        // Normalize blank search to null to avoid lower(bytea) PostgreSQL type error
        String normalizedSearch = (search == null || search.isBlank()) ? null : search;

        // Page<Product> productPage = productRepository.findProducts(
        //     seller.getId(),
        //     search,
        //     categoryId,
        //     status,
        //     pageable
        // );

        Page<Product> productPage = productRepository.findProducts(
            seller.getId(),
            normalizedSearch,
            categoryId,
            status != null ? status.name() : null,  // native query expects String, not enum
            pageable
        );
        
        List<ProductDto> content = productPage.getContent().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        
        return new PageResponse<>(
            content,
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalElements(),
            productPage.getTotalPages()
        );
    }
    
    @Override
    public ProductDto updateProduct(Long productId, CreateProductRequest request) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        // Verify ownership
        Seller seller = getCurrentSeller();
        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new ResourceNotFoundException("Product not found");
        }
        
        // Validate SKU uniqueness if changed
        if (!product.getSku().equals(request.sku())) {
            if (productRepository.findBySellerIdAndSku(seller.getId(), request.sku()).isPresent()) {
                throw new ValidationException("Product with SKU " + request.sku() + " already exists");
            }
        }
        
        // Update product fields
        product.setName(request.name());
        product.setDescription(request.description());
        product.setSku(request.sku());
        product.setBasePrice(request.basePrice());
        product.setBrand(request.brand());
        product.setWeight(request.weight());
        product.setLength(request.length());
        product.setWidth(request.width());
        product.setHeight(request.height());
        
        // Update category
        if (request.categoryId() != null) {
            ProductCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }
        
        // Update variants
        if (request.variants() != null) {
            // Remove existing variants
            product.getVariants().clear();
            
            // Add new variants
            for (CreateVariantRequest variantRequest : request.variants()) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setSku(variantRequest.sku());
                variant.setVariantName(variantRequest.variantName());
                variant.setPrice(variantRequest.price());
                variant.setImageUrl(variantRequest.imageUrl());
                
                if (variantRequest.attributes() != null) {
                    try {
                        variant.setAttributes(objectMapper.writeValueAsString(variantRequest.attributes()));
                    } catch (JsonProcessingException e) {
                        throw new ValidationException("Invalid variant attributes format");
                    }
                }
                
                product.getVariants().add(variant);
            }
        }
        
        product = productRepository.save(product);
        
        return toDto(product);
    }
    
    @Override
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        // Verify ownership
        Seller seller = getCurrentSeller();
        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new ResourceNotFoundException("Product not found");
        }
        
        // TODO: Verify no active listings exist (will be implemented in listing module)
        
        productRepository.delete(product);
    }
    
    @Override
    public ProductDto updateStatus(Long productId, ProductStatus status) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        // Verify ownership
        Seller seller = getCurrentSeller();
        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new ResourceNotFoundException("Product not found");
        }
        
        product.setStatus(status);
        product = productRepository.save(product);
        
        return toDto(product);
    }
    
    @Override
    public ImageUploadResponse generateUploadUrl(ImageUploadRequest request) {
        // Validate content type is an image
        if (!request.contentType().startsWith("image/")) {
            throw new ValidationException("Content type must be an image format");
        }
        
        S3Service.PresignedUrlResult result = s3Service.generatePresignedUploadUrl(
            request.fileName(),
            request.contentType()
        );
        
        return new ImageUploadResponse(
            result.uploadUrl(),
            result.publicUrl(),
            result.key()
        );
    }
    
    @Override
    public void confirmImageUpload(Long productId, String imageKey) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        // Verify ownership
        Seller seller = getCurrentSeller();
        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new ResourceNotFoundException("Product not found");
        }
        
        // Get public URL for the image
        String imageUrl = s3Service.getPublicUrl(imageKey);
        
        // Add image URL to product
        product.getImageUrls().add(imageUrl);
        productRepository.save(product);
    }
    
    private Seller getCurrentSeller() {
        Long userId = authService.getCurrentUser().getId();
        return sellerRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
    }
    
    private ProductDto toDto(Product product) {
        CategoryDto categoryDto = null;
        if (product.getCategory() != null) {
            ProductCategory category = product.getCategory();
            categoryDto = new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getParent() != null ? category.getParent().getId() : null
            );
        }
        
        List<VariantDto> variantDtos = product.getVariants().stream()
            .map(this::toVariantDto)
            .collect(Collectors.toList());
        
        return new ProductDto(
            product.getId(),
            product.getName(),
            product.getDescription(),
            categoryDto,
            product.getSku(),
            product.getBasePrice(),
            product.getStatus(),
            new ArrayList<>(product.getImageUrls()),
            product.getBrand(),
            product.getWeight(),
            product.getLength(),
            product.getWidth(),
            product.getHeight(),
            variantDtos,
            product.getCreatedAt()
        );
    }
    
    private VariantDto toVariantDto(ProductVariant variant) {
        Map<String, String> attributes = null;
        if (variant.getAttributes() != null) {
            try {
                attributes = objectMapper.readValue(variant.getAttributes(), Map.class);
            } catch (JsonProcessingException e) {
                attributes = new HashMap<>();
            }
        }
        
        return new VariantDto(
            variant.getId(),
            variant.getSku(),
            variant.getVariantName(),
            attributes,
            variant.getPrice(),
            variant.getImageUrl()
        );
    }
}

