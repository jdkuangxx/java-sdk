package org.fisco.bcos.sdk.v3.filter;

import java.util.ArrayList;
import java.util.List;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.response.EthFilter;
import org.fisco.bcos.sdk.v3.client.protocol.response.EthLog;

/** Handler hashes for working with block filter requests */
public class BlocksFilter extends Filter<List<String>> {

    public BlocksFilter(Client client, Callback<List<String>> callback) {
        super(client, callback, false);
    }

    @Override
    protected EthFilter sendRequest() {
        return client.newBlockFilterWithoutException();
    }

    @Override
    protected void process(List<EthLog.LogResult> logResults) {
        List<String> blockHashes = new ArrayList<>(logResults.size());

        for (EthLog.LogResult logResult : logResults) {
            if (!(logResult instanceof EthLog.Hash)) {
                throw new FilterException(
                        "Unexpected result type: " + logResult.get() + ", required Hash");
            }

            blockHashes.add(((EthLog.Hash) logResult).get());
        }

        callback.onEvent(blockHashes);
    }
}
