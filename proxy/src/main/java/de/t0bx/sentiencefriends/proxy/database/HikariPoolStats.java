package de.t0bx.sentiencefriends.proxy.database;

public record HikariPoolStats(int activeConnections, int idleConnections, int totalConnections, int waitingThreads) {

    @Override
    public String toString() {
        return "HikariPoolStats{" +
                "activeConnections=" + activeConnections +
                ", idleConnections=" + idleConnections +
                ", totalConnections=" + totalConnections +
                ", waitingThreads=" + waitingThreads +
                '}';
    }
}
