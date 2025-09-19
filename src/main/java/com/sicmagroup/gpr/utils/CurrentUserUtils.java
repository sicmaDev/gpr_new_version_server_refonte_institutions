package com.sicmagroup.gpr.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;

import lombok.RequiredArgsConstructor;

/**
 * CurrentUserUtils
 */
@RequiredArgsConstructor
@Service
public class CurrentUserUtils {

    private final AuthenticationServiceImpl authServiceImpl;
    private final ServicePointRepository servicePointRepository;

    
    public User getUser() throws Exception {
        try {

            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                    .getPrincipal();
            User connectedUser = authServiceImpl.getByEmail(userDetails.getUsername());
            return connectedUser;

        } catch (Exception e) {

            throw e;
        }
    }
}