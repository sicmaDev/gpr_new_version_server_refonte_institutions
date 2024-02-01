package com.sicmagroup.gpr.configuration;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.service.jwt.JwtService;
import com.sicmagroup.gpr.service.jwt.JwtServiceImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtServiceImpl jwtServiceImpl;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("Ressss");
        // request.setAttribute("Access-Control-Allow-Origin", "http://localhost:3000");
        System.out.println(request.getHeader("origin"));
        // jwt token is in the header. So trying to extract the header
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        // if the header is null or don't start with bearer the check step failed so
        // return 403 HTTP response.
        // The token need to start with Bearer !!!
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // try to extract the token from the authHeader (authentication header)
        jwt = authHeader.substring(7);
        try {
            userEmail = jwtServiceImpl.extracUserName(jwt);
            // System.out.println(userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtServiceImpl.isTokenValid(jwt, userDetails)) {
                    // update the securityContextHolder
                    // System.out.println(userDetails.getAuthorities());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                            userDetails.getPassword(), userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                } else {

                    throw new ServletException("Token invalid, ré-authentifiez vous svp!");
                    // System.out.println("TOKEN NOT VALID");
                }
            }
           
            
        } catch (Exception e) {
            // System.out.println(e.getMessage());
            if(e.getMessage() != null && e.getMessage().startsWith("JWT expired")){
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write( "{\"status\":false, \"content\":{\"title\": \"Token invalide\", \"message\":\"Connectez-vous à nouveau pour accéder à cette ressource\"} }");
                response.getWriter().flush();
                response.getWriter().close();
            } else {
                throw new ServletException("Token invalid, re-authentifiez vous svp!");
            }
            // throw new ServletException("Token invalid, ré-authentifiez vous svp!");
            //   ObjectMapper mapper = new ObjectMapper();
            // ApiResponseDto apiResponseDto = ApiResponseDto
            //     .builder()
            //     .content(ErrorResponse.builder().title("Token invalid").message("Connectez-vous à nouveau pour accéder à cette ressource"))
            //     .build();
           
        }
        // userEmail = jwtServiceImpl.extracUserName(jwt);
        // verify if the user is already authenticated. If not getAuthentication() is
        // null

        //  System.out.println("userDetails.getAuthorities()");
         filterChain.doFilter(request, response);

    }

}
