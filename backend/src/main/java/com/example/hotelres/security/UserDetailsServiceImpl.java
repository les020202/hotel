package com.example.hotelres.security;

import com.example.hotelres.user.User;
import com.example.hotelres.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service @RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository users;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        User u = users.findByLoginId(loginId).orElseThrow(
                () -> new UsernameNotFoundException("user not found"));
        return new org.springframework.security.core.userdetails.User(
                u.getLoginId(), u.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(u.getRole().name())));
    }
}
