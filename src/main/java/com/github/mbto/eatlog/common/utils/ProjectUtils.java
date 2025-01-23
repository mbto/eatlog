package com.github.mbto.eatlog.common.utils;

import com.github.mbto.eatlog.Application;
import com.github.mbto.eatlog.common.custommodel.InternalAccount;
import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Setting;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.RenderMapping;
import org.jooq.impl.*;
import org.springframework.boot.autoconfigure.jooq.JooqExceptionTranslator;
import org.springframework.boot.autoconfigure.jooq.SpringTransactionProvider;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.github.mbto.eatlog.common.Constants.*;
import static com.github.mbto.eatlog.common.custommodel.InternalAccount.*;
import static com.github.mbto.eatlog.webapp.WebUtils.msgFromBundle;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
public class ProjectUtils {
//    public static final UIntegerComparator uIntegerComparator = new UIntegerComparator();
    public static final Comparator<Number> numberComparator = Comparator.comparingDouble(Number::doubleValue);

//    public static Supplier<Set<LocalDate>> descSortedLocalDateContainerCreator() {
//        return () -> new TreeSet<>(Comparator.<LocalDate>naturalOrder().reversed());
//    }

    public static Map<String, String> collectResources(String resourceDirName, String extension) {
        try {
            ClassLoader classLoader = Application.class.getClassLoader();
            URL url = classLoader.getResource("");
            if (url == null) {
                throw new RuntimeException("Unable to get resource URL from classloader=" + classLoader);
            }
            URI uri = url.toURI();
            if (uri.getScheme().equals("jar")) {
                synchronized (Application.class) {
                    try (FileSystem ignored = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                        Path dirPath = Paths.get(uri).getParent() // Why parent name is "classes!"? java19?
                                .resolve("classes")
                                .resolve(resourceDirName);
                        return collectResources(dirPath, extension);
                    }
                }
            } else {
                Path dirPath = Paths.get(uri).getParent().getParent().getParent()
                        .resolve("resources")
                        .resolve("main")
                        .resolve(resourceDirName);
                return collectResources(dirPath, extension);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed load resource from " +
                    "'resources/" + resourceDirName + "'", e);
        }
    }

    private static Map<String, String> collectResources(Path resourceDirPath, String extension) throws IOException {
        if (!Files.isDirectory(resourceDirPath))
            return Collections.emptyMap();
        Map<String, String> payloadByFilename = new TreeMap<>(Comparator.naturalOrder());
        Files.walkFileTree(resourceDirPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path internalPath, BasicFileAttributes attrs) {
                String fileName = internalPath.getFileName().toString();
                String actualExtension = null;
                int dot = fileName.lastIndexOf(".");
                if (dot > -1) {
                    actualExtension = fileName.substring(dot + 1);
                    fileName = fileName.substring(0, dot); // fileName.extension -> fileName
                }
                if(!extension.equals(actualExtension))
                    return FileVisitResult.CONTINUE;
                String payload;
                try {
                    payload = Files.readString(internalPath, StandardCharsets.UTF_8);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                if(extension.equals("sql")) {
                    payload = "/*\n" + fileName + " */\n" + payload;
                }
                if(payloadByFilename.put(fileName, payload) != null) {
                    throw new IllegalStateException("Resource from '" + internalPath.toAbsolutePath() + "' "
                            + "already exists as '" + fileName + "'");
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return payloadByFilename;
    }

    public static DefaultDSLContext configurateJooqContext(HikariDataSource hikariDataSource) {
        return configurateJooqContext(hikariDataSource, MILLISECONDS.toSeconds(hikariDataSource.getMaxLifetime()), null, null);
    }

    public static DefaultDSLContext configurateJooqContext(HikariDataSource hikariDataSource, long queryTimeoutSec) {
        return configurateJooqContext(hikariDataSource, queryTimeoutSec, null, null);
    }

    public static DefaultDSLContext configurateJooqContext(HikariDataSource hikariDataSource, String schema, String overridedSchema) {
        return configurateJooqContext(hikariDataSource, MILLISECONDS.toSeconds(hikariDataSource.getMaxLifetime()), schema, overridedSchema);
    }

    public static DefaultDSLContext configurateJooqContext(HikariDataSource hikariDataSource,
                                                           long queryTimeoutSec,
                                                           String schema,
                                                           String overridedSchema) {
        DefaultConfiguration config = new DefaultConfiguration();
        config.set(new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(hikariDataSource)))
                .set(new DefaultExecuteListenerProvider(new JooqExceptionTranslator()))
                .set(new SpringTransactionProvider(new DataSourceTransactionManager(hikariDataSource)))
                .set(SQLDialect.MYSQL)
        ;
        config.settings()
                .withInterpreterDialect(SQLDialect.MYSQL)
                .withParseDialect(SQLDialect.MYSQL)
                .withQueryTimeout(Math.toIntExact(queryTimeoutSec))
        ;
        boolean overridingSchema = schema != null && overridedSchema != null && !schema.equalsIgnoreCase(overridedSchema);
        if (overridingSchema) {
            config.settings()
                    .withRenderMapping(new RenderMapping()
                            .withSchemata(new MappedSchema()
                                    .withInput(schema)
                                    .withOutput(overridedSchema)
                            )
                    )
            ;
        }
        DefaultDSLContext defaultDSLContext = new DefaultDSLContext(config);
        if (schema != null)
            defaultDSLContext.setSchema(overridingSchema ? overridedSchema : schema).execute();
        return defaultDSLContext;
    }

//    public static HikariDataSource buildHikariDataSource(Project project) {
//        return buildHikariDataSource(project, null);
//    }
//
//    public static HikariDataSource buildHikariDataSource(Project project, Properties properties) {
//        return buildHikariDataSource("pool-" + project.getDatabaseSchema() + " [" + project.getId() + "] " + project.getName(),
//                "jdbc:mysql://" + project.getDatabaseHostport() + "/",
//                project.getDatabaseSchema(),
//                project.getDatabaseUsername(),
//                project.getDatabasePassword(),
//                properties);
//    }

    public static HikariDataSource buildHikariDataSource(String poolName) {
        return buildHikariDataSource(poolName, null, null, null, null, null);
    }

    public static HikariDataSource buildHikariDataSource(String poolName, String schema) {
        return buildHikariDataSource(poolName, null, schema, null, null, null);
    }

    public static HikariDataSource buildHikariDataSource(String poolName, String schema, Properties properties) {
        return buildHikariDataSource(poolName, null, schema, null, null, properties);
    }

    public static HikariDataSource buildHikariDataSource(String poolName,
                                                         String jdbcUrl,
                                                         String schema,
                                                         String username,
                                                         String password,
                                                         Properties properties) {
        HikariDataSource hds = DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .type(HikariDataSource.class)
                .build();
        hds.setPoolName(poolName.replace(':', '-'));
        if (jdbcUrl != null)
            hds.setJdbcUrl(jdbcUrl);
        if (schema != null)
            hds.setSchema(schema);
        if (username != null)
            hds.setUsername(username);
        if (password != null)
            hds.setPassword(password);
        // Settings will be overrided from application.properties before creating bean "eatlogDataSource"
        // This is not final values:
        hds.setMaximumPoolSize(2);
        hds.setMinimumIdle(1);

        hds.setConnectionTimeout(SECONDS.toMillis(10));
        hds.setValidationTimeout(SECONDS.toMillis(5));
        hds.setIdleTimeout(SECONDS.toMillis(59));
        hds.setMaxLifetime(hds.getIdleTimeout() + SECONDS.toMillis(1));

        if (properties != null && !properties.isEmpty())
            hds.setDataSourceProperties(properties);

        return hds;
    }

    public static String hikariDataSourceToString(HikariDataSource hds) {
        return "Using datasource settings: jdbcUrl=" + hds.getJdbcUrl()
                + ", schema=" + hds.getSchema()
                + ", username=" + hds.getUsername()
                + ", dataSourceProperties=" + hds.getDataSourceProperties();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static int pointwiseUpdateQuery(DSLContext dslContext, TableField pkField, Object pkValue,
                                           List<Pair<Field, ?>> updatableFields) {
        List<Field> selectFields = new ArrayList<>(updatableFields.size() + 2);
        Field aliasedPkField;
        selectFields.add(aliasedPkField = pkField.as("pkey"));
        for (Pair<Field, ?> updatableField : updatableFields) {
            selectFields.add(updatableField.getLeft());
        }
        Table updatableTable = pkField.getTable();
        CommonTableExpression<Record> cte = DSL.name("cte")
                .as(DSL.select(selectFields)
                        .from(updatableTable)
                        .where(pkField.eq(pkValue)));
        Collation collation = DSL.collation("utf8mb4_bin");
        Condition condition = null;
        for (Pair<Field, ?> updatableField : updatableFields) {
            Field targetField = updatableField.getLeft();
            Object newValue = updatableField.getRight();
            Condition newCondition;
            if (newValue == null) {
                newCondition = targetField.isNotNull();
            } else {
                Condition qualifiedCondition;
                if (targetField.getType() == JSON.class
                        || targetField.getType() == TreeSet.class) {
                    qualifiedCondition = cte.field(targetField)
                            .notEqual(DSL.val(newValue).cast(JSON.class));
                } else if (newValue instanceof String) {
                    // 'ddd': `cte`.`description` collate utf8mb4_bin <> 'DDD')
                    qualifiedCondition = cte.field(targetField)
                            .collate(collation)
                            .notEqual(newValue);
//                    qualifiedCondition = cte.field(targetField)
//                          .collate(collation)
//                          .notEqual(DSL.value(newValue).collate(collation));
                } else {
                    qualifiedCondition = cte.field(targetField)
                            .notEqual(newValue);
                }
                newCondition = DSL.or(cte.field(targetField).isNull(), qualifiedCondition);
            }
            condition = condition != null ? DSL.or(condition, newCondition) : newCondition;
        }
        UpdateSetFirstStep updateStep = dslContext.with(cte)
                .update(updatableTable.join(cte)
                        .on(pkField.eq(cte.field(aliasedPkField)))
                );
        for (Pair<Field, ?> updatableField : updatableFields) {
            Field targetField = updatableField.getLeft();
            Object newValue = updatableField.getRight();
            updateStep.set(targetField, newValue);
        }
        return ((UpdateSetMoreStep) updateStep)
                .where(pkField.eq(pkValue), condition)
                .execute();
    }

//    /**
//     * @return "@ 2 500 914 bytes (2.39 mb)"
//     */
//    public static String buildHumanFileSize(long value) {
//        DecimalFormat decLongFormat;
//        DecimalFormatSymbols separators = new DecimalFormatSymbols();
//        separators.setDecimalSeparator(',');
//        separators.setGroupingSeparator(' ');
//
//        decLongFormat = new DecimalFormat("###,###", separators);
//        decLongFormat.setGroupingUsed(true);
//        decLongFormat.setMinimumFractionDigits(0);
//        decLongFormat.setMaximumFractionDigits(0);
//
//        return " @ " + decLongFormat.format(value) + " bytes " + String.format("(%.2f mb)", value / 1024f / 1024f);
//    }

//    public static long MBtoBytes(int mb) {
//        return mb * 1024 * 1024L;
//    }

    public static String addDotsForBigString(String value, int maxLen) {
        int len = value.length();
        if(len > maxLen) {
            return value.substring(0, maxLen) + "...";
        }
        return value;
    }

    public static String concatStrings(String str1, String str2) {
        return str1 + str2;
    }

    public static String declension(boolean simpleDeclensionEnabled,
                                    long value,
                                    String leftPart,
                                    String oneValue,
                                    String fourValues,
                                    String fiveValues) {
        if(simpleDeclensionEnabled) {
            return concatStrings(leftPart, isValueEqualsOne(value) ? oneValue : fiveValues);
        }
        value = Math.abs(value) % 100;
        if (value > 10 && value < 20) {
            return concatStrings(leftPart, fiveValues);
        }
        value = value % 10;
        if (value > 1 && value < 5) {
            return concatStrings(leftPart, fourValues);
        }
        return concatStrings(leftPart, isValueEqualsOne(value) ? oneValue : fiveValues);
    }

    public static String declensionValuedL10N(long value, String keyPrefix) {
        boolean simpleDeclensionEnabled = Boolean.parseBoolean(msgFromBundle(BUNDLE_PROPS_SIMPLE_DECLENSION_ENABLED));
        String leftPart = msgFromBundle(keyPrefix + ".0");
        if (simpleDeclensionEnabled) {
            return value + " "
                    + concatStrings(leftPart,
                        isValueEqualsOne(value)
                            ? msgFromBundle(keyPrefix + ".1")
                            : msgFromBundle(keyPrefix + ".5"));
        }
        return value + " " + declension(simpleDeclensionEnabled,
                value,
                leftPart,
                msgFromBundle(keyPrefix + ".1"),
                msgFromBundle(keyPrefix + ".4"),
                msgFromBundle(keyPrefix + ".5"));
    }

    public static String declensionValued(long value, String leftPart,
                                          String oneValue, String fourValues, String fiveValues) {
        return declensionValued(true, value, leftPart, oneValue, fourValues, fiveValues);
    }

    public static String declensionValued(boolean simpleDeclensionEnabled, long value, String leftPart,
                                          String oneValue, String fourValues, String fiveValues) {
        return value + " "
                + declension(simpleDeclensionEnabled, value, leftPart, oneValue, fourValues, fiveValues);
    }

    public static boolean isValueEqualsOne(long value) {
        return value == -1 || value == 1;
    }

//    public static String declension2(long value, String word) {
//        return declension2(value, word, "s");
//    }

//    public static String declension2(long value, String word, String moreOne) {
//        return declension2(value, word, moreOne, "");
//    }

//    public static String declension2(long value, String word, String moreOne, String oneOrZero) {
//        return value + " " + word + (value > 1 ? moreOne : oneOrZero);
//    }

//    public static String productToString(Product product) {
//        return product.getTitle() + "[" + product.getId() + "]";
//    }

//    public static String selectItemToString(SelectItem selectItem) {
//        return "SelectItem{label=" + selectItem.getLabel() + ", value=" + selectItem.getValue() + "}";
//    }

//    public static String settingToString(Setting setting) {
//        return "Setting{id=" + setting.getId() +
//                ", key=" + setting.getKey() +
//                ", value.length=" + (setting.getValue() != null ? setting.getValue().length() : null) + "}";
//    }

    public static TreeSet<String> convertRolesToNullIfEmpty(InternalAccount object) {
        return convertValueToNullIfEmptyOrFalse(object, rolesGetterFunc, rolesSetterFunc);
    }
    public static Boolean convertIsBannedToNullIfFalse(InternalAccount object) {
        return convertValueToNullIfEmptyOrFalse(object, isBannedGetterFunc, isBannedSetterFunc);
    }
    public static TreeSet<String> convertRolesToNullIfEmpty(Setting object) {
        return convertValueToNullIfEmptyOrFalse(object, settingRolesGetterFunc, settingRolesSetterFunc);
    }
    public static <T, R> R convertValueToNullIfEmptyOrFalse(T object,
                                                            Function<T, R> getterFunc,
                                                            BiConsumer<T, R> setterFunc) {
        R value = getterFunc.apply(object);
        if(value == null) {
            return null;
        }
        if(value instanceof Boolean booleanValue && !booleanValue
                || value instanceof Collection<?> container && container.isEmpty()) {
            setterFunc.accept(object, null);
            return null;
        }
        return value;
    }
}