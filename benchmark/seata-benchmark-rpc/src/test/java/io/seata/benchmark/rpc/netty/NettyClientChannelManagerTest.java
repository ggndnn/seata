package io.seata.benchmark.rpc.netty;

import io.netty.channel.Channel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NettyClientChannelManagerTest {
    @Test
    public void testCreation() {
        INettyClientChannelManager clientChannelManager = testPoolCreation(RpcPoolType.POOL_TYPE_AP_1_LOCK);
        Assertions.assertEquals(NettyClientChannelManagerAp1Lock.class, clientChannelManager.getClass());
        clientChannelManager = testPoolCreation(RpcPoolType.POOL_TYPE_AP_1_LOCK_CAS);
        Assertions.assertEquals(NettyClientChannelManagerAp1LockCas.class, clientChannelManager.getClass());
        clientChannelManager = testPoolCreation(RpcPoolType.POOL_TYPE_AP_2_LOCK_CAS);
        Assertions.assertEquals(NettyClientChannelManagerAp2LockCas.class, clientChannelManager.getClass());
        clientChannelManager = testPoolCreation(RpcPoolType.POOL_TYPE_AP_1_NO_LOCK);
        Assertions.assertEquals(NettyClientChannelManagerAp1NoLock.class, clientChannelManager.getClass());
        clientChannelManager = testPoolCreation(RpcPoolType.POOL_TYPE_AP_2_LOCK);
        Assertions.assertEquals(NettyClientChannelManagerAp2Lock.class, clientChannelManager.getClass());
        clientChannelManager = testPoolCreation(RpcPoolType.POOL_TYPE_AP_2_NO_LOCK);
        Assertions.assertEquals(NettyClientChannelManagerAp2NoLock.class, clientChannelManager.getClass());
    }

    private INettyClientChannelManager testPoolCreation(String poolType) {
        String serverAddress = "10.0.0.0";
        INettyClientChannelManager clientChannelManager = RpcPoolBench.createClientChannelManager(poolType);
        Assertions.assertNotNull(clientChannelManager);
        Channel channel = clientChannelManager.acquireChannel(serverAddress);
        Assertions.assertNotNull(channel);
        clientChannelManager.releaseChannel(channel, serverAddress);
        Channel channel2 = clientChannelManager.acquireChannel(serverAddress);
        Assertions.assertNotNull(channel2);
        Assertions.assertEquals(channel, channel2);
        clientChannelManager.releaseChannel(channel2, serverAddress);
        channel = clientChannelManager.acquireChannel(serverAddress);
        clientChannelManager.destroyChannel(serverAddress, channel);
        channel2 = clientChannelManager.acquireChannel(serverAddress);
        Assertions.assertNotEquals(channel, channel2);
        clientChannelManager.releaseChannel(channel2, serverAddress);
        clientChannelManager.releaseChannel(channel, serverAddress);
        return clientChannelManager;
    }
}
