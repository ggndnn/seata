package io.seata.benchmark.rpc.netty;

import io.netty.channel.Channel;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.ResultCode;

import java.util.concurrent.TimeoutException;

/**
 * ggndnn
 */
public class RpcClientMock {
    public void onRegisterMsgSuccess(String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {
    }

    public void onRegisterMsgFail(String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {

    }

    public Object sendAsyncRequestWithResponse(Channel channel, Object msg) throws TimeoutException {
        RegisterRMResponse response = new RegisterRMResponse();
        response.setResultCode(ResultCode.Success);
        response.setMsg("test");
        response.setIdentified(true);
        response.setVersion("1.0");
        return response;
    }
}
