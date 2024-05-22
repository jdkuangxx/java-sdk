package org.fisco.bcos.sdk.v3.filter;

import java.util.ArrayList;
import java.util.List;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.response.EthLog;
import org.fisco.bcos.sdk.v3.client.protocol.response.Log;

/** Logs filter handler. */
public class LogsFilter extends LogFilterI<List<Log>> {

    public LogsFilter(
            Client client,
            Callback<List<Log>> callback,
            org.fisco.bcos.sdk.v3.client.protocol.request.EthFilter params,
            boolean getHistoryLog) {
        super(client, callback, params, getHistoryLog);
    }

    @Override
    protected void processImpl(List<EthLog.LogResult> logResults) {
        List<Log> logs = new ArrayList<>(logResults.size());

        for (EthLog.LogResult logResult : logResults) {
            if (!(logResult instanceof EthLog.LogObject)) {
                throw new FilterException(
                        "Unexpected result type: " + logResult.get() + " required LogObject");
            }
            logs.add(((EthLog.LogObject) logResult).get());
        }
        callback.onEvent(logs);
    }
}
