//package com.bigstock.biz.infra;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import io.opentracing.Scope;
//import io.opentracing.Span;
//import io.opentracing.Tracer;
//import io.opentracing.tag.Tags;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//
//@Component
//@Order(Integer.MIN_VALUE)
//public class JaegerWebFilter extends OncePerRequestFilter {
//
//    @Value("${spring.application.name}")
//    private String applicationName;
//
//    private final Tracer tracer;
//
//    public JaegerWebFilter(Tracer tracer) {
//        this.tracer = tracer;
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//            throws ServletException, IOException {
//        // 过滤掉 Actuator 的 URL
//        if ( request.getRequestURI().startsWith("/actuator")) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//        Span span = tracer.buildSpan(applicationName).start();
//        Scope scope = tracer.activateSpan(span);
//
//        try {
//            filterChain.doFilter(request, response);
//        } catch (Exception e) {
//            span.setTag("error", true);
//            span.log(Map.of(
//                    "event", "error",
//                    "error.object", e,
//                    "message", e.getMessage(),
//                    "stack", e.getStackTrace()
//            ));
//            throw e;
//        } finally {
//            span.log(spanLogDecorator(request, span));
//            scope.close();
//            span.finish();
//        }
//    }
//
//    private Map<String, Object> spanLogDecorator(HttpServletRequest request, Span span) {
//        Map<String, Object> logs = new HashMap<>();
//        logs.put("event", "handle");
//
//        String handler = request.getRequestURI();
//        logs.put("handler", handler);
//
//        Tags.COMPONENT.set(span, "java-spring-web");
//        Tags.HTTP_METHOD.set(span, request.getMethod());
//        Tags.HTTP_URL.set(span, request.getRequestURL().toString());
//        Optional.ofNullable(request.getRemoteAddr()).ifPresent(remoteAddress -> {
//            Tags.PEER_HOSTNAME.set(span, remoteAddress);
//            // 根据IP地址类型设置相应的标签
//            if (remoteAddress.contains(":")) {
//                Tags.PEER_HOST_IPV6.set(span, remoteAddress);
//            } else {
//                Tags.PEER_HOST_IPV4.set(span, remoteAddress);
//            }
//        });
//        logs.put("http.method", request.getMethod());
//        logs.put("http.url", request.getRequestURL().toString());
//        logs.put("http.host", request.getRemoteHost());
//        logs.put("http.port", request.getRemotePort());
//
//        return logs;
//    }
//}
