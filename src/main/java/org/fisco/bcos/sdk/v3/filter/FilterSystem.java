package org.fisco.bcos.sdk.v3.filter;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.request.EthFilter;
import org.fisco.bcos.sdk.v3.client.protocol.response.EthLog;
import org.fisco.bcos.sdk.v3.client.protocol.response.Log;

public class FilterSystem {
    private ScheduledExecutorService scheduledExecutorService;
    private Client client;
    private long pollingInterval = 1 * 1000;

    public FilterSystem(Client client, int poolSize, long pollingInterval) {
        this.client = client;
        this.scheduledExecutorService = Executors.newScheduledThreadPool(poolSize);
        this.pollingInterval = pollingInterval;
    }

    public FilterSystem(Client client, int poolSize) {
        this.client = client;
        this.scheduledExecutorService = Executors.newScheduledThreadPool(poolSize);
    }

    private <T> void run(
            Filter<T> filter, FlowableEmitter<? super T> emitter, long pollingInterval) {

        filter.run(scheduledExecutorService, pollingInterval);
        emitter.setCancellable(filter::cancel);
    }

    /**
     * Create an flowable to filter for specific log events on the blockchain.
     *
     * @param filter filter criteria
     * @param getHistoryLog need to obtain historical logs
     * @return a {@link Flowable} instance that emits all Log events matching the filter
     */
    public Flowable<Log> logFlowable(EthFilter filer, boolean getHistoryLog) {
        return Flowable.create(
                subscriber -> {
                    LogFilter logFilter =
                            new LogFilter(client, subscriber::onNext, filer, getHistoryLog);
                    run(logFilter, subscriber, pollingInterval);
                },
                BackpressureStrategy.BUFFER);
    }

    /**
     * Create an Flowable to emit block hashes.
     *
     * @return a {@link Flowable} instance that emits all new block hashes as new blocks are created
     *     on the blockchain
     */
    public Flowable<String> blockHashFlowable() {
        return Flowable.create(
                subscriber -> {
                    BlockFilter blockFilter = new BlockFilter(client, subscriber::onNext);
                    run(blockFilter, subscriber, pollingInterval);
                },
                BackpressureStrategy.BUFFER);
    }

    /**
     * Create an {@link Flowable} instance to emit all new transactions as they are confirmed on the
     * blockchain. i.e. they have been mined and are incorporated into a block.
     *
     * @return a {@link Flowable} instance to emit new transactions on the blockchain
     */
    public Flowable<String> transactionHashFlowable() {
        return Flowable.create(
                subscriber -> {
                    PendingTransactionFilter pendingTransactionFilter =
                            new PendingTransactionFilter(client, subscriber::onNext);

                    run(pendingTransactionFilter, subscriber, pollingInterval);
                },
                BackpressureStrategy.BUFFER);
    }

    public List<Log> getLogs(EthFilter filter) throws FilterException {
        EthLog ethLog = client.getLogsWithoutException(filter);
        if (ethLog.hasError()) {
            throw new FilterException(
                    "Invalid request: "
                            + (ethLog.getError() == null
                                    ? "Unknown Error"
                                    : ethLog.getError().getMessage()));
        }
        List<Log> logs = new ArrayList<>();
        for (EthLog.LogResult logResult : ethLog.getLogs()) {
            if (logResult instanceof EthLog.LogObject) {
                logs.add(((EthLog.LogObject) logResult).get());
            } else {
                throw new FilterException(
                        "Unexpected result type: " + logResult.get() + " required LogObject");
            }
        }
        return logs;
    }

    public void stop() {
        // Disable new tasks from being submitted
        scheduledExecutorService.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                // Cancel currently executing tasks
                scheduledExecutorService.shutdownNow();
                // Wait a while for tasks to respond to being cancelled
                if (!scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
