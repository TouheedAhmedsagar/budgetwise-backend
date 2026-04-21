package com.budget.service;

import com.budget.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired UserRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = repo.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Not found: " + username));
        return new User(user.getUsername(), user.getPassword(), Collections.emptyList());
    }
}
