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
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.rpc.netty.NettyClientConfig;
import io.seata.core.rpc.netty.NettyPoolKey;
import io.seata.core.rpc.netty.RpcClientBootstrap;
import org.mockito.Mockito;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author ggndnn
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
public class RpcPoolBench {
    @Param({RpcPoolType.POOL_TYPE_AP_1_LOCK, RpcPoolType.POOL_TYPE_AP_1_LOCK_CAS, RpcPoolType.POOL_TYPE_AP_1_NO_LOCK, RpcPoolType.POOL_TYPE_AP_2_LOCK, RpcPoolType.POOL_TYPE_AP_2_NO_LOCK})
    public String poolType;

    @Param("1")
    public int poolSize;

    /**
     * 地址产生机制
     */
    @Param({"single", "random"})
    public String addrGenType;

    @Param("8")
    public int randomAddrCount;

    private String[] randomAddrList;

    private BaseNettyClientChannelManager clientChannelManager;

    @Setup(Level.Trial)
    public void setup() {
        if ("random".equals(addrGenType)) {
            randomAddrList = new String[randomAddrCount > 0 ? randomAddrCount : poolSize];
            for (int i = 0; i < randomAddrCount; i++) {
                randomAddrList[i] = String.valueOf(i);
            }
        }
        clientChannelManager = (BaseNettyClientChannelManager) RpcPoolBench.createClientChannelManager(poolType, poolSize);
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        clientChannelManager.close();
    }

    @Benchmark
    @Threads(1)
    public void benchmark() {
        doBenchmark();
    }

    private void doBenchmark() {
        String address = generateAddress();
        Channel ch = clientChannelManager.acquireChannel(address);
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            numbers.add(ThreadLocalRandom.current().nextInt());
        }
        Collections.shuffle(numbers);
        Collections.sort(numbers);
        clientChannelManager.releaseChannel(ch, address);
    }

    private String generateAddress() {
        if ("random".equals(addrGenType)) {
            return randomAddrList[(int) (Math.random() % randomAddrCount)];
        } else {
            return "10.0.0.10";
        }
    }

    static INettyClientChannelManager createClientChannelManager(String poolType) {
        return createClientChannelManager(poolType, -1);
    }

    static INettyClientChannelManager createClientChannelManager(String poolType, int poolSize) {
        Function<String, NettyPoolKey> poolKeyFunction = serverAddress -> {
            String resourceIds = "1,2,3,4,5";
            RegisterRMRequest message = new RegisterRMRequest("app1", "group1");
            message.setResourceIds(resourceIds);
            return new NettyPoolKey(NettyPoolKey.TransactionRole.RMROLE, serverAddress, message);
        };
        INettyClientChannelManager clientChannelManager;
        RpcClientBootstrap bootstrapMock = Mockito.mock(RpcClientBootstrap.class);
        Mockito.when(bootstrapMock.getNewChannel(Mockito.any(InetSocketAddress.class))).thenAnswer(i -> new NettyChannelMock(null, i.getArgument(0)));
        NettyClientConfig clientConfig = new NettyClientConfig();
        clientConfig.poolSize = poolSize;
        switch (poolType) {
            case RpcPoolType.POOL_TYPE_AP_1_LOCK:
                clientChannelManager = new NettyClientChannelManagerAp1Lock(new NettyPoolableFactoryAp1(bootstrapMock), poolKeyFunction, clientConfig);
                break;
            case RpcPoolType.POOL_TYPE_AP_1_LOCK_CAS:
                clientChannelManager = new NettyClientChannelManagerAp1LockCas(new NettyPoolableFactoryAp1Raw(bootstrapMock), poolKeyFunction, clientConfig);
                break;
            case RpcPoolType.POOL_TYPE_AP_1_NO_LOCK:
                clientChannelManager = new NettyClientChannelManagerAp1NoLock(new NettyPoolableFactoryAp1Raw2(bootstrapMock), poolKeyFunction, clientConfig);
                break;
            case RpcPoolType.POOL_TYPE_AP_2_LOCK:
                clientChannelManager = new NettyClientChannelManagerAp2Lock(new NettyPoolableFactoryAp2(bootstrapMock), poolKeyFunction, clientConfig);
                break;
            case RpcPoolType.POOL_TYPE_AP_2_NO_LOCK:
                clientChannelManager = new NettyClientChannelManagerAp2NoLock(new NettyPoolableFactoryAp2Raw(bootstrapMock), poolKeyFunction, clientConfig);
                break;
            case RpcPoolType.POOL_TYPE_AP_2_LOCK_CAS:
                clientChannelManager = new NettyClientChannelManagerAp2LockCas(new NettyPoolableFactoryAp2(bootstrapMock), poolKeyFunction, clientConfig);
                break;
            default:
                clientChannelManager = null;
                break;
        }
        return clientChannelManager;
    }
}
