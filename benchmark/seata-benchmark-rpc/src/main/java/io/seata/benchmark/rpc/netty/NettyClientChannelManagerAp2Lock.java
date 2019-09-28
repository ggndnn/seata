/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.benchmark.rpc.netty;

import io.netty.channel.Channel;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.NetUtil;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.rpc.netty.NettyClientConfig;
import io.seata.core.rpc.netty.NettyPoolKey;
import io.seata.discovery.registry.RegistryFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Netty client pool manager.
 *
 * @author jimin.jm @alibaba-inc.com
 * @author zhaojun
 */
class NettyClientChannelManagerAp2Lock extends BaseNettyClientChannelManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyClientChannelManagerAp2Lock.class);

    private final ConcurrentMap<String, Object> channelLocks = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, NettyPoolKey> poolKeyMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Channel> channels = new ConcurrentHashMap<>();

    private final GenericKeyedObjectPool<NettyPoolKey, Channel> nettyClientKeyPool;

    private Function<String, NettyPoolKey> poolKeyFunction;

    NettyClientChannelManagerAp2Lock(final NettyPoolableFactoryAp2 keyPoolableFactory,
                                     final Function<String, NettyPoolKey> poolKeyFunction,
                                     final NettyClientConfig clientConfig) {
        nettyClientKeyPool = new GenericKeyedObjectPool<>(keyPoolableFactory);
        nettyClientKeyPool.setConfig(getNettyPoolConfig(clientConfig));
        this.poolKeyFunction = poolKeyFunction;
    }

    private GenericKeyedObjectPoolConfig<Channel> getNettyPoolConfig(final NettyClientConfig clientConfig) {
        GenericKeyedObjectPoolConfig<Channel> poolConfig = new GenericKeyedObjectPoolConfig<>();
        poolConfig.setMaxTotal(clientConfig.getMaxPoolActive());
        poolConfig.setMaxTotalPerKey(clientConfig.getMaxPoolActive());
        poolConfig.setMinIdlePerKey(clientConfig.getMinPoolIdle());
        poolConfig.setMaxWaitMillis(clientConfig.getMaxAcquireConnMills());
        poolConfig.setTestOnBorrow(clientConfig.isPoolTestBorrow());
        poolConfig.setTestOnReturn(clientConfig.isPoolTestReturn());
        poolConfig.setLifo(clientConfig.isPoolLifo());
        return poolConfig;
    }

    /**
     * Get all channels registered on current Rpc Client.
     *
     * @return channels
     */
    @Override
    public ConcurrentMap<String, Channel> getChannels() {
        return channels;
    }

    /**
     * Acquire netty client channel connected to remote server.
     *
     * @param serverAddress server address
     * @return netty channel
     */
    @Override
    public Channel acquireChannel(String serverAddress) {
        Channel channelToServer = channels.get(serverAddress);
        if (channelToServer != null) {
            channelToServer = getExistAliveChannel(channelToServer, serverAddress);
            if (null != channelToServer) {
                return channelToServer;
            }
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("will connect to " + serverAddress);
        }
        channelLocks.putIfAbsent(serverAddress, new Object());
        synchronized (channelLocks.get(serverAddress)) {
            return doConnect(serverAddress);
        }
    }

    /**
     * Release channel to pool if necessary.
     *
     * @param channel       channel
     * @param serverAddress server address
     */
    @Override
    public void releaseChannel(Channel channel, String serverAddress) {
        if (null == channel || null == serverAddress) {
            return;
        }
        try {
            synchronized (channelLocks.get(serverAddress)) {
                Channel ch = channels.get(serverAddress);
                if (null == ch) {
                    nettyClientKeyPool.returnObject(poolKeyMap.get(serverAddress), channel);
                    return;
                }
                if (ch.compareTo(channel) == 0) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("return to pool, rm channel:{}", channel);
                    }
                    channels.remove(serverAddress);
                    // destroyChannel(serverAddress, channel);
                }
                nettyClientKeyPool.returnObject(poolKeyMap.get(serverAddress), channel);
            }
        } catch (Exception exx) {
            LOGGER.error(exx.getMessage());
        }
    }

    /**
     * Destroy channel.
     *
     * @param serverAddress server address
     * @param channel       channel
     */
    @Override
    public void destroyChannel(String serverAddress, Channel channel) {
        if (null == channel) {
            return;
        }
        try {
            if (channel.equals(channels.get(serverAddress))) {
                channels.remove(serverAddress);
            }
            nettyClientKeyPool.invalidateObject(poolKeyMap.get(serverAddress), channel);
            // nettyClientKeyPool.returnObject(poolKeyMap.get(serverAddress), channel);
        } catch (Exception exx) {
            LOGGER.error("return channel to rmPool error:{}", exx.getMessage());
        }
    }

    /**
     * Reconnect to remote server of current transaction service group.
     *
     * @param transactionServiceGroup transaction service group
     */
    @Override
    public void reconnect(String transactionServiceGroup) {
        List<String> availList = null;
        try {
            availList = getAvailServerList(transactionServiceGroup);
        } catch (Exception exx) {
            LOGGER.error("Failed to get available servers: {}", exx.getMessage());
        }
        if (CollectionUtils.isEmpty(availList)) {
            LOGGER.error("no available server to connect.");
            return;
        }
        for (String serverAddress : availList) {
            try {
                acquireChannel(serverAddress);
            } catch (Exception e) {
                LOGGER.error("{} can not connect to {} cause:{}", FrameworkErrorCode.NetConnect.getErrCode(), serverAddress, e.getMessage(), e);
            }
        }
    }

    @Override
    public void invalidateObject(final String serverAddress, final Channel channel) throws Exception {
        nettyClientKeyPool.invalidateObject(poolKeyMap.get(serverAddress), channel);
    }

    void registerChannel(final String serverAddress, final Channel channel) {
        if (null != channels.get(serverAddress) && channels.get(serverAddress).isActive()) {
            return;
        }
        channels.put(serverAddress, channel);
    }

    private Channel doConnect(String serverAddress) {
        Channel channelToServer = channels.get(serverAddress);
        if (channelToServer != null && channelToServer.isActive()) {
            return channelToServer;
        }
        Channel channelFromPool;
        try {
            NettyPoolKey currentPoolKey = poolKeyFunction.apply(serverAddress);
            NettyPoolKey previousPoolKey = poolKeyMap.putIfAbsent(serverAddress, currentPoolKey);
            if (null != previousPoolKey && previousPoolKey.getMessage() instanceof RegisterRMRequest) {
                RegisterRMRequest registerRMRequest = (RegisterRMRequest) currentPoolKey.getMessage();
                ((RegisterRMRequest) previousPoolKey.getMessage()).setResourceIds(registerRMRequest.getResourceIds());
            }
            channelFromPool = nettyClientKeyPool.borrowObject(poolKeyMap.get(serverAddress));
            doRegister(poolKeyMap.get(serverAddress), channelFromPool);
            channels.put(serverAddress, channelFromPool);
        } catch (Exception exx) {
            LOGGER.error("{} register RM failed.", FrameworkErrorCode.RegisterRM.getErrCode(), exx);
            throw new FrameworkException("can not register RM,err:" + exx.getMessage());
        }
        return channelFromPool;
    }

    private void doRegister(NettyPoolKey poolKey, Channel channelToServer) {
        long start = System.currentTimeMillis();
        try {
            if (null == poolKey.getMessage()) {
                throw new FrameworkException("register msg is null, role:" + poolKey.getTransactionRole().name());
            }
            Object response = rpcRemotingClient.sendAsyncRequestWithResponse(channelToServer, poolKey.getMessage());
            if (!isResponseSuccess(response, poolKey.getTransactionRole())) {
                rpcRemotingClient.onRegisterMsgFail(poolKey.getAddress(), channelToServer, response,
                        poolKey.getMessage());
            } else {
                rpcRemotingClient.onRegisterMsgSuccess(poolKey.getAddress(), channelToServer, response,
                        poolKey.getMessage());
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "register success, cost " + (System.currentTimeMillis() - start) + " ms, version:"
                                + getVersion(response, poolKey.getTransactionRole()) + ",role:" + poolKey.getTransactionRole().name()
                                + ",channel:" + channelToServer);
            }
        } catch (Exception exx) {
            LOGGER.error("{} register RM failed.", FrameworkErrorCode.RegisterRM.getErrCode(), exx);
            throw new FrameworkException(
                    "register error,role:" + poolKey.getTransactionRole().name() + ",err:" + exx.getMessage());
        }
    }

    private boolean isResponseSuccess(Object response, NettyPoolKey.TransactionRole transactionRole) {
        if (null == response) {
            return false;
        }
        if (transactionRole.equals(NettyPoolKey.TransactionRole.TMROLE)) {
            if (!(response instanceof RegisterTMResponse)) {
                return false;
            }
            return ((RegisterTMResponse) response).isIdentified();
        } else if (transactionRole.equals(NettyPoolKey.TransactionRole.RMROLE)) {
            if (!(response instanceof RegisterRMResponse)) {
                return false;
            }
            return ((RegisterRMResponse) response).isIdentified();
        }
        return false;
    }

    private String getVersion(Object response, NettyPoolKey.TransactionRole transactionRole) {
        if (transactionRole.equals(NettyPoolKey.TransactionRole.TMROLE)) {
            return ((RegisterTMResponse) response).getVersion();
        } else {
            return ((RegisterRMResponse) response).getVersion();
        }
    }

    private List<String> getAvailServerList(String transactionServiceGroup) throws Exception {
        List<String> availList = new ArrayList<>();
        List<InetSocketAddress> availInetSocketAddressList = RegistryFactory.getInstance().lookup(
                transactionServiceGroup);
        if (!CollectionUtils.isEmpty(availInetSocketAddressList)) {
            for (InetSocketAddress address : availInetSocketAddressList) {
                availList.add(NetUtil.toStringAddress(address));
            }
        }
        return availList;
    }

    private Channel getExistAliveChannel(Channel rmChannel, String serverAddress) {
        if (rmChannel.isActive()) {
            return rmChannel;
        } else {
            int i = 0;
            for (; i < NettyClientConfig.getMaxCheckAliveRetry(); i++) {
                try {
                    Thread.sleep(NettyClientConfig.getCheckAliveInternal());
                } catch (InterruptedException exx) {
                    LOGGER.error(exx.getMessage());
                }
                rmChannel = channels.get(serverAddress);
                if (null != rmChannel && rmChannel.isActive()) {
                    return rmChannel;
                }
            }
            if (i == NettyClientConfig.getMaxCheckAliveRetry()) {
                LOGGER.warn("channel {} is not active after long wait, close it.", rmChannel);
                releaseChannel(rmChannel, serverAddress);
                return null;
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        try {
            nettyClientKeyPool.clear();
            nettyClientKeyPool.close();
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new IOException(e);
            }
        }
    }
}

