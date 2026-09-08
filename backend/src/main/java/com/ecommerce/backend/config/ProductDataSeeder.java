package com.ecommerce.backend.config;

import com.ecommerce.backend.entity.Category;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.ProductImage;
import com.ecommerce.backend.repository.CategoryRepository;
import com.ecommerce.backend.repository.ProductImageRepository;
import com.ecommerce.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Order(4)
@RequiredArgsConstructor
@Slf4j
public class ProductDataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    public void run(String... args) {
        log.info("Checking clothing seeder products...");

        Category pants = getOrCreateCategory("Pants");
        Category shirts = getOrCreateCategory("Shirts");
        Category caps = getOrCreateCategory("Caps");
        Category bags = getOrCreateCategory("Bags");
        Category shoes = getOrCreateCategory("Shoes");
        Category socks = getOrCreateCategory("Socks");
        
        int created = 0;
        int updated = 0;
        // Prices in NPR, descriptions tailored for Nepal clothing branch, images from Unsplash clothing
        int r;
        r = seedOrUpdate(pants, "Slim Fit Chino Pants",
                "Premium slim-fit chinos tailored from organic cotton stretch twill. Mid-rise with tapered leg, garment-dyed for a soft vintage hand-feel. Breathable, durable and perfect for office, casual Fridays or weekend outings in Kathmandu. Easy care, colorfast.",
                new BigDecimal("2499.00"), 4.7f, 142, 120, true,
                List.of(
                        "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?q=80&w=800&auto=format&fit=crop",
                        "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?q=80&w=800&auto=format&fit=crop",
                        "https://images.unsplash.com/photo-1507680434567-5739c80be1ac?q=80&w=800&auto=format&fit=crop"
                ));
        if (r == 1) created++; else if (r == 2) updated++;
        r = seedOrUpdate(shirts, "Classic Oxford Shirt",
                "Timeless Oxford shirt in 100% breathable long-staple cotton. Button-down collar, single chest pocket, double-stitched seams and a relaxed yet sharp fit. Ideal for office, Dashain gatherings or layered winter wear. Pre-shrunk and wrinkle-resistant.",
                new BigDecimal("1899.00"), 4.6f, 98, 85, false,
                List.of(
                        "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?q=80&w=800&auto=format&fit=crop",
                        "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?q=80&w=800&auto=format&fit=crop",
                        "https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?q=80&w=800&auto=format&fit=crop"
                ));
        if (r == 1) created++; else if (r == 2) updated++;
        r = seedOrUpdate(caps, "Washed Canvas Baseball Cap",
                "Low-profile 6-panel baseball cap in washed canvas with brass buckle closure and curved brim. Embroidered minimal logo, breathable eyelets and adjustable fit. Perfect for sunny valley days, travel or street style.",
                new BigDecimal("799.00"), 4.5f, 76, 200, false,
                List.of(
                        "https://images.unsplash.com/photo-1521369909029-2afed882baee?q=80&w=800&auto=format&fit=crop",
                        "https://images.unsplash.com/photo-1532241903571-a6d89d9422f9?q=80&w=800&auto=format&fit=crop",
                        "https://images.unsplash.com/photo-1588850561407-ed78c282e89b?q=80&w=800&auto=format&fit=crop"
                ));
        if (r == 1) created++; else if (r == 2) updated++;
        r = seedOrUpdate(bags, "Minimalist Leather Tote",
                "Handcrafted full-grain leather tote with spacious interior, fits 15\" laptop, hidden phone pocket and brass hardware. Minimal design meets maximum utility – ideal for office, college or daily bazaar carry. Ages beautifully.",
                new BigDecimal("3499.00"), 4.6f, 87, 30, false,
                List.of(
                        "https://images.unsplash.com/photo-1590874103328-eac38a683ce7?q=80&w=800&auto=format&fit=crop",
                        "https://images.unsplash.com/photo-1548036328-c9fa89d128fa?q=80&w=800&auto=format&fit=crop",
                        "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?q=80&w=800&auto=format&fit=crop"
                ));
        if (r == 1) created++; else if (r == 2) updated++;
        r = seedOrUpdate(shoes, "Low Top Leather Sneakers",
                "Minimal low-top sneakers in premium smooth leather with cushioned footbed, breathable lining and vulcanized rubber sole. Clean lines, fine stitching and all-day comfort for urban walks, office or casual wear.",
                new BigDecimal("5999.00"), 4.8f, 203, 60, true,
                List.of(
                        "https://images.unsplash.com/photo-1542291026-7eec264c27ff?q=80&w=800&auto=format&fit=crop",
                        "https://images.unsplash.com/photo-1600185365483-26d7a4cc7519?q=80&w=800&auto=format&fit=crop",
                        "https://images.unsplash.com/photo-1608231387042-66d1773070a5?q=80&w=800&auto=format&fit=crop"
                ));
        if (r == 1) created++; else if (r == 2) updated++;
        r = seedOrUpdate(socks, "Organic Cotton Crew Socks (3-Pack)",
                "Ultra-soft crew socks 3-pack in GOTS-certified organic cotton with reinforced heel & toe. Breathable, moisture-wicking, ribbed cuff stays up without squeeze. Everyday comfort for office, trek or home.",
                new BigDecimal("899.00"), 4.7f, 54, 300, false,
                List.of(
                        "https://images.unsplash.com/photo-1582966772680-860e372bb558?q=80&w=800&auto=format&fit=crop",
                        "https://images.unsplash.com/photo-1614253429387-76d1d4f73a61?q=80&w=800&auto=format&fit=crop",
                        "https://images.unsplash.com/photo-1610366010292-f6dbfcc44171?q=80&w=800&auto=format&fit=crop"
                ));
        if (r == 1) created++; else if (r == 2) updated++;

        int total = (int) productRepository.count();
        if (created == 0 && updated == 0) {
            log.info("All clothing seeder products already present with correct images/pricing ({} existing total), skipping.", total);
        } else {
            log.info("Clothing seeder done – {} new, {} updated, total now {}.", created, updated, total);
        }
    }

    private int seedOrUpdate(Category category, String name, String description, BigDecimal price, Float rating, Integer reviewCount, Integer stock, Boolean isNewArrival, List<String> imageUrls) {
        var existingOpt = productRepository.findByName(name);
        if (existingOpt.isPresent()) {
            Product existing = existingOpt.get();
            List<ProductImage> existingImages = productImageRepository.findByProductId(existing.getId());
            boolean hasIrrelevantImages = existingImages.stream().anyMatch(img -> img.getUrl() != null && img.getUrl().contains("picsum.photos"));
            boolean priceMismatch = existing.getPrice() == null || existing.getPrice().compareTo(price) != 0;
            boolean descMismatch = existing.getDescription() == null || !existing.getDescription().equals(description);
            // If images are irrelevant (picsum) or price/description outdated, fix it
            if (hasIrrelevantImages || priceMismatch || descMismatch) {
                log.info("Updating product '{}' – fixing images/pricing/description (picsum={}, priceMismatch={}, descMismatch={})", name, hasIrrelevantImages, priceMismatch, descMismatch);
                existing.setDescription(description);
                existing.setPrice(price);
                existing.setRating(rating);
                existing.setReviewCount(reviewCount);
                existing.setStockQuantity(stock);
                existing.setIsNewArrival(isNewArrival);
                existing.setCategory(category);
                productRepository.save(existing);
                // Replace images
                if (!existingImages.isEmpty()) {
                    productImageRepository.deleteAll(existingImages);
                    productImageRepository.flush();
                }
                List<ProductImage> newImages = new java.util.ArrayList<>();
                for (int i = 0; i < imageUrls.size(); i++) {
                    newImages.add(ProductImage.builder()
                            .product(existing)
                            .url(imageUrls.get(i))
                            .altText(name + (i == 0 ? " Main" : " Angle " + i))
                            .isPrimary(i == 0)
                            .displayOrder(i)
                            .build());
                }
                productImageRepository.saveAll(newImages);
                return 2; // updated
            }
            log.debug("Product '{}' already exists with correct data, skipping", name);
            return 0;
        }
        // Create new product with proper clothing images
        seedProductWithImages(category, name, description, price, rating, reviewCount, stock, isNewArrival, imageUrls);
        return 1;
    }

    private Category getOrCreateCategory(String name) {
        return categoryRepository.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name) && c.getParent() == null)
                .findFirst()
                .orElseGet(() -> {
                    Category saved = categoryRepository.save(Category.builder().name(name).build());
                    log.info("Created clothing category '{}' id={}", saved.getName(), saved.getId());
                    return saved;
                });
    }

    private void seedProductWithImages(Category category, String name, String description, BigDecimal price, Float rating, Integer reviewCount, Integer stock, Boolean isNewArrival, List<String> imageUrls) {
        Product product = Product.builder()
                .category(category)
                .name(name)
                .description(description)
                .price(price)
                .rating(rating)
                .reviewCount(reviewCount)
                .stockQuantity(stock)
                .isNewArrival(isNewArrival)
                .build();
        product = productRepository.save(product);

        List<ProductImage> images = new java.util.ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(ProductImage.builder()
                    .product(product)
                    .url(imageUrls.get(i))
                    .altText(name + (i == 0 ? " Main" : " Angle " + i))
                    .isPrimary(i == 0)
                    .displayOrder(i)
                    .build());
        }
        productImageRepository.saveAll(images);
    }
}
