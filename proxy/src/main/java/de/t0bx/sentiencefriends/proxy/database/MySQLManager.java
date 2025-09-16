package de.t0bx.sentiencefriends.proxy.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import lombok.Getter;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class MySQLManager implements IMySQLManager, AutoCloseable {
    private HikariDataSource dataSource;
    @Getter
    private final ExecutorService executor;
    private final Logger logger;

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int poolSize;
    private final long connectionTimeout;
    private final long idleTimeout;
    private final long maxLifetime;

    private static final int DEFAULT_POOL_SIZE = 10;
    private static final long DEFAULT_CONNECTION_TIMEOUT = 30000;
    private static final long DEFAULT_IDLE_TIMEOUT = 600000;
    private static final long DEFAULT_MAX_LIFETIME = 1800000;

    public MySQLManager(String host, int port, String username, String password, String database) {
        this(host, port, username, password, database, DEFAULT_POOL_SIZE, DEFAULT_CONNECTION_TIMEOUT,
                DEFAULT_IDLE_TIMEOUT, DEFAULT_MAX_LIFETIME);
    }

    public MySQLManager(String host, int port, String username, String password, String database,
                        int poolSize, long connectionTimeout, long idleTimeout, long maxLifetime) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
        this.poolSize = poolSize;
        this.connectionTimeout = connectionTimeout;
        this.idleTimeout = idleTimeout;
        this.maxLifetime = maxLifetime;

        this.executor = Executors.newFixedThreadPool(Math.max(2, poolSize / 2));
        this.logger = ProxyPlugin.getInstance().getLogger();
    }

    @Override
    public void connect() throws SQLException {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database +
                    "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            config.setMaximumPoolSize(poolSize);
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(idleTimeout);
            config.setMaxLifetime(maxLifetime);

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");

            this.dataSource = new HikariDataSource(config);
            this.logger.info("Connected to MySQL using HikariCP with a pool size of {}!", poolSize);
        } catch (Exception exception) {
            throw new SQLException("Failed to initialize MySQL connection pool", exception);
        }
    }

    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            this.logger.info("HikariCP connection pool has been closed!");
        }
    }

    @Override
    public void close() {
        disconnect();
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Connection pool has not been initialized or has been closed!");
        }
        return dataSource.getConnection();
    }

    @Override
    public void update(String query, Object... params) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            setParameters(preparedStatement, params);
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public CompletableFuture<Void> updateAsync(String query, Object... params) {
        return CompletableFuture.runAsync(() -> {
            try {
                update(query, params);
            } catch (SQLException exception) {
                logger.error("Error while executing async update", exception);
                throw new RuntimeException("Error while updating MySQL", exception);
            }
        }, executor);
    }

    public int[] batchUpdate(String query, List<Object[]> batchParams) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            for (Object[] params : batchParams) {
                setParameters(statement, params);
                statement.addBatch();
            }

            return statement.executeBatch();
        }
    }

    public CompletableFuture<int[]> batchUpdateAsync(String query, List<Object[]> batchParams) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return batchUpdate(query, batchParams);
            } catch (SQLException exception) {
                logger.error("Error while executing async batch update",exception);
                throw new RuntimeException("Error while batch updating MySQL", exception);
            }
        }, executor);
    }

    @Override
    public <T> List<T> query(String query, Function<ResultSet, T> resultHandler, Object... params) throws SQLException {
        List<T> results = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            setParameters(preparedStatement, params);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(resultHandler.apply(resultSet));
                }
            }
        }

        return results;
    }

    @Override
    public <T> CompletableFuture<List<T>> queryAsync(String query, Function<ResultSet, T> resultHandler, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return query(query, resultHandler, params);
            } catch (SQLException exception) {
                logger.error("Error while executing async query", exception);
                throw new RuntimeException("Error while querying MySQL", exception);
            }
        }, executor);
    }

    public <T> Optional<T> queryFirst(String query, Function<ResultSet, T> resultHandler, Object... params) throws SQLException {
        List<T> results = query(query, resultHandler, params);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    public <T> CompletableFuture<Optional<T>> queryFirstAsync(String query, Function<ResultSet, T> resultHandler, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return queryFirst(query, resultHandler, params);
            } catch (SQLException exception) {
                logger.error("Error while executing async queryFirst", exception);
                throw new RuntimeException("Error while querying MySQL for first result", exception);
            }
        }, executor);
    }

    public <T> T queryScalar(String query, Class<T> type, Object... params) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            setParameters(statement, params);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getObject(1, type);
                }
                return null;
            }
        }
    }

    public <T> CompletableFuture<T> queryScalarAsync(String query, Class<T> type, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return queryScalar(query, type, params);
            } catch (SQLException exception) {

                logger.error("Error while executing async queryScalar", exception);
                throw new RuntimeException("Error while querying MySQL for scalar value", exception);
            }
        }, executor);
    }

    public void transaction(TransactionCallback callback) throws SQLException {
        Connection connection = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            callback.execute(connection);

            connection.commit();
        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                }
            }
            if (e instanceof SQLException) {
                throw (SQLException) e;
            } else {
                throw new SQLException("Transaction failed", e);
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException closeEx) {
                    logger.error("Failed to close connection after transaction", closeEx);
                }
            }
        }
    }

    public CompletableFuture<Void> transactionAsync(TransactionCallback callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                transaction(callback);
            } catch (SQLException exception) {
                logger.error("Error while executing async transaction", exception);
                throw new RuntimeException("Error while executing transaction", exception);
            }
        }, executor);
    }

    public long count(String table, String whereClause, Object... params) throws SQLException {
        String query = "SELECT COUNT(*) FROM " + table;
        if (whereClause != null && !whereClause.isEmpty()) {
            query += " WHERE " + whereClause;
        }

        return queryScalar(query, Long.class, params);
    }

    public CompletableFuture<Long> countAsync(String table, String whereClause, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return count(table, whereClause, params);
            } catch (SQLException exception) {
                logger.error( "Error while executing async count", exception);
                throw new RuntimeException("Error while counting rows in MySQL", exception);
            }
        }, executor);
    }

    public boolean exists(String table, String whereClause, Object... params) throws SQLException {
        return count(table, whereClause, params) > 0;
    }

    public CompletableFuture<Boolean> existsAsync(String table, String whereClause, Object... params) {
        return countAsync(table, whereClause, params).thenApply(count -> count > 0);
    }

    public CompletableFuture<Void> batchMultiAsync(Map<String, List<Object[]>> batches) {
        return CompletableFuture.runAsync(() -> {
            Connection connection = null;
            try {
                connection = getConnection();
                connection.setAutoCommit(false);

                for (Map.Entry<String, List<Object[]>> entry : batches.entrySet()) {
                    String sql = entry.getKey();
                    List<Object[]> paramsList = entry.getValue();
                    if (paramsList == null || paramsList.isEmpty()) continue;

                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        for (Object[] params : paramsList) {
                            setParameters(ps, params);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                connection.commit();
            } catch (SQLException ex) {
                if (connection != null) {
                    try { connection.rollback(); } catch (SQLException rbEx) {
                        logger.error("Failed to rollback batchMultiAsync transaction", rbEx);
                    }
                }
                logger.error("Error while executing batchMultiAsync", ex);
                throw new RuntimeException("Error while executing batchMultiAsync", ex);
            } finally {
                if (connection != null) {
                    try {
                        connection.setAutoCommit(true);
                        connection.close();
                    } catch (SQLException closeEx) {
                        logger.error("Failed to close connection after batchMultiAsync", closeEx);
                    }
                }
            }
        }, executor);
    }

    public HikariPoolStats getPoolStats() {
        if (dataSource == null) {
            throw new IllegalStateException("Connection pool has not been initialized!");
        }

        return new HikariPoolStats(
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }

    private void setParameters(PreparedStatement preparedStatement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            preparedStatement.setObject(i + 1, params[i]);
        }
    }
}
