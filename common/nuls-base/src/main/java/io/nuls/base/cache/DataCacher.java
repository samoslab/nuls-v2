/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.base.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 异步请求响应结果缓存类
 *
 * @author captain
 * @version 1.0
 * @date 18-11-12 下午2:35
 */
public class DataCacher<T> {

    public DataCacher() {
    }

    private Map<String, CompletableFuture<T>> cacher = new HashMap<>();

    public CompletableFuture<T> addFuture(String hash) {
        var future = new CompletableFuture<T>();
        cacher.put(hash, future);
        return future;
    }

    public boolean complete(String hash, T t) {
        CompletableFuture<T> future = cacher.get(hash);
        if (future == null) {
            return false;
        }
        future.complete(t);
        cacher.remove(hash);
        return true;
    }

    public void removeFuture(byte[] hash) {
        cacher.remove(hash);
    }
}
