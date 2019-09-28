package io.seata.benchmark.rpc.netty;

import java.io.Closeable;

/**
 * @author ggndnn
 */
abstract class BaseNettyClientChannelManager implements INettyClientChannelManager, Closeable {
    RpcClientMock rpcRemotingClient = new RpcClientMock();
}
