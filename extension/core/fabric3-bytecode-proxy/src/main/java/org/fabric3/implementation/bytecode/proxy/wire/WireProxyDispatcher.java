/*
 * Fabric3
 * Copyright (c) 2009-2012 Metaform Systems
 *
 * Fabric3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version, with the
 * following exception:
 *
 * Linking this software statically or dynamically with other
 * modules is making a combined work based on this software.
 * Thus, the terms and conditions of the GNU General Public
 * License cover the whole combination.
 *
 * As a special exception, the copyright holders of this software
 * give you permission to link this software with independent
 * modules to produce an executable, regardless of the license
 * terms of these independent modules, and to copy and distribute
 * the resulting executable under terms of your choice, provided
 * that you also meet, for each linked independent module, the
 * terms and conditions of the license of that module. An
 * independent module is a module which is not derived from or
 * based on this software. If you modify this software, you may
 * extend this exception to your version of the software, but
 * you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version.
 *
 * Fabric3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the
 * GNU General Public License along with Fabric3.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.fabric3.implementation.bytecode.proxy.wire;

import org.fabric3.implementation.bytecode.proxy.common.ProxyDispatcher;
import org.fabric3.spi.invocation.CallFrame;
import org.fabric3.spi.invocation.Message;
import org.fabric3.spi.invocation.MessageCache;
import org.fabric3.spi.invocation.WorkContext;
import org.fabric3.spi.invocation.WorkContextCache;
import org.fabric3.spi.wire.Interceptor;
import org.fabric3.spi.wire.InvocationChain;
import org.oasisopen.sca.ServiceReference;
import org.oasisopen.sca.ServiceRuntimeException;
import org.oasisopen.sca.ServiceUnavailableException;

/**
 * Dispatches from a proxy to a wire.
 */
public class WireProxyDispatcher<B> implements ProxyDispatcher, ServiceReference<B> {
    private static final long serialVersionUID = -3766594738137530257L;

    private Class<B> interfaze;
    private String callbackUri;
    private transient InvocationChain[] chains;

    public void init(Class<B> interfaze, String callbackUri, InvocationChain[] chains) {
        this.interfaze = interfaze;
        this.callbackUri = callbackUri;
        this.chains = chains;
    }

    public B getService() {
        throw new UnsupportedOperationException();
    }

    public Class<B> getBusinessInterface() {
        return interfaze;
    }

    public Object _f3_invoke(int index, Object args) throws Throwable {
        InvocationChain chain = chains[index];

        Interceptor headInterceptor = chain.getHeadInterceptor();

        WorkContext workContext = WorkContextCache.getThreadWorkContext();

        if (callbackUri != null) {
            initializeCallFrame(workContext);
        }

        Message message = MessageCache.getAndResetMessage();
        message.setBody(args);
        message.setWorkContext(workContext);
        try {
            // dispatch the invocation down the chain and get the response
            Message response;
            try {
                response = headInterceptor.invoke(message);
            } catch (ServiceUnavailableException e) {
                // simply rethrow ServiceUnavailableExceptions
                throw e;
            } catch (ServiceRuntimeException e) {
                // simply rethrow ServiceRuntimeException
                throw e;
            } catch (RuntimeException e) {
                // wrap other exceptions raised by the runtime
                throw new ServiceUnavailableException(e);
            }

            // handle response from the application, returning or throwing an exception as appropriate
            Object body = response.getBody();
            boolean fault = response.isFault();

            if (fault) {
                throw (Throwable) body;
            } else {
                return body;
            }
        } finally {
            if (callbackUri != null) {
                // no callframe was created as the wire is unidrectional
                workContext.popCallFrame();
            }
            message.reset();
        }

    }

    /**
     * Initializes and returns a CallFrame for the invocation if it is required. A CallFrame is required if the wire is bidrectional (i.e. there is a callback).
     * It is not required if the wire is targeted to a unidirectional service. If not required, null is returned, thereby avoiding the overhead of creating and
     * pushing a CallFrame onto the current WorkContext.
     *
     * @param workContext the current work context
     */
    private void initializeCallFrame(WorkContext workContext) {
        // the wire is bidrectional so a callframe is required
        CallFrame frame = new CallFrame(callbackUri, null);
        workContext.addCallFrame(frame);
    }

}
