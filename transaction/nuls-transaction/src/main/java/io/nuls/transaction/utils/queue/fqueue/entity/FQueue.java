/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.transaction.utils.queue.fqueue.entity;


import static io.nuls.transaction.utils.LoggerUtil.Log;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 基于文件系统的持久化队列
 *
 * @author opensource
 */
public class FQueue extends AbstractQueue<byte[]> implements Serializable {
    private static final long serialVersionUID = -1L;

    private FSQueue fsQueue = null;
    private Lock lock = new ReentrantReadWriteLock().writeLock();

    public FQueue(String path) throws IOException {
        try {
            fsQueue = new FSQueue(path);
        } catch (Exception e) {
            Log.error(e);
        }
    }

    /**
     * 创建一个持久化队列
     *
     * @param path              文件的存储路径
     * @param entityLimitLength 存储数据的单个文件的大小
     */
    public FQueue(String path, long entityLimitLength) throws Exception {
        fsQueue = new FSQueue(path, entityLimitLength);
    }

    public FQueue(File dir) throws Exception {
        fsQueue = new FSQueue(dir);
    }

    /**
     * 创建一个持久化队列
     *
     * @param dir               文件的存储目录
     * @param entityLimitLength 存储数据的单个文件的大小
     */
    public FQueue(File dir, int entityLimitLength) throws Exception {
        fsQueue = new FSQueue(dir, entityLimitLength);
    }

    @Override
    public Iterator<byte[]> iterator() {
        throw new UnsupportedOperationException("iterator Unsupported now");
    }

    @Override
    public int size() {
        return fsQueue.getQueueSize();
    }

    @Override
    public boolean offer(byte[] e) {
        try {
            lock.lock();
            fsQueue.add(e);
            return true;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (Exception ex) {
        } finally {
            lock.unlock();
        }
        return false;
    }

    @Override
    public byte[] peek() {
        try {
            lock.lock();
            return fsQueue.readNext();
        } catch (IOException ex) {
            return null;
        } catch (Exception ex) {
            return null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public byte[] poll() {
        try {
            lock.lock();
            return fsQueue.readNextAndRemove();
        } catch (IOException ex) {
            return null;
        } catch (Exception ex) {
            return null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        try {
            lock.lock();
            fsQueue.clear();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (Exception e) {
        } finally {
            lock.unlock();
        }
    }

    /**
     * 关闭文件队列
     */
    public void close() throws IOException {
        if (fsQueue != null) {
            fsQueue.close();
        }
    }
}
