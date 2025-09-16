package de.t0bx.sentiencefriends.proxy.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public interface IMySQLManager {

    /**
     * Retrieves the executor service used for managing asynchronous tasks.
     *
     * @return the ExecutorService instance.
     */
    ExecutorService getExecutor();

    /**
     * Establishes a connection to the MySQL database.
     *
     * @throws SQLException if an error occurs while establishing the connection
     */
    void connect() throws SQLException;

    /**
     * Closes the connection to the MySQL database.
     *
     * @throws SQLException if an error occurs while closing the connection
     */
    void disconnect() throws SQLException;

    /**
     * Executes an update operation (INSERT, UPDATE, DELETE).
     *
     * @param query SQL query
     * @param params parameters for the query
     * @throws SQLException if an error occurs during execution
     */
    void update(String query, Object... params) throws SQLException;

    /**
     * Executes an update operation asynchronously.
     *
     * @param query SQL query
     * @param params parameters for the query
     * @return CompletableFuture that is completed when the operation finishes
     */
    CompletableFuture<Void> updateAsync(String query, Object... params);

    /**
     * Executes a query that returns results.
     *
     * @param query SQL query
     * @param resultHandler function to convert the ResultSet into objects of type T
     * @param params parameters for the query
     * @param <T> type of the returned objects
     * @return list of objects of type T
     * @throws SQLException if an error occurs during execution
     */
    <T> List<T> query(String query, Function<ResultSet, T> resultHandler, Object... params) throws SQLException;

    /**
     * Executes a query asynchronously that returns results.
     *
     * @param query SQL query
     * @param resultHandler function to convert the ResultSet into objects of type T
     * @param params parameters for the query
     * @param <T> type of the returned objects
     * @return CompletableFuture that is completed with the result list when the operation finishes
     */
    <T> CompletableFuture<List<T>> queryAsync(String query, Function<ResultSet, T> resultHandler, Object... params);

    /**
     * Executes a batch update operation.
     *
     * @param query SQL query
     * @param batchParams list of parameter arrays for the batch operation
     * @return array of update counts
     * @throws SQLException if an error occurs during execution
     */
    int[] batchUpdate(String query, List<Object[]> batchParams) throws SQLException;

    /**
     * Executes a batch update operation asynchronously.
     *
     * @param query SQL query
     * @param batchParams list of parameter arrays for the batch operation
     * @return CompletableFuture that is completed with the update counts when the operation finishes
     */
    CompletableFuture<int[]> batchUpdateAsync(String query, List<Object[]> batchParams);

    /**
     * Executes a query and returns the first result, if available.
     *
     * @param query SQL query
     * @param resultHandler function to convert the ResultSet into an object of type T
     * @param params parameters for the query
     * @param <T> type of the returned object
     * @return Optional containing the result, or an empty Optional if no result was found
     * @throws SQLException if an error occurs during execution
     */
    <T> Optional<T> queryFirst(String query, Function<ResultSet, T> resultHandler, Object... params) throws SQLException;

    /**
     * Executes a query asynchronously and returns the first result, if available.
     *
     * @param query SQL query
     * @param resultHandler function to convert the ResultSet into an object of type T
     * @param params parameters for the query
     * @param <T> type of the returned object
     * @return CompletableFuture that is completed with the result when the operation finishes
     */
    <T> CompletableFuture<Optional<T>> queryFirstAsync(String query, Function<ResultSet, T> resultHandler, Object... params);

    /**
     * Executes a scalar query that returns a single value.
     *
     * @param query SQL query
     * @param type class of the returned type
     * @param params parameters for the query
     * @param <T> type of the returned value
     * @return scalar value of type T, or null if no result was found
     * @throws SQLException if an error occurs during execution
     */
    <T> T queryScalar(String query, Class<T> type, Object... params) throws SQLException;

    /**
     * Executes a scalar query asynchronously that returns a single value.
     *
     * @param query SQL query
     * @param type class of the returned type
     * @param params parameters for the query
     * @param <T> type of the returned value
     * @return CompletableFuture that is completed with the scalar value when the operation finishes
     */
    <T> CompletableFuture<T> queryScalarAsync(String query, Class<T> type, Object... params);

    /**
     * Executes multiple queries in a transaction.
     *
     * @param callback callback function that performs operations with the connection
     * @throws SQLException if an error occurs during execution
     */
    void transaction(TransactionCallback callback) throws SQLException;

    /**
     * Executes multiple queries asynchronously in a transaction.
     *
     * @param callback callback function that performs operations with the connection
     * @return CompletableFuture that is completed when the transaction finishes
     */
    CompletableFuture<Void> transactionAsync(TransactionCallback callback);

    /**
     * Counts the number of records in a table that match a condition.
     *
     * @param table table name
     * @param whereClause WHERE clause (without the word "WHERE"), may be null
     * @param params parameters for the WHERE clause
     * @return number of matching records
     * @throws SQLException if an error occurs during execution
     */
    long count(String table, String whereClause, Object... params) throws SQLException;

    /**
     * Asynchronously counts the number of records in a table that match a condition.
     *
     * @param table table name
     * @param whereClause WHERE clause (without the word "WHERE"), may be null
     * @param params parameters for the WHERE clause
     * @return CompletableFuture that is completed with the count when the operation finishes
     */
    CompletableFuture<Long> countAsync(String table, String whereClause, Object... params);

    /**
     * Checks whether a record exists that matches a condition.
     *
     * @param table table name
     * @param whereClause WHERE clause (without the word "WHERE")
     * @param params parameters for the WHERE clause
     * @return true if a record exists, false otherwise
     * @throws SQLException if an error occurs during execution
     */
    boolean exists(String table, String whereClause, Object... params) throws SQLException;

    /**
     * Asynchronously checks whether a record exists that matches a condition.
     *
     * @param table table name
     * @param whereClause WHERE clause (without the word "WHERE")
     * @param params parameters for the WHERE clause
     * @return CompletableFuture that is completed with the result when the operation finishes
     */
    CompletableFuture<Boolean> existsAsync(String table, String whereClause, Object... params);

    /**
     * Returns the current statistics of the connection pool.
     *
     * @return HikariPoolStats object containing the pool statistics
     */
    HikariPoolStats getPoolStats();

    /**
     * Retrieves a connection from the pool.
     *
     * @return a database connection
     * @throws SQLException if an error occurs while retrieving the connection
     */
    Connection getConnection() throws SQLException;

    /**
     * Executes multiple batch update operations asynchronously. Each entry in the provided map
     * represents a set of SQL queries to be executed in batch for the corresponding key.
     *
     * @param batches a map where the key is the SQL query string, and the value is a list of parameter arrays
     *                to be used for the batch update operations.
     * @return a CompletableFuture that completes when all batch operations have been processed.
     */
    CompletableFuture<Void> batchMultiAsync(Map<String, List<Object[]>> batches);

    /**
     * Interface for transaction callbacks.
     */
    interface TransactionCallback {

        /**
         * Executed within a transaction.
         *
         * @param connection the database connection for the transaction
         * @throws SQLException if an error occurs during execution
         */
        void execute(Connection connection) throws SQLException;
    }
}
