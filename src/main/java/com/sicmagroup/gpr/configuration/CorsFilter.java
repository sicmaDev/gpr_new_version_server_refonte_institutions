// package com.sicmagroup.gpr.configuration;

// import java.io.IOException;

// import org.springframework.stereotype.Component;

// import jakarta.servlet.Filter;
// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.ServletRequest;
// import jakarta.servlet.ServletResponse;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import lombok.RequiredArgsConstructor;

// @Component
// @RequiredArgsConstructor
// public class CorsFilter implements Filter {@Override
//     public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//             throws IOException, ServletException {

//         HttpServletResponse res = (HttpServletResponse) response;
//          HttpServletRequest req = (HttpServletRequest) request;
//         res.setHeader("Access-Control-Allow-Origin", "*"); // Ou l'origine spécifique autorisée
//         res.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, OPTIONS, DELETE");
//         res.setHeader("Access-Control-Max-Age", "3600");
//         res.setHeader("Access-Control-Allow-Headers", "Access-Control-Allow-Headers, Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
//         res.setHeader("Access-Control-Expose-Headers", "*");
//         System.out.println("jfzbibihze");
//         System.out.println(res.getHeader("Access-Control-Allow-Origin"));
//         for (int i = 0; i < res.getHeaderNames().size(); i++) {
//             System.out.println(res.getHeaderNames().toArray()[i]);
//         }

//         // System.out.println(response.);
//         if ("OPTIONS".equalsIgnoreCase(request.getParameter("method"))) {
//             res.setStatus(HttpServletResponse.SC_OK);
//         } else {
//             chain.doFilter(req, res);
//         }
//     }
    
// }
