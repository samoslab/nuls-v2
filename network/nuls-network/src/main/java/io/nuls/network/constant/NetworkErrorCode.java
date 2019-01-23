/*
 * *
 *  * MIT License
 *  *
 *  * Copyright (c) 2017-2018 nuls.io
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */
package io.nuls.network.constant;


import io.nuls.tools.constant.ErrorCode;

/**
 * 错误码管理器
 * NetworkErrorCode
 *
 * @author Lan
 */
public interface NetworkErrorCode{
    ErrorCode SUCCESS= ErrorCode.init("10000");

    ErrorCode FAIL= ErrorCode.init("50000");

    ErrorCode DATA_ERROR = ErrorCode.init("20002");
    ErrorCode PARAMETER_ERROR = ErrorCode.init("20003");
    ErrorCode PEER_NOT_EXIST = ErrorCode.init("20004");


    ErrorCode NET_MESSAGE_ERROR = ErrorCode.init("40002");
    ErrorCode NET_MESSAGE_SEND_FAIL = ErrorCode.init("40003");
    ErrorCode NET_MESSAGE_SEND_EXCEPTION = ErrorCode.init("40004");


    ErrorCode NET_BROADCAST_FAIL = ErrorCode.init("40011");
    ErrorCode NET_NODE_DEAD = ErrorCode.init("40013");
    ErrorCode NET_NODE_MISS_CHANNEL = ErrorCode.init("40014");


}