package org.fisco.bcos.sdk.v3.filter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.response.EthFilter;
import org.fisco.bcos.sdk.v3.client.protocol.response.EthLog;
import org.fisco.bcos.sdk.v3.client.protocol.response.EthUninstallFilter;
import org.fisco.bcos.sdk.v3.model.JsonRpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class for creating managed filter requests with callbacks. */
public abstract class Filter<T> {

    private static final Logger log = LoggerFactory.getLogger(Filter.class);

    protected final Client client;
    protected Callback<T> callback;

    protected volatile EthFilter filter;

    protected ScheduledFuture<?> schedule;

    protected ScheduledFuture<?> firstSchedule;

    protected ScheduledExecutorService scheduledExecutorService;

    protected long blockTime;

    protected boolean getHistoryLog;

    private static final String FILTER_NOT_FOUND_PATTERN = "(?i)\\bfilter\\s+not\\s+found\\b";

    public Filter(Client client, Callback<T> callback, boolean getHistoryLog) {
        this.client = client;
        this.callback = callback;
        this.getHistoryLog = getHistoryLog;
    }

    public void run(ScheduledExecutorService scheduledExecutorService, long blockTime) {
        EthFilter ethFilter = sendRequest();
        if (ethFilter.hasError()) {
            throwException(ethFilter.getError());
        }
        filter = ethFilter;
        this.scheduledExecutorService = scheduledExecutorService;
        this.blockTime = blockTime;
        Runnable task =
                () -> {
                    run(false);
                };
        firstSchedule = scheduledExecutorService.schedule(task, 0, TimeUnit.MILLISECONDS);
    }

    private void run(boolean installFilter) {
        if (installFilter) {
            EthFilter ethFilter = sendRequest();
            if (ethFilter.hasError()) {
                throwException(ethFilter.getError());
            }
            filter = ethFilter;
        }
        if (getHistoryLog) {
            getInitialFilterLogs();
        }
        // wait for the completion of obtaining historical logs before starting to obtain new logs
        schedule =
                scheduledExecutorService.scheduleAtFixedRate(
                        () -> {
                            try {
                                this.pollFilter(this.filter);
                            } catch (Throwable e) {
                                // All exceptions must be caught, otherwise our job terminates
                                // without any notification
                                log.warn("Error sending request", e);
                            }
                        },
                        0,
                        blockTime,
                        TimeUnit.MILLISECONDS);
    }

    // default implement
    protected void getInitialFilterLogs() {}

    private void pollFilter(EthFilter ethFilter) {
        EthLog ethLog = null;
        ethLog = client.getFilterChangesWithoutException(ethFilter);
        if (ethLog.hasError()) {
            JsonRpcResponse.Error error = ethLog.getError();
            String message = error.getMessage();
            if (Pattern.compile(FILTER_NOT_FOUND_PATTERN).matcher(message).find()) {
                reinstallFilter();
            } else {
                throwException(error);
            }
        } else {
            process(ethLog.getLogs());
        }
    }

    protected abstract EthFilter sendRequest();

    protected abstract void process(List<EthLog.LogResult> logResults);

    private void reinstallFilter() {
        log.warn(
                "Previously installed filter has not been found, trying to re-install. Filter id: {}",
                filter.getFilterId());
        schedule.cancel(false);
        this.run(true);
    }

    public void cancel() {
        firstSchedule.cancel(false);
        schedule.cancel(false);
        try {
            EthUninstallFilter ethUninstallFilter = uninstallFilter(filter);
            if (ethUninstallFilter.hasError()) {
                throwException(ethUninstallFilter.getError());
            }

            if (!ethUninstallFilter.isUninstalled()) {
                throw new FilterException(
                        "Filter with id '" + filter.getFilterId() + "' failed to uninstall");
            }
        } catch (IOException e) {
            throwException(e);
        }
    }

    protected EthUninstallFilter uninstallFilter(EthFilter filterId) throws IOException {
        return client.uninstallFilterWithoutException(filterId);
    }

    void throwException(JsonRpcResponse.Error error) {
        throw new FilterException(
                "Invalid request: " + (error == null ? "Unknown Error" : error.getMessage()));
    }

    void throwException(Throwable cause) {
        throw new FilterException("Error sending request", cause);
    }
}
