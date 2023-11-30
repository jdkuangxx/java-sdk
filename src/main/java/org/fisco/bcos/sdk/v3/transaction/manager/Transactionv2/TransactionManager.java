package org.fisco.bcos.sdk.v3.transaction.manager.Transactionv2;

import java.math.BigInteger;
import org.fisco.bcos.sdk.jni.common.JniException;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.response.Call;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.model.callback.RespCallback;
import org.fisco.bcos.sdk.v3.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.v3.transaction.gasProvider.ContractGasProvider;
import org.fisco.bcos.sdk.v3.transaction.gasProvider.EIP1559Struct;

public abstract class TransactionManager {

    protected Client client;

    public Client getClient() {
        return client;
    }

    protected TransactionManager(Client client) {
        this.client = client;
    }

    protected abstract ContractGasProvider getGasProvider();

    protected abstract void steGasProvider(ContractGasProvider gasProvider);

    /**
     * Simple send tx
     *
     * @param to to address
     * @param data input data
     * @param value transfer value
     * @return receipt
     */
    protected TransactionReceipt sendTransaction(String to, String data, BigInteger value)
            throws JniException {
        return sendTransaction(to, data, value, "", false);
    }

    /**
     * Send tx with abi field
     *
     * @param to to address
     * @param data input data
     * @param abi ABI JSON string, generated by compile contract, should fill in when you deploy
     *     contract
     * @param value transfer value
     * @param constructor if you deploy contract, should set to be true
     * @return receipt
     */
    protected abstract TransactionReceipt sendTransaction(
            String to, String data, BigInteger value, String abi, boolean constructor)
            throws JniException;

    /**
     * Send tx with gasPrice and gasLimit fields
     *
     * @param to to address
     * @param data input data
     * @param abi ABI JSON string, generated by compile contract, should fill in when you deploy
     *     contract
     * @param value transfer value
     * @param gasPrice price of gas
     * @param gasLimit use limit of gas
     * @param constructor if you deploy contract, should set to be true
     * @return receipt
     */
    protected abstract TransactionReceipt sendTransaction(
            String to,
            String data,
            BigInteger value,
            BigInteger gasPrice,
            BigInteger gasLimit,
            String abi,
            boolean constructor)
            throws JniException;

    /**
     * Send tx with gasPrice and gasLimit fields
     *
     * @param to to address
     * @param data input data
     * @param value transfer value
     * @param gasPrice price of gas
     * @param gasLimit use limit of gas
     * @param blockLimit block limit
     * @param abi ABI JSON string, generated by compile contract, should fill in when you deploy
     *     contract
     * @param constructor if you deploy contract, should set to be true
     * @return receipt
     */
    protected abstract TransactionReceipt sendTransaction(
            String to,
            String data,
            BigInteger value,
            BigInteger gasPrice,
            BigInteger gasLimit,
            BigInteger blockLimit,
            String abi,
            boolean constructor)
            throws JniException;

    /**
     * Simple send tx asynchronously
     *
     * @param to to address
     * @param data input data
     * @param value transfer value
     * @return receipt
     */
    protected String asyncSendTransaction(
            String to, String data, BigInteger value, TransactionCallback callback)
            throws JniException {
        return asyncSendTransaction(to, data, value, "", false, callback);
    }

    /**
     * Send tx with abi field asynchronously
     *
     * @param to to address
     * @param data input data
     * @param abi ABI JSON string, generated by compile contract, should fill in when you deploy
     *     contract
     * @param value transfer value
     * @param constructor if you deploy contract, should set to be true
     * @return receipt
     */
    protected abstract String asyncSendTransaction(
            String to,
            String data,
            BigInteger value,
            String abi,
            boolean constructor,
            TransactionCallback callback)
            throws JniException;

    /**
     * Send tx with gasPrice and gasLimit fields asynchronously
     *
     * @param to to address
     * @param data input data
     * @param abi ABI JSON string, generated by compile contract, should fill in when you deploy
     *     contract
     * @param value transfer value
     * @param gasPrice price of gas
     * @param gasLimit use limit of gas
     * @param constructor if you deploy contract, should set to be true
     * @return receipt
     */
    protected abstract String asyncSendTransaction(
            String to,
            String data,
            BigInteger value,
            BigInteger gasPrice,
            BigInteger gasLimit,
            String abi,
            boolean constructor,
            TransactionCallback callback)
            throws JniException;

    /**
     * Send tx with gasPrice and gasLimit fields asynchronously
     *
     * @param to to address
     * @param data input data
     * @param value transfer value
     * @param gasPrice price of gas
     * @param gasLimit use limit of gas
     * @param blockLimit block limit
     * @param abi ABI JSON string, generated by compile contract, should fill in when you deploy
     *     contract
     * @param constructor if you deploy contract, should set to be true
     * @param callback callback function
     * @return receipt
     */
    protected abstract String asyncSendTransaction(
            String to,
            String data,
            BigInteger value,
            BigInteger gasPrice,
            BigInteger gasLimit,
            BigInteger blockLimit,
            String abi,
            boolean constructor,
            TransactionCallback callback)
            throws JniException;

    /**
     * Send tx with EIP1559
     *
     * @param to to address
     * @param data input data
     * @param value transfer value
     * @param eip1559Struct EIP1559 transaction payload
     * @return receipt
     */
    protected TransactionReceipt sendTransactionEIP1559(
            String to, String data, BigInteger value, EIP1559Struct eip1559Struct)
            throws JniException {
        return sendTransactionEIP1559(to, data, value, eip1559Struct, "", false);
    }

    /**
     * Send tx with EIP1559
     *
     * @param to to address
     * @param data input data
     * @param abi ABI JSON string, generated by compile contract, should fill in when you deploy
     *     contract
     * @param value transfer value
     * @param eip1559Struct EIP1559 transaction payload
     * @param constructor if you deploy contract, should set to be true
     * @return receipt
     */
    protected abstract TransactionReceipt sendTransactionEIP1559(
            String to,
            String data,
            BigInteger value,
            EIP1559Struct eip1559Struct,
            String abi,
            boolean constructor)
            throws JniException;

    /**
     * Send tx with EIP1559
     *
     * @param to to address
     * @param data input data
     * @param value transfer value
     * @param eip1559Struct EIP1559 transaction payload
     * @param blockLimit block limit
     * @param abi ABI JSON string, generated by compile contract, should fill in when you deploy
     * @param constructor if you deploy contract, should set to be true
     * @return receipt
     */
    protected abstract TransactionReceipt sendTransactionEIP1559(
            String to,
            String data,
            BigInteger value,
            EIP1559Struct eip1559Struct,
            BigInteger blockLimit,
            String abi,
            boolean constructor)
            throws JniException;

    /**
     * Send tx with EIP1559 asynchronously
     *
     * @param to to address
     * @param data input data
     * @param value transfer value
     * @param eip1559Struct EIP1559 transaction payload
     * @param callback callback function
     * @return receipt
     */
    protected String asyncSendTransactionEIP1559(
            String to,
            String data,
            BigInteger value,
            EIP1559Struct eip1559Struct,
            TransactionCallback callback)
            throws JniException {
        return asyncSendTransactionEIP1559(to, data, value, eip1559Struct, "", false, callback);
    }

    /**
     * Send tx with EIP1559 asynchronously
     *
     * @param to to address
     * @param data input data
     * @param abi ABI JSON string, generated by compile contract, should fill in when you deploy
     *     contract
     * @param value transfer value
     * @param eip1559Struct EIP1559 transaction payload
     * @param constructor if you deploy contract, should set to be true
     * @param callback callback function
     * @return receipt
     */
    protected abstract String asyncSendTransactionEIP1559(
            String to,
            String data,
            BigInteger value,
            EIP1559Struct eip1559Struct,
            String abi,
            boolean constructor,
            TransactionCallback callback)
            throws JniException;

    /**
     * Send tx with EIP1559 asynchronously
     *
     * @param to to address
     * @param data input data
     * @param value transfer value
     * @param eip1559Struct EIP1559 transaction payload
     * @param blockLimit block limit
     * @param abi ABI JSON string, generated by compile contract, should fill in when you deploy
     * @param constructor if you deploy contract, should set to be true
     * @param callback callback function
     * @return receipt
     */
    protected abstract String asyncSendTransactionEIP1559(
            String to,
            String data,
            BigInteger value,
            EIP1559Struct eip1559Struct,
            BigInteger blockLimit,
            String abi,
            boolean constructor,
            TransactionCallback callback)
            throws JniException;

    /**
     * Send call
     *
     * @param to to address
     * @param data input data
     * @return call result
     */
    protected abstract Call sendCall(String to, String data);

    /**
     * Send call with signature of call data
     *
     * @param to to address
     * @param data input data
     * @param signature signature of call data
     */
    protected abstract Call sendCall(String to, String data, String signature);

    /**
     * Send call asynchronously
     *
     * @param to to address
     * @param data input data
     * @param callback callback function
     */
    protected abstract void asyncSendCall(String to, String data, RespCallback<Call> callback);

    /**
     * Send call asynchronously with signature of call data
     *
     * @param to to address
     * @param data input data
     * @param signature signature of call data
     * @param callback callback function
     */
    protected abstract void asyncSendCall(
            String to, String data, String signature, RespCallback<Call> callback);
}
