package com.szml.movieticket.server.security;

import com.szml.movieticket.pojo.enums.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 自定义 UserDetails。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
@Getter
public class AdminUserDetails implements UserDetails {

    private final Long userId;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public AdminUserDetails(Long userId, String password, UserRole role, boolean enabled) {
        this.userId = userId;
        this.password = password;
        this.enabled = enabled;
        String roleName = (role == UserRole.ADMIN) ? "ROLE_ADMIN" : "ROLE_USER";
        this.authorities = List.of(new SimpleGrantedAuthority(roleName));
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return String.valueOf(userId); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return enabled; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}
