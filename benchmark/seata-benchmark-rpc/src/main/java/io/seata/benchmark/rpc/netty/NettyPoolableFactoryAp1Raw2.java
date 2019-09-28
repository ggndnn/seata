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
import io.seata.core.rpc.netty.RpcClientBootstrap;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @author ggndnn
 */
public class NettyPoolableFactoryAp1Raw2 implements KeyedPoolableObjectFactory<String, Channel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyPoolableFactoryAp1Raw2.class);

    private final RpcClientBootstrap clientBootstrap;

    public NettyPoolableFactoryAp1Raw2(RpcClientBootstrap clientBootstrap) {
        this.clientBootstrap = clientBootstrap;
    }

    @Override
    public Channel makeObject(String key) {
        InetSocketAddress address = NetUtil.toInetSocketAddress(key);
            if (LOGGER.isInfoEnabled()) {
            LOGGER.info("NettyPool create channel to " + key);
        }
        return clientBootstrap.getNewChannel(address);
    }

    @Override
    public void destroyObject(String key, Channel channel) throws Exception {

        if (null != channel) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("will destroy channel:" + channel);
            }
            channel.disconnect();
            channel.close();
        }
    }

    @Override
    public boolean validateObject(String key, Channel obj) {
        if (null != obj && obj.isActive()) {
            return true;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("channel valid false,channel:" + obj);
        }
        return false;
    }

    @Override
    public void activateObject(String key, Channel obj) throws Exception {
    }

    @Override
    public void passivateObject(String key, Channel obj) throws Exception {
    }
}
