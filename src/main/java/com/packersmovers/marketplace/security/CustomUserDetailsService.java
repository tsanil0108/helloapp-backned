package com.packersmovers.marketplace.security;

import com.packersmovers.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Login identifier can be either email or mobile - we try both. */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(identifier)
                .or(() -> userRepository.findByMobile(identifier))
                .map(CustomUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user found for: " + identifier));
    }
}
