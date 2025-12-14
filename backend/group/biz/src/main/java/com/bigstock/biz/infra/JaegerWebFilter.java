//package com.bigstock.gateway.infra;
//
//import io.opentracing.Scope;
//import io.opentracing.Span;
//import io.opentracing.Tracer;
//import io.opentracing.tag.Tags;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.web.server.ServerWebExchange;
//import org.springframework.web.server.WebFilter;
//import org.springframework.web.server.WebFilterChain;
//import reactor.core.publisher.Mono;
//
//import java.net.Inet6Address;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//@Configuration
//@Order(Integer.MIN_VALUE)
//public class JaegerWebFilter implements WebFilter {
//
//    @Value("${spring.application.name}")
//    private String applicationName;
//
//    @Autowired
//    private Tracer tracer;
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
//        if ( exchange.getRequest().getURI().getPath().startsWith("/actuator")) {
//            return chain.filter(exchange);
//        }
//    	Span span = tracer.buildSpan(applicationName).start();
//        Scope scope = tracer.activateSpan(span);
//
//        
//        try {
//        	// Collect request data before the filter chain
//        	collectRequestData(exchange, span);
//
//            return chain.filter(exchange).doFinally(signalType -> {
//                scope.close();
//                span.finish();
//            });
//        } catch (Exception e) {
//            span.setTag(Tags.ERROR, true);
//            span.log(Map.of(
//                    "event", "error",
//                    "error.object", e,
//                    "message", e.getMessage(),
//                    "stack", e.getStackTrace()
//            ));
//            throw e;
//        } finally {
//        	span.log(Map.of(
//                      "event", "handle",
//                      "method", exchange.getRequest().getMethod(),
//                      "url", getFullUrl(exchange.getRequest()),
//                      "status", exchange.getResponse().getStatusCode()
//                  ));
//            scope.close();
//            span.finish();
//        }
//    }
//
//    private void collectRequestData(ServerWebExchange exchange, Span span) {
//        ServerHttpRequest request = exchange.getRequest();
//        Tags.COMPONENT.set(span, "java-spring-webflux");
//        Tags.HTTP_METHOD.set(span, request.getMethod().name());
//        Tags.HTTP_URL.set(span, request.getURI().toString());
//        Optional.ofNullable(request.getRemoteAddress()).ifPresent(remoteAddress -> {
//            Tags.PEER_HOSTNAME.set(span, remoteAddress.getHostString());
//            Tags.PEER_PORT.set(span, remoteAddress.getPort());
//            Optional.ofNullable(remoteAddress.getAddress()).ifPresent(inetAddress -> {
//                if (inetAddress instanceof Inet6Address) {
//                    Tags.PEER_HOST_IPV6.set(span, inetAddress.getHostAddress());
//                } else {
//                    Tags.PEER_HOST_IPV4.set(span, inetAddress.getHostAddress());
//                }
//            });
//        });
//
//        // Log additional attributes if needed
//        Map<String, Object> logs = new HashMap<>();
//        logs.put("http.method", request.getMethod().name());
//        logs.put("http.url", request.getURI().toString());
//        logs.put("http.host", request.getRemoteAddress().getHostString());
//        logs.put("http.port", request.getRemoteAddress().getPort());
//        span.log(logs);
//    }
//    
//    private String getFullUrl(ServerHttpRequest request) {
//        StringBuilder url = new StringBuilder(request.getURI().toString());
//        String query = request.getURI().getQuery();
//        if (query != null && !query.isEmpty()) {
//            url.append('?').append(query);
//        }
//        return url.toString();
//    }
//}