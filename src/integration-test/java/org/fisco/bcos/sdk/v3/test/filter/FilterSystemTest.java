package org.fisco.bcos.sdk.v3.test.filter;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import org.fisco.bcos.sdk.jni.common.JniException;
import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.request.DefaultBlockParameter;
import org.fisco.bcos.sdk.v3.client.protocol.request.DefaultBlockParameterName;
import org.fisco.bcos.sdk.v3.client.protocol.request.EthFilter;
import org.fisco.bcos.sdk.v3.client.protocol.response.Log;
import org.fisco.bcos.sdk.v3.codec.EventEncoder;
import org.fisco.bcos.sdk.v3.config.exceptions.ConfigException;
import org.fisco.bcos.sdk.v3.filter.FilterException;
import org.fisco.bcos.sdk.v3.filter.FilterSystem;
import org.fisco.bcos.sdk.v3.model.ConstantConfig;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.test.contract.solidity.EventSubDemo;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FilterSystemTest {
    private static final String configFile =
            FilterSystemTest.class
                    .getClassLoader()
                    .getResource(ConstantConfig.CONFIG_FILE_NAME)
                    .getPath();
    private static final String GROUP = "group0";
    private static final String INVALID_BLOCK_RANGE =
            "(?i)\\binvalid\\s+block\\s+range\\s+params\\b";
    private static final String EXCEED_MAX_TOPICS = "(?i)\\bexceed\\s+max\\s+topics\\b";

    private static void checkLogEntry(
            Client client,
            TransactionReceipt receipt,
            String txHash,
            String blockHash,
            Log log,
            String address) {

        Assert.assertEquals(receipt.getStatus(), 0);
        Assert.assertEquals(receipt.getTransactionHash(), txHash);
        String hash = client.getBlockHashByNumber(receipt.getBlockNumber()).getBlockHashByNumber();
        Assert.assertEquals(blockHash, hash);
        List<TransactionReceipt.Logs> txLogs = receipt.getLogEntries();
        int logIndex = log.getLogIndex().intValue();
        Assert.assertTrue(logIndex < txLogs.size());
        TransactionReceipt.Logs matched = txLogs.get(logIndex);
        Assert.assertEquals(log.getBlockHash(), blockHash);
        Assert.assertEquals(log.getTransactionHash(), txHash);
        Assert.assertEquals(matched.getData(), log.getData());
        Assert.assertEquals(matched.getTopics(), log.getTopics());
        Assert.assertEquals(address, log.getAddress());
    }

    @Test
    public void normalTest()
            throws FilterException, ContractException, ConfigException, JniException,
                    InterruptedException {
        BcosSDK sdk = BcosSDK.build(configFile);
        Client client = sdk.getClient(GROUP);
        FilterSystem filterSystem = sdk.getFilterSystem(client, 1, 1000);
        EventSubDemo eventSubDemo =
                EventSubDemo.deploy(client, client.getCryptoSuite().getCryptoKeyPair());
        EventEncoder encoder = new EventEncoder(client.getCryptoSuite().getHashImpl());

        List<String> blockHashes = new ArrayList<>();
        List<String> txHashes = new ArrayList<>();
        List<Log> logs = new ArrayList<>();
        List<Log> logs1 = new ArrayList<>();
        List<Log> logs2 = new ArrayList<>();
        List<Log> logs3 = new ArrayList<>();

        Disposable blockSub =
                filterSystem
                        .blockHashFlowable()
                        .subscribe(
                                block -> {
                                    blockHashes.add(block);
                                });

        Disposable txSub =
                filterSystem
                        .transactionHashFlowable()
                        .subscribe(
                                tx -> {
                                    txHashes.add(tx);
                                });

        EthFilter ethFilter =
                new EthFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST)
                        .addSingleTopic(encoder.encode(EventSubDemo.TRANSFERACCOUNT_EVENT));

        Disposable logSub =
                filterSystem
                        .logFlowable(ethFilter, false)
                        .subscribe(
                                log -> {
                                    logs.add(log);
                                });

        EthFilter ethFilter1 =
                new EthFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST)
                        .addSingleTopic(encoder.encode(EventSubDemo.TRANSFERACCOUNT_EVENT))
                        .addSingleTopic(encoder.buildEventSignature("test1"))
                        .addSingleTopic(encoder.buildEventSignature("test2"));

        Disposable logSub1 =
                filterSystem
                        .logFlowable(ethFilter1, false)
                        .subscribe(
                                log -> {
                                    logs1.add(log);
                                });

        EthFilter ethFilter2 =
                new EthFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST)
                        .addNullTopic()
                        .addSingleTopic(encoder.buildEventSignature("test1"))
                        .addNullTopic();

        Disposable logSub2 =
                filterSystem
                        .logFlowable(ethFilter2, false)
                        .subscribe(
                                log -> {
                                    logs2.add(log);
                                });

        EthFilter ethFilter3 =
                new EthFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST)
                        .addNullTopic()
                        .addOptionalTopics(encoder.buildEventSignature("test1"), encoder.buildEventSignature("test2"))
                        .addNullTopic();

        Disposable logSub3 =
                filterSystem
                        .logFlowable(ethFilter3, false)
                        .subscribe(
                                log -> {
                                    logs3.add(log);
                                });

        List<TransactionReceipt> receipts = new ArrayList<>();
        receipts.add(eventSubDemo.transfer("test1", "test2", BigInteger.valueOf(100)));
        receipts.add(eventSubDemo.transfer("test1", "test4", BigInteger.valueOf(100)));
        receipts.add(eventSubDemo.transfer("test2", "test6", BigInteger.valueOf(100)));

        Thread.sleep(2 * 1000);

        Assert.assertTrue(receipts.size() == txHashes.size());
        Assert.assertTrue(receipts.size() == blockHashes.size());
        Assert.assertTrue(receipts.size() == logs.size());

        for (int i = 0; i < logs.size(); i++) {
            checkLogEntry(
                    client,
                    receipts.get(i),
                    txHashes.get(i),
                    blockHashes.get(i),
                    logs.get(i),
                    eventSubDemo.getContractAddress());
        }

        Assert.assertTrue("logs1.size = " + logs1.size(),1 == logs1.size());
        for (int i = 0; i < logs1.size(); i++) {
            checkLogEntry(
                    client,
                    receipts.get(i),
                    txHashes.get(i),
                    blockHashes.get(i),
                    logs1.get(i),
                    eventSubDemo.getContractAddress());
        }

        Assert.assertTrue("logs2.size = " + logs2.size(),2 == logs2.size());
        for (int i = 0; i < logs2.size(); i++) {
            checkLogEntry(
                    client,
                    receipts.get(i),
                    txHashes.get(i),
                    blockHashes.get(i),
                    logs2.get(i),
                    eventSubDemo.getContractAddress());
        }

        Assert.assertTrue("logs3.size = " + logs3.size(),3 == logs3.size());
        for (int i = 0; i < logs3.size(); i++) {
            checkLogEntry(
                    client,
                    receipts.get(i),
                    txHashes.get(i),
                    blockHashes.get(i),
                    logs3.get(i),
                    eventSubDemo.getContractAddress());
        }

        blockSub.dispose();
        txSub.dispose();
        logSub.dispose();
        logSub1.dispose();
        logSub2.dispose();
        logSub3.dispose();
        filterSystem.stop();
    }

    @Test
    public void failedTest()
            throws FilterException, ContractException, ConfigException, JniException,
                    InterruptedException {
        BcosSDK sdk = BcosSDK.build(configFile);
        Client client = sdk.getClient(GROUP);
        FilterSystem filterSystem = sdk.getFilterSystem(client, 1, 1000);
        EventSubDemo eventSubDemo =
                EventSubDemo.deploy(client, client.getCryptoSuite().getCryptoKeyPair());
        EventEncoder encoder = new EventEncoder(client.getCryptoSuite().getHashImpl());

        List<Log> logs1 = new ArrayList<>();
        List<Log> logs2 = new ArrayList<>();
        List<Log> logs3 = new ArrayList<>();

        EthFilter ethFilter1 =
                new EthFilter(DefaultBlockParameter.valueOf(4), DefaultBlockParameter.valueOf(3))
                        .addSingleTopic(encoder.encode(EventSubDemo.TRANSFERACCOUNT_EVENT));

        EthFilter ethFilter2 =
                new EthFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameter.valueOf(3))
                        .addSingleTopic(encoder.encode(EventSubDemo.TRANSFERACCOUNT_EVENT));

        EthFilter ethFilter3 =
                new EthFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameter.valueOf(3))
                        .addSingleTopic(encoder.encode(EventSubDemo.TRANSFERACCOUNT_EVENT))
                        .addSingleTopic(encoder.encode(EventSubDemo.ECHOBYTES_EVENT))
                        .addSingleTopic(encoder.encode(EventSubDemo.TRANSFERAMOUNT_EVENT))
                        .addSingleTopic(encoder.encode(EventSubDemo.TRANSFER_EVENT))
                        .addSingleTopic(encoder.encode(EventSubDemo.TRANSFERDATA_EVENT));

        Disposable logSub1 =
                filterSystem
                        .logFlowable(ethFilter1, false)
                        .doOnError(
                                e -> {
                                    Assert.assertTrue(
                                            Pattern.compile(INVALID_BLOCK_RANGE)
                                                    .matcher(e.getMessage())
                                                    .find());
                                })
                        .onErrorResumeNext(Flowable.empty())
                        .subscribe(
                                log -> {
                                    logs1.add(log);
                                });

        Disposable logSub2 =
                filterSystem
                        .logFlowable(ethFilter2, false)
                        .doOnError(
                                e -> {
                                    Assert.assertTrue(
                                            Pattern.compile(INVALID_BLOCK_RANGE)
                                                    .matcher(e.getMessage())
                                                    .find());
                                })
                        .onErrorResumeNext(Flowable.empty())
                        .subscribe(
                                log -> {
                                    logs2.add(log);
                                });

        Disposable logSub3 =
                filterSystem
                        .logFlowable(ethFilter3, false)
                        .doOnError(
                                e -> {
                                    Assert.assertTrue(
                                            Pattern.compile(EXCEED_MAX_TOPICS)
                                                    .matcher(e.getMessage())
                                                    .find());
                                })
                        .onErrorResumeNext(Flowable.empty())
                        .subscribe(
                                log -> {
                                    logs3.add(log);
                                });

        List<TransactionReceipt> receipts = new ArrayList<>();
        receipts.add(eventSubDemo.transfer("test1", "test2", BigInteger.valueOf(100)));
        receipts.add(eventSubDemo.transfer("test3", "test4", BigInteger.valueOf(100)));
        receipts.add(eventSubDemo.transfer("test5", "test6", BigInteger.valueOf(100)));

        Thread.sleep(2 * 1000);

        Assert.assertTrue("logs1.size = " + logs1.size(), logs1.size() == 0);
        Assert.assertTrue("logs2.size = " + logs2.size(), logs2.size() == 0);
        Assert.assertTrue("logs3.size = " + logs3.size(), logs3.size() == 0);

        Assert.assertTrue(logSub1.isDisposed());
        Assert.assertTrue(logSub2.isDisposed());
        Assert.assertTrue(logSub3.isDisposed());
        filterSystem.stop();
    }
}
