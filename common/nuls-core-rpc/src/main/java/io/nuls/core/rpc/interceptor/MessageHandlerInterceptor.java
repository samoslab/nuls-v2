/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.core.rpc.interceptor;

import io.nuls.core.rpc.protocol.MessageHandler;
import io.nuls.core.rpc.protocol.ProtocolValidator;
import io.nuls.core.rpc.util.ModuleHelper;
import io.nuls.core.rpc.protocol.Protocol;
import io.nuls.core.rpc.protocol.ProtocolGroupManager;
import io.nuls.core.core.annotation.Interceptor;
import io.nuls.core.core.inteceptor.base.BeanMethodInterceptor;
import io.nuls.core.core.inteceptor.base.BeanMethodInterceptorChain;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author zhouwei
 * @date 2017/10/13
 */
@Interceptor(MessageHandler.class)
public class MessageHandlerInterceptor implements BeanMethodInterceptor<MessageHandler> {

    @Override
    public Object intercept(MessageHandler annotation, Object object, Method method, Object[] params, BeanMethodInterceptorChain interceptorChain) throws Throwable {
        if (ModuleHelper.isSupportProtocolUpdate()) {
            Map map = (Map) params[0];
            int chainId = (Integer) map.get("chainId");
            Protocol protocol = ProtocolGroupManager.getCurrentProtocol(chainId);
            boolean validate = ProtocolValidator.meaasgeValidate(annotation.message(), object.getClass().getSuperclass(), protocol, method.getName());
            if (!validate) {
                throw new RuntimeException("The message or message handler is not available in the current version!");
            }
        }
        return interceptorChain.execute(annotation, object, method, params);
    }
}
