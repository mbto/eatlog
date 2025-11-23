package com.github.mbto.eatlog.webapp;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MainPageRedirect implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.xhtml");
//        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>
    containerCustomizer() {
        return container -> {
            container.addErrorPages(new ErrorPage(HttpStatus.FORBIDDEN, "/403.xhtml"));
            container.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/404.xhtml"));
//            container.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/500.xhtml"));
            container.addErrorPages(new ErrorPage(Throwable.class, "/error.xhtml"));
        };
    }
}