package com.github.mbto.eatlog;

import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import static com.github.mbto.eatlog.common.model.eatlog.Eatlog.EATLOG;
import static com.github.mbto.eatlog.utils.ProjectUtils.buildHikariDataSource;
import static com.github.mbto.eatlog.utils.ProjectUtils.configurateJooqContext;

@Configuration
public class JooqConfig {
    @Bean
    @Lazy(false)
    DSLContext eatlogDsl(HikariDataSource eatlogDataSource,
                         @Value("${eatlog.datasource.schema}") String schema
    ) {
        return configurateJooqContext(eatlogDataSource, EATLOG.getName(), schema);
    }

    @ConfigurationProperties("eatlog.datasource")
    @Bean
    HikariDataSource eatlogDataSource(
            @Value("${eatlog.datasource.schema}") String schema
    ) {
        return buildHikariDataSource("eatlog-pool", schema);
    }
}