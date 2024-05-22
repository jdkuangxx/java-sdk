package org.fisco.bcos.sdk.v3.filter;

import java.util.ArrayList;
import java.util.List;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.response.EthFilter;
import org.fisco.bcos.sdk.v3.client.protocol.response.EthLog;

/** Handler hashes for working with transaction filter requests. */
public class PendingTransactionsFilter extends Filter<List<String>> {

    public PendingTransactionsFilter(Client client, Callback<List<String>> callback) {
        super(client, callback, false);
    }

    @Override
    protected EthFilter sendRequest() {
        return client.newPendingTransactionFilterWithoutException();
    }

    @Override
    protected void process(List<EthLog.LogResult> logResults) {
        List<String> logs = new ArrayList<>(logResults.size());

        for (EthLog.LogResult logResult : logResults) {
            if (!(logResult instanceof EthLog.Hash)) {
                throw new FilterException(
                        "Unexpected result type: " + logResult.get() + ", required Hash");
            }

            logs.add(((EthLog.Hash) logResult).get());
        }

        callback.onEvent(logs);
    }
}
