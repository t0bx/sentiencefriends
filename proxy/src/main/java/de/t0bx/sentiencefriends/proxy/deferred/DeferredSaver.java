package de.t0bx.sentiencefriends.proxy.deferred;

import de.t0bx.sentiencefriends.proxy.ProxyPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DeferredSaver implements AutoCloseable {

    private record SqlOp(String sql, Object[] params) { }

    private final ProxyPlugin plugin;
    private final ConcurrentLinkedQueue<SqlOp> queue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public DeferredSaver(ProxyPlugin plugin, long intervalMillis, String threadName) {
        this.plugin = plugin;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, threadName);
            t.setDaemon(true);
            return t;
        });
        this.scheduler.scheduleAtFixedRate(this::flushSafe, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    public void enqueue(String sql, Object... params) {
        if (!running.get()) return;
        queue.add(new SqlOp(sql, params));
    }

    public void flushNow() { flushInternal(); }

    private void flushSafe() {
        try { flushInternal(); }
        catch (Throwable t) {
            plugin.getLogger().warn("Flush failed: {}", t.getMessage());
        }
    }

    private void flushInternal() {
        if (queue.isEmpty()) return;

        List<SqlOp> drained = new ArrayList<>(Math.max(32, queue.size()));
        for (SqlOp op; (op = queue.poll()) != null; ) drained.add(op);
        if (drained.isEmpty()) return;

        Map<String, List<Object[]>> batches = new LinkedHashMap<>();
        for (SqlOp op : drained) {
            batches.computeIfAbsent(op.sql, k -> new ArrayList<>()).add(op.params);
        }

        plugin.getMySQLManager().batchMultiAsync(batches)
                .exceptionally(ex -> {
                    plugin.getLogger().warn("batchMultiAsync failed: {}", ex.getMessage());
                    return null;
                });
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) return;
        try { flushInternal(); }
        finally { scheduler.shutdownNow(); }
    }
}
