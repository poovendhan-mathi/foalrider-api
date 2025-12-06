package com.foalrider.config;

import com.foalrider.modules.user.entity.Role;
import com.foalrider.modules.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initializes default data on application startup.
 * Creates default roles if they don't exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        initializeRoles();
    }

    private void initializeRoles() {
        log.info("Checking and initializing default roles...");
        
        List<RoleDefinition> defaultRoles = List.of(
            new RoleDefinition("ROLE_ADMIN", "Administrator with full access", 
                List.of("*")),
            new RoleDefinition("ROLE_CUSTOMER", "Regular customer", 
                List.of("orders:read", "orders:write", "profile:read", "profile:write", "cart:read", "cart:write")),
            new RoleDefinition("ROLE_VENDOR", "Vendor/Seller", 
                List.of("products:read", "products:write", "orders:read", "profile:read", "profile:write", "inventory:read", "inventory:write"))
        );

        for (RoleDefinition def : defaultRoles) {
            if (roleRepository.findByName(def.name).isEmpty()) {
                Role role = Role.builder()
                    .name(def.name)
                    .description(def.description)
                    .permissions(def.permissions)
                    .isSystem(true)
                    .build();
                roleRepository.save(role);
                log.info("Created role: {}", def.name);
            } else {
                log.debug("Role already exists: {}", def.name);
            }
        }
        
        log.info("Role initialization complete. Total roles: {}", roleRepository.count());
    }

    private record RoleDefinition(String name, String description, List<String> permissions) {}
}
