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
import io.seata.common.util.NetUtil;
import io.seata.core.rpc.netty.NettyPoolKey;
import io.seata.core.rpc.netty.RpcClientBootstrap;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @author ggndnn
 */
public class NettyPoolableFactoryAp2 implements KeyedPooledObjectFactory<NettyPoolKey, Channel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyPoolableFactoryAp2.class);

    private final RpcClientBootstrap clientBootstrap;

    public NettyPoolableFactoryAp2(RpcClientBootstrap clientBootstrap) {
        this.clientBootstrap = clientBootstrap;
    }

    @Override
    public PooledObject<Channel> makeObject(NettyPoolKey key) {
        InetSocketAddress address = NetUtil.toInetSocketAddress(key.getAddress());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("NettyPool create channel to " + key);
        }
        Channel channel = clientBootstrap.getNewChannel(address);
        return new DefaultPooledObject<>(channel);
    }

    @Override
    public void destroyObject(NettyPoolKey key, PooledObject<Channel> p) throws Exception {
        Channel channel = p.getObject();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("will destroy channel:" + channel);
        }
        channel.disconnect();
        channel.close();
    }

    @Override
    public boolean validateObject(NettyPoolKey key, PooledObject<Channel> p) {
        Channel channel = p.getObject();
        if (null != channel && channel.isActive()) {
            return true;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("channel valid false,channel:" + channel);
        }
        return false;
    }

    @Override
    public void activateObject(NettyPoolKey key, PooledObject<Channel> p) throws Exception {
    }

    @Override
    public void passivateObject(NettyPoolKey key, PooledObject<Channel> p) throws Exception {
    }
}
