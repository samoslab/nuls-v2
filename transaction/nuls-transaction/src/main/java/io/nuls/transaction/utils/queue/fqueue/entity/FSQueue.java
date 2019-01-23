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

import io.nuls.kernel.utils.queue.fqueue.exception.FileEOFException;
import io.nuls.tools.exception.NulsRuntimeException;
import io.nuls.transaction.utils.queue.fqueue.internal.Entity;
import io.nuls.transaction.utils.queue.fqueue.internal.Index;

import java.io.File;
import java.io.IOException;

/**
 * @author opensource
 */
public class FSQueue {
    private long entityLimitLength;
    private String path = null;
    /**
     * 文件操作实例
     */
    private Index idx = null;
    private Entity writerHandle = null;
    private Entity readerHandle = null;
    /**
     * 文件操作位置信息
     */
    private int readerIndex = -1;
    private int writerIndex = -1;

    public FSQueue(String dir) throws Exception {
        this(new File(dir));
    }

    /**
     * 在指定的目录中，以fileLimitLength为单个数据文件的最大大小限制初始化队列存储
     *
     * @param dir               队列数据存储的路径
     * @param entityLimitLength 单个数据文件的大小，不能超过2G
     */
    public FSQueue(String dir, long entityLimitLength) throws Exception {
        this(new File(dir), entityLimitLength);
    }

    public FSQueue(File dir) throws Exception {
        this(dir, 1024 * 1024 * 2);
    }

    /**
     * 在指定的目录中，以fileLimitLength为单个数据文件的最大大小限制初始化队列存储
     *
     * @param dir               队列数据存储的目录
     * @param entityLimitLength 单个数据文件的大小，不能超过2G
     */
    public FSQueue(File dir, long entityLimitLength) throws Exception {
        if (dir.exists() == false && dir.isDirectory() == false) {
            if (dir.mkdirs() == false) {
                throw new IOException("create dir error");
            }
        }
        this.entityLimitLength = entityLimitLength;
        path = dir.getAbsolutePath();
        // 打开索引文件
        idx = new Index(path);
        initHandle();
    }

    private void initHandle() throws Exception {
        writerIndex = idx.getWriterIndex();
        readerIndex = idx.getReaderIndex();
        writerHandle = new Entity(path, writerIndex, entityLimitLength, idx);
        if (readerIndex == writerIndex) {
            readerHandle = writerHandle;
        } else {
            readerHandle = new Entity(path, readerIndex, entityLimitLength, idx);
        }
    }

    /**
     * 一个文件的数据写入达到fileLimitLength的时候，滚动到下一个文件实例
     */
    private void rotateNextLogWriter() throws Exception {
        writerIndex = (writerIndex + 1) % 1000 + 1;
        writerHandle.putNextFileNumber(writerIndex);
        if (readerHandle != writerHandle) {
            writerHandle.close();
        }
        idx.putWriterIndex(writerIndex);
        writerHandle = new Entity(path, writerIndex, entityLimitLength, idx, true);
    }

    /**
     * 向队列存储添加一个byte数组
     */
    public void add(byte[] message) throws Exception {
        short status = writerHandle.write(message);
        if (status == Entity.WRITEFULL) {
            rotateNextLogWriter();
            status = writerHandle.write(message);
        }
        if (status == Entity.WRITESUCCESS) {
            idx.incrementSize();
        }
    }

    private byte[] read(boolean commit) throws Exception {
        byte[] bytes = null;
        try {
            bytes = readerHandle.read(commit);
        } catch (FileEOFException e) {
            int nextFileNumber = readerHandle.getNextFileNumber();
            readerHandle.reset();
            File deleteFile = readerHandle.getFile();
            readerHandle.close();
            deleteFile.delete();
            // 更新下一次读取的位置和索引
            idx.putReaderPosition(Entity.MESSAGE_START_POSITION);
            idx.putReaderIndex(nextFileNumber);
            if (writerHandle.getCurrentFileNumber() == nextFileNumber) {
                readerHandle = writerHandle;
            } else {
                readerHandle = new Entity(path, nextFileNumber, entityLimitLength, idx);
            }
            try {
                bytes = readerHandle.read(commit);
            } catch (FileEOFException e1) {
                throw new NulsRuntimeException(e1);
            }
        }
        if (bytes != null) {
            idx.decrementSize();
        }
        return bytes;
    }

    /**
     * 读取队列头的数据，但不移除。
     */
    public byte[] readNext() throws Exception {
        return read(false);
    }

    /**
     * 从队列存储中取出最先入队的数据，并移除它
     */
    public byte[] readNextAndRemove() throws Exception {
        return read(true);
    }

    public void clear() throws Exception {
        idx.clear();
        initHandle();
    }

    public void close() throws IOException {
        readerHandle.close();
        writerHandle.close();
        idx.close();
    }

    public int getQueueSize() {
        return idx.getSize();
    }
}
