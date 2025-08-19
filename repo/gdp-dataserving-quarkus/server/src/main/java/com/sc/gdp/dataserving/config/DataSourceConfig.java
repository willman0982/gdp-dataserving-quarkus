package com.sc.gdp.dataserving.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.Optional;

/**
 * Configuration for multiple data sources
 * Supports PostgreSQL, Trino, StarRocks, and other databases
 */
@ConfigMapping(prefix = "gdp.datasource")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DataSourceConfig {

    /**
     * Primary data source type
     */
    @WithName("type")
    @WithDefault("postgresql")
    String type();

    /**
     * Trino configuration
     */
    Optional<TrinoConfig> trino();

    /**
     * StarRocks configuration
     */
    Optional<StarRocksConfig> starrocks();

    /**
     * Custom JDBC configuration
     */
    Optional<CustomJdbcConfig> custom();

    interface TrinoConfig {
        String url();
        Optional<String> catalog();
        Optional<String> schema();
        Optional<String> username();
        Optional<String> password();
    }

    interface StarRocksConfig {
        String url();
        Optional<String> database();
        Optional<String> username();
        Optional<String> password();
    }

    interface CustomJdbcConfig {
        String url();
        String driverClass();
        Optional<String> username();
        Optional<String> password();
    }
}