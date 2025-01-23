package com.github.mbto.eatlog;

import lombok.extern.slf4j.Slf4j;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.*;
import org.jooq.meta.jaxb.Database;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Run configuration for generate model classes without compile project with unexisted model classes:
 * :test --tests "com.github.mbto.eatlog.ManualGenerateModel.generateModel" -x compileJava
 * Add environment "GENERATE_MODEL" for run generation. (This is protection against autotests)
 */
@SuppressWarnings("NewClassNamingConvention")
@Slf4j
public class ManualGenerateModel {
    @Test
    public void generateModel() throws Throwable {
        if(System.getenv("GENERATE_MODEL") == null) {
            log.info("Skipped generating model, environment GENERATE_MODEL required");
            return;
        }
        String targetPackage = "com.github.mbto.eatlog.common.model.eatlog";
        Path targetPackagePath = Paths.get(ManualGenerateModel.class.getResource("/").toURI()).getParent()
                .getParent().getParent().getParent()
                .resolve("src").resolve("main").resolve("java")
                .toAbsolutePath();
        log.info("targetPackage=" + targetPackage);
        log.info("targetPackagePath=" + targetPackagePath);
        Configuration configuration = new Configuration()
                .withJdbc(new Jdbc()
                        .withDriver("com.mysql.cj.jdbc.Driver")
                        .withUrl("jdbc:mysql://127.0.0.1:3306/")
                        .withUser("root")
                        .withPassword("root")
//                        .withProperties(
//                                new Property()
//                                        .withKey("user")
//                                        .withValue("root"),
//                                new Property()
//                                        .withKey("password")
//                                        .withValue("root")
//                        )
                )
                .withGenerator(new Generator()
                        .withDatabase(new Database()
                                .withName("org.jooq.meta.mysql.MySQLDatabase")
                                .withIncludes(".*")
//                                .withExcludes("")
                                .withSchemata(
                                        new SchemaMappingType().withInputSchema("eatlog")/*,
                                        new SchemaMappingType().withInputSchema("eatlog_maxmind_city")*/
                                )
                                .withForcedTypes(
                                        new ForcedType()
                                                .withTypes("(?i:TINYINT UNSIGNED)")
                                                .withName("BOOLEAN")
//                                        ,new ForcedType()
//                                                .withTypes("(?i:JSON)")
//                                                .withName("VARCHAR")
                                        , new ForcedType()
                                                .withTypes("(?i:JSON)")
                                                .withJsonConverter(true)
                                                .withIncludeExpression(".*\\.roles")
                                                .withUserType("java.util.TreeSet<java.lang.String>")
//                                                .withUserType("com.fasterxml.jackson.databind.JsonNode")
                                )
                        )
                        .withGenerate(new Generate()
                                .withDaos(false)
                                .withRoutines(true)
                                .withPojos(true)
                                .withPojosEqualsAndHashCode(true)
                                .withValidationAnnotations(true)
                                .withJavaTimeTypes(true)
//                                .withJpaAnnotations(true)
                        )
                        .withTarget(new Target()
                                .withDirectory(targetPackagePath.toString())
                                .withPackageName(targetPackage)
                                .withEncoding("UTF-8")
                                .withClean(true)
                        )
                );
        GenerationTool.generate(configuration);
        log.info("Successfully generated model with package " + targetPackage);
    }
}