/*
 * Copyright (c) 2010-2010 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.larex.io.connector;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;

import org.codehaus.larex.io.ByteBuffers;
import org.codehaus.larex.io.RuntimeIOException;
import org.codehaus.larex.io.RuntimeSocketConnectException;
import org.codehaus.larex.io.RuntimeSocketTimeoutException;
import org.codehaus.larex.io.async.AsyncChannel;
import org.codehaus.larex.io.async.AsyncCoordinator;
import org.codehaus.larex.io.async.AsyncInterpreter;
import org.codehaus.larex.io.async.AsyncInterpreterFactory;
import org.codehaus.larex.io.async.AsyncSelector;
import org.codehaus.larex.io.async.StandardAsyncChannel;
import org.codehaus.larex.io.async.StandardAsyncCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Revision$ $Date$
 */
public class StandardEndpoint<T extends AsyncInterpreter> extends Endpoint<T>
{
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SocketChannel channel;
    private final AsyncSelector selector;
    private final AsyncInterpreterFactory<T> interpreterFactory;
    private final Executor threadPool;
    private final ByteBuffers byteBuffers;

    public StandardEndpoint(SocketChannel channel, AsyncSelector selector, AsyncInterpreterFactory<T> interpreterFactory, Executor threadPool, ByteBuffers byteBuffers)
    {
        this.channel = channel;
        this.selector = selector;
        this.interpreterFactory = interpreterFactory;
        this.threadPool = threadPool;
        this.byteBuffers = byteBuffers;
    }

    @Override
    public T connect(InetSocketAddress address)
    {
        try
        {
            Socket socket = channel.socket();
            InetSocketAddress bindAddress = getBindAddress();
            if (bindAddress != null)
            {
                socket.bind(bindAddress);
                logger.debug("{} bound to {}", this, bindAddress);
            }
            int connectTimeout = getConnectTimeout();
            if (connectTimeout < 0)
                connectTimeout = 0;
            logger.debug("{} connecting to {} (timeout {})", new Object[]{this, address, connectTimeout});
            socket.connect(address, connectTimeout);
            logger.debug("{} connected to {}", this, address);
            return connected(channel);
        }
        catch (AlreadyConnectedException x)
        {
            close();
            throw x;
        }
        catch (ConnectException x)
        {
            close();
            throw new RuntimeSocketConnectException(x);
        }
        catch (SocketTimeoutException x)
        {
            close();
            throw new RuntimeSocketTimeoutException(x);
        }
        catch (IOException x)
        {
            close();
            throw new RuntimeIOException(x);
        }
    }

    protected T connected(SocketChannel channel) throws IOException
    {
        channel.configureBlocking(false);

        AsyncCoordinator coordinator = newCoordinator(selector, threadPool);

        AsyncChannel asyncChannel = newAsyncChannel(channel, coordinator, byteBuffers);
        coordinator.setAsyncChannel(asyncChannel);

        T interpreter = interpreterFactory.newAsyncInterpreter(coordinator);
        coordinator.setAsyncInterpreter(interpreter);

        register(selector, asyncChannel, coordinator);

        return interpreter;
    }

    protected AsyncCoordinator newCoordinator(AsyncSelector selector, Executor threadPool)
    {
        return new StandardAsyncCoordinator(selector, threadPool);
    }

    protected AsyncChannel newAsyncChannel(SocketChannel channel, AsyncCoordinator coordinator, ByteBuffers byteBuffers)
    {
        return new StandardAsyncChannel(channel, coordinator, byteBuffers);
    }

    protected void register(AsyncSelector selector, AsyncChannel channel, AsyncCoordinator coordinator)
    {
        selector.register(channel, coordinator);
    }

    private void close()
    {
        try
        {
            channel.close();
        }
        catch (IOException x)
        {
            logger.debug("Exception closing channel " + channel, x);
        }
    }
}
