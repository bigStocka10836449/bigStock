package com.bigstock.sharedComponent.jaeger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

import io.micrometer.observation.ObservationPredicate;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;

//import io.jaegertracing.internal.JaegerTracer;
//import io.opentracing.Tracer;
//import io.opentracing.noop.NoopTracerFactory;
//import io.opentracing.util.GlobalTracer;

@Configuration
public class JaegerConfig {

	@Value("${tracing.url}")
	private String tracingUrl;
//    @Value("${opentracing.jaeger.enabled:false}")
//    private boolean jaegerEnabled;
//
//    @Value("${opentracing.jaeger.http-sender.url}")
//    private String opentracingHttpSenderUrl;
//
//	@Value("${spring.application.name}")
//	private String applicationName;
//  
//	@Value("${opentracing.jaeger.sampler.param}")
//	private float samplerParam;
//	
//	@Value("${opentracing.jaeger.sampler.type}")
//	private String samplerType;
	
//    @Bean
//    public Tracer jaegerTracer() {
//
//		if (!jaegerEnabled) {
//			return NoopTracerFactory.create();
//		}
//
//        JaegerTracer tracer = new io.jaegertracing.Configuration(applicationName)
//                .withSampler(new io.jaegertracing.Configuration.SamplerConfiguration().withType(samplerType).withParam(samplerParam))
//                .withReporter(new io.jaegertracing.Configuration.ReporterConfiguration()
//                        .withSender(new io.jaegertracing.Configuration.SenderConfiguration().withEndpoint(opentracingHttpSenderUrl)
//                                ).withLogSpans(true)).getTracer();
//
//        GlobalTracer.registerIfAbsent(tracer);
//
//        return tracer;
//    }
	
	@Bean
	public OtlpGrpcSpanExporter otlpHttpSpanExporter() {
	  return OtlpGrpcSpanExporter.builder().setEndpoint(tracingUrl).build();
	}
	
    @Bean
    public ObservationPredicate noSpringSecurity() {
        return (name, context) -> !name.startsWith("spring.security.");
    }

    @Bean
    public ObservationPredicate noActuator() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext srCtx) {
                return !srCtx.getCarrier().getRequestURI().contains("/actuator/");
            }
            if(context instanceof org.springframework.http.server.reactive.observation.ServerRequestObservationContext srCtx) {
            	return  !srCtx.getCarrier().getURI().getPath().contains("/actuator/");
            }
            return true;
        };
    }

    @Bean
    public ObservationPredicate noSwagger() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext srCtx) {
                return !srCtx.getCarrier().getRequestURI().contains("/swagger");
            }
            if(context instanceof org.springframework.http.server.reactive.observation.ServerRequestObservationContext srCtx) {
            	return  !srCtx.getCarrier().getURI().getPath().contains("/swagger/");
            }
            return true;
        };
    }

    @Bean
    public ObservationPredicate noApiDocs() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext srCtx) {
                return !srCtx.getCarrier().getRequestURI().contains("/v3/api-docs");
            }
            if(context instanceof org.springframework.http.server.reactive.observation.ServerRequestObservationContext srCtx) {
            	return  !srCtx.getCarrier().getURI().getPath().contains("/v3/api-docs");
            }
            return true;
        };
    }
}
