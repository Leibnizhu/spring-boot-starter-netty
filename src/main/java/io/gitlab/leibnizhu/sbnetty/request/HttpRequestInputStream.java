package io.gitlab.leibnizhu.sbnetty.request;

import com.google.common.primitives.Ints;
import io.gitlab.leibnizhu.sbnetty.core.ClientAbortException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpRequestInputStream extends ServletInputStream {
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object lock = new Object();

    private final ByteToMessageDecoder.Cumulator cumulator = ByteToMessageDecoder.COMPOSITE_CUMULATOR;
    private ByteBuf buf = Unpooled.EMPTY_BUFFER;


    private volatile boolean lastReached = false;

    private final HttpRequestInputStreamReadListenerOp httpRequestInputStreamReadListenerOp = new HttpRequestInputStreamReadListenerOp();

    public void offer(HttpContent httpContent) {
        if (closed.get()) {
            return;
        }

        ByteBuf content = httpContent.content().retain();

        buf = cumulator.cumulate(content.alloc(), buf, content);
        if (httpContent instanceof LastHttpContent) {
            lastReached = true;
        }
        httpRequestInputStreamReadListenerOp.notifyDataAvailable();
    }


    @Override
    public boolean isFinished() {
        if (closed.get()) {
            return true;
        }
        return lastReached && buf.readableBytes() == 0;
    }


    /**
     * 已读入至少一次HttpContent且未读取完所有内容，或者HttpContent队列非空
     */
    @Override
    public boolean isReady() {
        if (closed.get()) {
            return false;
        }
        return buf.readableBytes() > 0;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        httpRequestInputStreamReadListenerOp.setReadListener(readListener);
    }

    /**
     * 跳过n个字节
     */
    @Override
    public long skip(long n) throws IOException {
        checkNotClosed();
        synchronized (lock) {
            long realSkip = 0;
            while (realSkip < n && buf.readableBytes() > 0) {
                long skipLen = Math.min(buf.readableBytes(), n - realSkip);
                buf.skipBytes(Ints.checkedCast(skipLen));
                realSkip += skipLen;
            }
            return realSkip;
        }
    }


    @Override
    public int available() throws IOException {
        return buf.readableBytes();
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            synchronized (lock) {
                // close和read不能同时发生
                ReferenceCountUtil.release(buf);
            }
        }
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        if (isFinished()) {
            return -1;
        }
        synchronized (lock) {
            try {
                if (isFinished()) {
                    return -1;
                }
                int realLen = Math.min(buf.readableBytes(), len);
                buf.readBytes(b, off, realLen);
                if (buf instanceof CompositeByteBuf) {
                    // discard read bytes with batch read model
                    ((CompositeByteBuf) buf).discardSomeReadBytes();
                }
                return realLen;
            } finally {
                if (isFinished()) {
                    httpRequestInputStreamReadListenerOp.notifyAllRead();
                }
            }
        }
    }

    @Override
    public int read() throws IOException {
        if (isFinished()) {
            return -1;
        }
        synchronized (lock) {
            try {
                if (isFinished()) {
                    return -1;
                }
                return buf.readByte() & 0xFF;
            } finally {

                if (isFinished()) {
                    httpRequestInputStreamReadListenerOp.notifyAllRead();
                }
            }
        }
    }


    private void checkNotClosed() throws IOException {
        if (closed.get()) {
            throw new ClientAbortException("Stream is closed");
        }
    }
}
