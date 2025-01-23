package com.github.mbto.eatlog;

import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import static com.github.mbto.eatlog.common.model.eatlog.Eatlog.EATLOG;
import static com.github.mbto.eatlog.common.utils.ProjectUtils.buildHikariDataSource;
import static com.github.mbto.eatlog.common.utils.ProjectUtils.configurateJooqContext;


@Configuration
public class JooqConfig {
    @ConfigurationProperties("eatlog.datasource")
    @Bean
    HikariDataSource eatlogDataSource() {
        return buildHikariDataSource("eatlog-pool", EATLOG.getName());
    }

    @Bean
    @Lazy(false)
    DSLContext eatlogDsl(HikariDataSource eatlogDataSource) {
        return configurateJooqContext(eatlogDataSource, EATLOG.getName(), null);
    }
}