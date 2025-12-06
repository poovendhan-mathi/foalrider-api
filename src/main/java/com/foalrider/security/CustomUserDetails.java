package com.foalrider.security;

import com.foalrider.modules.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Custom UserDetails implementation wrapping our User entity.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final String roleName;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.roleName = user.getRole().getName();
        this.enabled = user.isEnabled();
        this.accountNonLocked = user.isAccountNonLocked();
        
        // Ensure role has ROLE_ prefix for Spring Security hasRole() checks
        String roleAuthority = user.getRole().getName();
        if (!roleAuthority.startsWith("ROLE_")) {
            roleAuthority = "ROLE_" + roleAuthority;
        }
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority(roleAuthority)
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
