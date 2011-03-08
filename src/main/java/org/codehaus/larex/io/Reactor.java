/*
 * Copyright (c) 2010 the original author or authors
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

package org.codehaus.larex.io;

import java.nio.channels.Selector;

/**
 * <p>{@link Reactor} hides the complexity of working with {@link Selector}.</p>
 * <p>A {@link Reactor} associates an {@link Channel} to a {@link Listener} so that
 * when the I/O system associated to the channel signals readiness for I/O events, the listener is
 * notified.</p>
 * <p>It is possible to interact with a {@link Reactor} only by {@link #submit(Runnable) submitting}
 * tasks that will be executed by the {@link Reactor} on a dedicated thread.
 */
public interface Reactor
{
    /**
     * <p>Associates the given {@code channel} to the given {@code listener}, so that when the
     * I/O system detects activity for the channel, the listener is notified.</p>
     *
     * @param channel  the channel to register
     * @param listener the listener to notify
     * @see #unregister(Channel, Listener)
     */
    public void register(Channel channel, Listener listener);

    /**
     * <p>Updates the given {@code channel} by adding or removing interest on the given
     * {@code operations}.</p>
     *
     * @param channel    the channel to update
     * @param operations the operations to add or remove
     * @param add        whether to add or remove the operations
     */
    public void update(Channel channel, int operations, boolean add);

    /**
     * <p>Disassociates the given {@code channel} from the given {@code listener}.</p>
     *
     * @param channel the channel to unregister
     * @param listener the associated listener
     * @see #register(Channel, Listener)
     */
    public void unregister(Channel channel, Listener listener);

    /**
     * Submits the given {@code task} for execution by this {@link Reactor}.
     * @param task the task to execute.
     */
    public void submit(Runnable task);

    /**
     * <p>Closes this reactor.</p>
     * <p>Closing a reactor causes all channels registered with it to be closed.</p>
     */
    public void close();

    /**
     * <p>Blocks after a close request until this reactor terminates, the given {@code timeout}
     * elapses or the current thread is interrupted.</p>
     *
     * @param timeout the maximum time to wait, in milliseconds
     * @return true if this reactor terminated, false if the timeout elapsed
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean join(long timeout) throws InterruptedException;

    /**
     * <p>The interface for receiving events from a {@link Reactor}.</p>
     */
    public interface Listener
    {
        /**
         * <p>Invoked when the {@link Reactor} first registers with the I/O system.</p>
         */
        void onOpen();

        /**
         * <p>Invoked when the {@link Reactor} detects that the I/O system is ready to read.</p>
         *
         * @see #onWriteReady()
         */
        public void onReadReady();

        /**
         * <p>Invoked when the {@link Reactor} detects that the I/O system is ready to write.</p>
         *
         * @see #onReadReady()
         */
        public void onWriteReady();

        /**
         * <p>Invoked when the {@link Reactor} detects that the I/O system is closed.</p>
         */
        void onClose();
    }
}
