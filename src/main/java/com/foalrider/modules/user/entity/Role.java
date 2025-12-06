package com.foalrider.modules.user.entity;

import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Role entity for RBAC.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "description")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permissions", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> permissions = new ArrayList<>();

    @Column(name = "is_system")
    @Builder.Default
    private Boolean isSystem = false;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    @Builder.Default
    private List<User> users = new ArrayList<>();

    /**
     * Check if role has a specific permission.
     */
    public boolean hasPermission(String permission) {
        if (permissions == null) {
            return false;
        }
        // Super admin has all permissions
        if (permissions.contains("*")) {
            return true;
        }
        return permissions.contains(permission);
    }
}
