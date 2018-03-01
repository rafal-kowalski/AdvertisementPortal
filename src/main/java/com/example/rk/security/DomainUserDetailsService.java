package com.example.rk.security;

import com.example.rk.domain.User;
import com.example.rk.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DomainUserDetailsService implements UserDetailsService {
    private final Logger log = LoggerFactory.getLogger(DomainUserDetailsService.class);
    private UserRepository userRepository;

    public DomainUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(final String login) throws UsernameNotFoundException {
        log.debug("Authenticating: {}", login);
        //step 1: try login
        Optional<User> userByLogin = userRepository.findOneWithAuthoritiesByLogin(login);
        return userByLogin.map(this::createSecurityUser).orElseGet(() -> {
            //step 2: try email
            Optional<User> userByEmail = userRepository.findOneWithAuthoritiesByEmail(login);
            return userByEmail.map(this::createSecurityUser).orElseThrow(
                () -> new UsernameNotFoundException("User '" + login + "' was not found in database")
            );
        });
    }

    private org.springframework.security.core.userdetails.User createSecurityUser(User user) {
        //TODO check if user is activated
        List<GrantedAuthority> grantedAuthorities = user.getAuthorities().stream()
            .map(authority -> new SimpleGrantedAuthority(authority.getName()))
            .collect(Collectors.toList());
        return new org.springframework.security.core.userdetails.User(user.getLogin(),
            user.getPassword(),
            grantedAuthorities);
    }
}
