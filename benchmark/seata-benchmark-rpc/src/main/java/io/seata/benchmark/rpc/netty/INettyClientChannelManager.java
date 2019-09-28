package io.seata.benchmark.rpc.netty;

import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentMap;

/**
 * @author ggndnn
 */
public interface INettyClientChannelManager {
    /**
     * Get all channels registered on current Rpc Client.
     *
     * @return channels
     */
    ConcurrentMap<String, Channel> getChannels();

    /**
     * Acquire netty client channel connected to remote server.
     *
     * @param serverAddress server address
     * @return netty channel
     */
    Channel acquireChannel(String serverAddress);

    /**
     * Release channel to pool if necessary.
     *
     * @param channel channel
     * @param serverAddress server address
     */
    void releaseChannel(Channel channel, String serverAddress);

    /**
     * Destroy channel.
     *
     * @param serverAddress server address
     * @param channel channel
     */
    void destroyChannel(String serverAddress, Channel channel);

    /**
     * Reconnect to remote server of current transaction service group.
     *
     * @param transactionServiceGroup transaction service group
     */
    void reconnect(String transactionServiceGroup);

    void invalidateObject(final String serverAddress, final Channel channel) throws Exception;
}
