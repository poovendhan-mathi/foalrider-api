package com.foalrider.config;

import com.foalrider.modules.product.entity.*;
import com.foalrider.modules.product.repository.*;
import com.foalrider.modules.user.entity.Role;
import com.foalrider.modules.user.entity.User;
import com.foalrider.modules.user.repository.RoleRepository;
import com.foalrider.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test"})
@Order(3)
public class ProductDataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("Products already exist, skipping seeding...");
            return;
        }

        log.info("Starting Product Data Seeding...");
        
        createTestAccounts();
        Map<String, Category> categories = createCategories();
        Map<String, Brand> brands = createBrands();
        createProducts(categories, brands);
        
        log.info("Product Data Seeding Complete!");
        log.info("Categories: {}", categoryRepository.count());
        log.info("Brands: {}", brandRepository.count());
        log.info("Products: {}", productRepository.count());
        log.info("Test Accounts: admin@foalrider.com, customer@foalrider.com, vendor@foalrider.com (password: Test@123)");
    }

    private void createTestAccounts() {
        log.info("Creating test accounts...");
        String password = "Test@123";

        if (userRepository.findByEmail("admin@foalrider.com").isEmpty()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
            userRepository.save(User.builder()
                .email("admin@foalrider.com")
                .passwordHash(passwordEncoder.encode(password))
                .firstName("Admin").lastName("User").phone("+1234567890")
                .role(adminRole).isEmailVerified(true).isActive(true)
                .regionCode("US").locale("en-US").build());
            log.info("Created admin account");
        }

        if (userRepository.findByEmail("customer@foalrider.com").isEmpty()) {
            Role customerRole = roleRepository.findByName("ROLE_CUSTOMER").orElseThrow();
            userRepository.save(User.builder()
                .email("customer@foalrider.com")
                .passwordHash(passwordEncoder.encode(password))
                .firstName("John").lastName("Customer").phone("+1987654321")
                .role(customerRole).isEmailVerified(true).isActive(true)
                .regionCode("US").locale("en-US").build());
            log.info("Created customer account");
        }

        if (userRepository.findByEmail("vendor@foalrider.com").isEmpty()) {
            Role vendorRole = roleRepository.findByName("ROLE_VENDOR").orElseThrow();
            userRepository.save(User.builder()
                .email("vendor@foalrider.com")
                .passwordHash(passwordEncoder.encode(password))
                .firstName("Fashion").lastName("Vendor").phone("+1555666777")
                .role(vendorRole).isEmailVerified(true).isActive(true)
                .regionCode("US").locale("en-US").build());
            log.info("Created vendor account");
        }
    }

    private Map<String, Category> createCategories() {
        log.info("Creating categories...");
        Map<String, Category> categories = new HashMap<>();

        Category mens = saveCategory("Men's Fashion", "mens-fashion", "Men's clothing", 
            "https://images.unsplash.com/photo-1490578474895-699cd4e2cf59?w=800", null, true);
        Category womens = saveCategory("Women's Fashion", "womens-fashion", "Women's clothing",
            "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=800", null, true);
        Category kids = saveCategory("Kids' Fashion", "kids-fashion", "Children's clothing",
            "https://images.unsplash.com/photo-1503919545889-aef636e10ad4?w=800", null, true);

        categories.put("mens", mens);
        categories.put("womens", womens);
        categories.put("kids", kids);

        categories.put("mens-shirts", saveCategory("Shirts", "mens-shirts", "Men's shirts",
            "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=800", mens, false));
        categories.put("mens-pants", saveCategory("Pants", "mens-pants", "Men's pants",
            "https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=800", mens, false));
        categories.put("mens-jackets", saveCategory("Jackets", "mens-jackets", "Men's jackets",
            "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800", mens, false));

        categories.put("womens-dresses", saveCategory("Dresses", "womens-dresses", "Women's dresses",
            "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=800", womens, false));
        categories.put("womens-tops", saveCategory("Tops", "womens-tops", "Women's tops",
            "https://images.unsplash.com/photo-1564257631407-4deb1f99d992?w=800", womens, false));

        categories.put("kids-boys", saveCategory("Boys", "kids-boys", "Boys' clothing",
            "https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?w=800", kids, false));
        categories.put("kids-girls", saveCategory("Girls", "kids-girls", "Girls' clothing",
            "https://images.unsplash.com/photo-1518831959646-742c3a14ebf7?w=800", kids, false));

        return categories;
    }

    private Category saveCategory(String name, String slug, String desc, String img, Category parent, boolean featured) {
        if (categoryRepository.existsBySlug(slug)) {
            return categoryRepository.findBySlug(slug).orElse(null);
        }
        return categoryRepository.save(Category.builder()
            .name(name).slug(slug).description(desc).imageUrl(img)
            .parent(parent).isActive(true).isFeatured(featured).displayOrder(0).build());
    }

    private Map<String, Brand> createBrands() {
        log.info("Creating brands...");
        Map<String, Brand> brands = new HashMap<>();
        brands.put("foalrider", saveBrand("FoalRider", "foalrider", "Premium fashion brand",
            "https://ui-avatars.com/api/?name=FR&background=000&color=fff&size=200", true));
        brands.put("urbanstyle", saveBrand("UrbanStyle", "urbanstyle", "Modern urban fashion",
            "https://ui-avatars.com/api/?name=US&background=2563eb&color=fff&size=200", true));
        brands.put("classicwear", saveBrand("ClassicWear", "classicwear", "Timeless classic fashion",
            "https://ui-avatars.com/api/?name=CW&background=dc2626&color=fff&size=200", false));
        return brands;
    }

    private Brand saveBrand(String name, String slug, String desc, String logo, boolean featured) {
        if (brandRepository.existsBySlug(slug)) {
            return brandRepository.findBySlug(slug).orElse(null);
        }
        return brandRepository.save(Brand.builder()
            .name(name).slug(slug).description(desc).logoUrl(logo)
            .isActive(true).isFeatured(featured).build());
    }

    private void createProducts(Map<String, Category> categories, Map<String, Brand> brands) {
        log.info("Creating 10 products...");
        Random rand = new Random();

        saveProduct("Classic White Oxford Shirt", "MENS-SHIRT-001",
            "A timeless white Oxford shirt perfect for any occasion",
            "Premium cotton Oxford shirt with button-down collar.",
            new BigDecimal("79.99"), new BigDecimal("59.99"),
            categories.get("mens-shirts"), brands.get("classicwear"),
            Arrays.asList("formal", "casual"), true, true,
            Arrays.asList(
                "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=800",
                "https://images.unsplash.com/photo-1598033129183-c4f50c736f10?w=800",
                "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=800"
            ), Arrays.asList("S", "M", "L", "XL"), rand);

        saveProduct("Slim Fit Chino Pants", "MENS-PANTS-001",
            "Comfortable slim-fit chinos for everyday wear",
            "Versatile chinos with stretch fabric.",
            new BigDecimal("89.99"), null,
            categories.get("mens-pants"), brands.get("urbanstyle"),
            Arrays.asList("casual", "office"), true, false,
            Arrays.asList(
                "https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=800",
                "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=800",
                "https://images.unsplash.com/photo-1542272604-787c3835535d?w=800"
            ), Arrays.asList("30", "32", "34", "36"), rand);

        saveProduct("Premium Leather Bomber Jacket", "MENS-JACKET-001",
            "Stylish leather bomber jacket with classic design",
            "Genuine leather bomber with ribbed cuffs.",
            new BigDecimal("299.99"), new BigDecimal("249.99"),
            categories.get("mens-jackets"), brands.get("foalrider"),
            Arrays.asList("premium", "leather"), true, true,
            Arrays.asList(
                "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800",
                "https://images.unsplash.com/photo-1520975954732-35dd22299614?w=800",
                "https://images.unsplash.com/photo-1559551409-dadc959f76b8?w=800"
            ), Arrays.asList("S", "M", "L", "XL"), rand);

        saveProduct("Floral Print Summer Dress", "WOMENS-DRESS-001",
            "Beautiful floral dress perfect for summer days",
            "Lightweight summer dress with V-neckline.",
            new BigDecimal("129.99"), new BigDecimal("89.99"),
            categories.get("womens-dresses"), brands.get("foalrider"),
            Arrays.asList("summer", "floral"), true, true,
            Arrays.asList(
                "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=800",
                "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=800",
                "https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?w=800"
            ), Arrays.asList("XS", "S", "M", "L"), rand);

        saveProduct("Elegant Silk Blouse", "WOMENS-TOP-001",
            "Luxurious silk blouse for sophisticated style",
            "100% mulberry silk blouse with relaxed fit.",
            new BigDecimal("159.99"), null,
            categories.get("womens-tops"), brands.get("classicwear"),
            Arrays.asList("formal", "elegant"), true, false,
            Arrays.asList(
                "https://images.unsplash.com/photo-1564257631407-4deb1f99d992?w=800",
                "https://images.unsplash.com/photo-1551163943-3f6a855d1153?w=800",
                "https://images.unsplash.com/photo-1485462537746-965f33f7f6a7?w=800"
            ), Arrays.asList("XS", "S", "M", "L"), rand);

        saveProduct("Elegant Evening Gown", "WOMENS-DRESS-002",
            "Stunning evening dress for special occasions",
            "Floor-length evening gown with elegant draping.",
            new BigDecimal("249.99"), new BigDecimal("199.99"),
            categories.get("womens-dresses"), brands.get("urbanstyle"),
            Arrays.asList("formal", "evening"), false, true,
            Arrays.asList(
                "https://images.unsplash.com/photo-1566174053879-31528523f8ae?w=800",
                "https://images.unsplash.com/photo-1518611012118-696072aa579a?w=800",
                "https://images.unsplash.com/photo-1562137369-1a1a0bc66744?w=800"
            ), Arrays.asList("XS", "S", "M", "L"), rand);

        saveProduct("Boys Cool Graphic Tee", "KIDS-BOYS-001",
            "Fun graphic t-shirt for active boys",
            "Comfortable cotton t-shirt with cool graphic.",
            new BigDecimal("29.99"), new BigDecimal("24.99"),
            categories.get("kids-boys"), brands.get("foalrider"),
            Arrays.asList("casual", "graphic"), false, true,
            Arrays.asList(
                "https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?w=800",
                "https://images.unsplash.com/photo-1503919889273-c4e74afd0df8?w=800",
                "https://images.unsplash.com/photo-1471286174890-9c112ffca5b4?w=800"
            ), Arrays.asList("4-5Y", "6-7Y", "8-9Y", "10-11Y"), rand);

        saveProduct("Boys Classic Denim Jeans", "KIDS-BOYS-002",
            "Durable denim jeans for everyday wear",
            "Classic fit denim with adjustable waistband.",
            new BigDecimal("49.99"), null,
            categories.get("kids-boys"), brands.get("urbanstyle"),
            Arrays.asList("denim", "casual"), false, false,
            Arrays.asList(
                "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=800",
                "https://images.unsplash.com/photo-1565084888279-aca607ecce0c?w=800",
                "https://images.unsplash.com/photo-1604176354204-9268737828e4?w=800"
            ), Arrays.asList("4-5Y", "6-7Y", "8-9Y", "10-11Y"), rand);

        saveProduct("Girls Sparkle Princess Dress", "KIDS-GIRLS-001",
            "Magical princess dress for special occasions",
            "Beautiful dress with sparkle tulle overlay.",
            new BigDecimal("69.99"), new BigDecimal("54.99"),
            categories.get("kids-girls"), brands.get("foalrider"),
            Arrays.asList("party", "princess"), true, true,
            Arrays.asList(
                "https://images.unsplash.com/photo-1518831959646-742c3a14ebf7?w=800",
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=800",
                "https://images.unsplash.com/photo-1494578379344-d6c710782a3d?w=800"
            ), Arrays.asList("4-5Y", "6-7Y", "8-9Y", "10-11Y"), rand);

        saveProduct("Girls Floral Leggings Set", "KIDS-GIRLS-002",
            "Cute matching top and leggings set",
            "Two-piece set with floral print.",
            new BigDecimal("39.99"), new BigDecimal("34.99"),
            categories.get("kids-girls"), brands.get("urbanstyle"),
            Arrays.asList("set", "floral"), false, true,
            Arrays.asList(
                "https://images.unsplash.com/photo-1476234251651-f353703a034d?w=800",
                "https://images.unsplash.com/photo-1519457431-44ccd64a579b?w=800",
                "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=800"
            ), Arrays.asList("4-5Y", "6-7Y", "8-9Y", "10-11Y"), rand);
    }

    private void saveProduct(String name, String sku, String shortDesc, String desc,
                             BigDecimal basePrice, BigDecimal salePrice, Category category,
                             Brand brand, List<String> tags, boolean featured, boolean isNew,
                             List<String> imageUrls, List<String> sizes, Random rand) {
        if (productRepository.existsBySku(sku)) return;

        String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        
        Product product = productRepository.save(Product.builder()
            .name(name).slug(slug).sku(sku)
            .shortDescription(shortDesc).description(desc)
            .basePrice(basePrice).salePrice(salePrice)
            .category(category).brand(brand).tags(tags)
            .isActive(true).isFeatured(featured).isNew(isNew)
            .weight(new BigDecimal("0.5")).weightUnit("kg")
            .metaTitle(name).metaDescription(shortDesc)
            .viewCount(0).soldCount(0).avgRating(BigDecimal.ZERO).reviewCount(0)
            .build());

        for (int i = 0; i < imageUrls.size(); i++) {
            productImageRepository.save(ProductImage.builder()
                .product(product).url(imageUrls.get(i))
                .altText(name + " - Image " + (i + 1))
                .displayOrder(i).isPrimary(i == 0).build());
        }

        int idx = 1;
        for (String size : sizes) {
            Map<String, String> attrs = new HashMap<>();
            attrs.put("size", size);
            productVariantRepository.save(ProductVariant.builder()
                .product(product).sku(sku + "-" + String.format("%03d", idx++))
                .name(size).attributes(attrs)
                .priceAdjustment(BigDecimal.ZERO)
                .stockQuantity(50 + rand.nextInt(100))
                .lowStockThreshold(10).isActive(true).build());
        }
        log.info("Created: {}", name);
    }
}
