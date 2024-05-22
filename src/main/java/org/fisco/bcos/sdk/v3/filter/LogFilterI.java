package org.fisco.bcos.sdk.v3.filter;

import java.math.BigInteger;
import java.util.List;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.exceptions.ClientException;
import org.fisco.bcos.sdk.v3.client.protocol.request.DefaultBlockParameterNumber;
import org.fisco.bcos.sdk.v3.client.protocol.response.EthFilter;
import org.fisco.bcos.sdk.v3.client.protocol.response.EthLog;

/** Log filter handler. */
public abstract class LogFilterI<T> extends Filter<T> {

    protected final org.fisco.bcos.sdk.v3.client.protocol.request.EthFilter params;

    private static BigInteger MAX_PROCESS_BLOCK = BigInteger.valueOf(100);
    private BigInteger lastProcessedBlock = BigInteger.ZERO;

    public LogFilterI(
            Client client,
            Callback<T> callback,
            org.fisco.bcos.sdk.v3.client.protocol.request.EthFilter params,
            boolean getHistoryLog) {
        super(client, callback, getHistoryLog);
        this.params = params;
    }

    @Override
    protected void getInitialFilterLogs() {
        // split [from, to] into multiple small intervals to avoid blockchain blocking
        BigInteger fromBlock = BigInteger.ZERO;
        BigInteger toBlock = BigInteger.ZERO;
        BigInteger latestBlock = BigInteger.ZERO;

        // fromIsLatest && toIsLatest
        if (params.getFromBlock().isLatest() && params.getToBlock().isLatest()) {
            return;
        }

        if (params.getFromBlock() instanceof DefaultBlockParameterNumber) {
            fromBlock = ((DefaultBlockParameterNumber) params.getFromBlock()).getBlockNumber();
        }

        if (params.getToBlock() instanceof DefaultBlockParameterNumber) {
            toBlock = ((DefaultBlockParameterNumber) params.getToBlock()).getBlockNumber();
        }

        try {
            latestBlock = client.getBlockNumber().getBlockNumber();
        } catch (ClientException e) {
            throwException(e);
        }

        // shallow copy of address and topics fields
        org.fisco.bcos.sdk.v3.client.protocol.request.EthFilter newParams =
                new org.fisco.bcos.sdk.v3.client.protocol.request.EthFilter(
                        params.getAddress(), params.getTopics());

        // avoid accessing future blocks to reduce sending useless requests
        toBlock = toBlock.min(latestBlock);
        fromBlock = fromBlock.max(lastProcessedBlock);
        for (BigInteger begin = fromBlock;
                begin.compareTo(toBlock) <= 0;
                begin = begin.add(MAX_PROCESS_BLOCK)) {
            BigInteger end = begin.add(MAX_PROCESS_BLOCK).min(toBlock);
            newParams.setFromBlock(begin).setToBlock(end);
            EthLog ethLog = client.getLogsWithoutException(newParams);
            if (ethLog.hasError()) {
                throwException(ethLog.getError());
            }
            processImpl(ethLog.getLogs());
        }

        // When the interval [from, to] is large, the above operations may be time-consuming.The
        // filter may have expired while processing the blocks (historical blocks) within [from,
        // to]. At this point, reinstallFilter will be performed in the pollFilter function.
        // However, a new block may have been generated between obtaining historical logs from the
        // last time and reinstallFilter. For newly installed filter, these blocks belong to
        // historical blocks and need to be reprocessed. Here, we use the variable
        // lastProcessedBlock to record the last processing position of historical blocks.
        lastProcessedBlock = toBlock;
    }

    @Override
    protected EthFilter sendRequest() {
        return client.newFilterWithoutException(params);
    }

    @Override
    protected void process(List<EthLog.LogResult> logResults) {
        // when the interface getFilterChange is successfully called and no "filter not found" error
        // occurs, reset lastProcessedBlock
        lastProcessedBlock = BigInteger.ZERO;
        processImpl(logResults);
    }

    protected abstract void processImpl(List<EthLog.LogResult> logResults);
}
