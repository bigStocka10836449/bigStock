package com.bigstock.biz.infra;

import java.io.IOException;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

@Configuration(proxyBeanMethods = false)
public class MarketQuoteImportNetworkConfig {

    private static final int INTERNAL_PORT = 18082;
    private static final String INTERNAL_CONNECTOR = MarketQuoteImportNetworkConfig.class.getName();

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> marketQuoteImportConnector() {
        return factory -> {
            Connector internal = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            internal.setPort(INTERNAL_PORT);
            factory.addAdditionalTomcatConnectors(internal);
            factory.addEngineValves(new ValveBase(true) {
                @Override
                public void invoke(Request request, Response response) throws IOException, ServletException {
                    // Use the actual connector even when a proxy supplies forwarded headers.
                    request.setAttribute(INTERNAL_CONNECTOR, request.getConnector() == internal);
                    getNext().invoke(request, response);
                }
            });
        };
    }

    public static boolean isInternal(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(INTERNAL_CONNECTOR));
    }
}
