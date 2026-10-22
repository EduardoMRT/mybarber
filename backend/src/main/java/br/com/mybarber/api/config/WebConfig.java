package br.com.mybarber.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final BarbeariaBloqueadaInterceptor bloqueio;

    public WebConfig(BarbeariaBloqueadaInterceptor bloqueio) {
        this.bloqueio = bloqueio;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(bloqueio).addPathPatterns("/api/painel/**");
    }
}
