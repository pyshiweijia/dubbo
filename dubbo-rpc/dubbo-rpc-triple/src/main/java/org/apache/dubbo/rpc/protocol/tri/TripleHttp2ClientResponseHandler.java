/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;

public final class TripleHttp2ClientResponseHandler extends SimpleChannelInboundHandler<Http2StreamFrame> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripleHttp2ClientResponseHandler.class);

    public TripleHttp2ClientResponseHandler() {
        super(false);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof Http2GoAwayFrame) {
            ctx.close();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2StreamFrame msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            onHeadersRead(ctx, (Http2HeadersFrame) msg);
        } else if (msg instanceof Http2DataFrame) {
            onDataRead(ctx, (Http2DataFrame) msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    private void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame msg) {
        TripleUtil.getClientStream(ctx).onHeaders(msg.headers());
        if (msg.isEndStream()) {
            final ClientStream clientStream = TripleUtil.getClientStream(ctx);
            clientStream.halfClose();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        final ClientStream clientStream = TripleUtil.getClientStream(ctx);
        final GrpcStatus status = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                .withCause(cause);
        clientStream.onError(status);
        ctx.close();
    }

    public void onDataRead(ChannelHandlerContext ctx, Http2DataFrame msg) throws Exception {
        super.channelRead(ctx, msg.content());
        if (msg.isEndStream()) {
            final ClientStream clientStream = TripleUtil.getClientStream(ctx);
            // stream already closed;
            if (clientStream != null) {
                clientStream.halfClose();
            }
        }
    }
}
