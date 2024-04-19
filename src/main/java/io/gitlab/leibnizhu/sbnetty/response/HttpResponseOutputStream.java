package io.gitlab.leibnizhu.sbnetty.response;

import com.google.common.base.Preconditions;
import io.gitlab.leibnizhu.sbnetty.core.ClientAbortException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.DefaultLastHttpContent;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpResponseOutputStream extends ServletOutputStream {

    private final ChannelHandlerContext ctx;
    private final NettyHttpServletResponse servletResponse;
    private WriteListener writeListener;

    private final ByteToMessageDecoder.Cumulator cumulator = ByteToMessageDecoder.COMPOSITE_CUMULATOR;
    private ByteBuf buf = Unpooled.EMPTY_BUFFER;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 8;
    private static final int MAX_BUFFER_SIZE = 1024 * 1024 * 4;
    private volatile boolean closed;

    private volatile boolean hasCommit = false;

    public boolean isHasCommit() {
        return hasCommit;
    }

    private Integer outerBufferSize;
    private final ReentrantLock lock = new ReentrantLock();

    HttpResponseOutputStream(ChannelHandlerContext ctx, NettyHttpServletResponse servletResponse) {
        this.ctx = ctx;
        this.servletResponse = servletResponse;
    }

    @Override
    public boolean isReady() {
        return ctx.channel().isWritable();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        checkNotNull(writeListener);
        if (this.writeListener != null) {
            throw new IllegalStateException("writeListener already set");
        }
        this.writeListener = writeListener;

        // write is Possible in any time
        ctx.executor().execute(() -> {
            try {
                writeListener.onWritePossible();
            } catch (Exception e) {
                writeListener.onError(e);
            }
        });
    }


    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        lock.lock();
        try {
            checkClose();
            // 这个byte[]数组会被重复使用，所以这里需要复制新的空间
            ByteBuf appendedBuffer = ctx.alloc().buffer(len);
            appendedBuffer.writeBytes(b, off, len);

            buf = cumulator.cumulate(ctx.alloc(), buf, appendedBuffer);
            if (buf.readableBytes() >= getBufferSize()) {
                flush();
            }
        } finally {
            lock.unlock();
        }
    }


    @Override
    public void write(int b) throws IOException {
        lock.lock();
        try {
            checkClose();
            buf.writeByte(b);
        } finally {
            lock.unlock();
        }

    }


    @Override
    public void flush() {
        performFlush(true);
    }

    private void performFlush(boolean flushNetty) {
        lock.lock();
        try {
            hasCommit = true;
            servletResponse.ensureResponseHeader(buf.readableBytes() > 0);
            if (buf.readableBytes() == 0) {
                return;
            }
            if (flushNetty) {
                ctx.writeAndFlush(buf);
            } else {
                ctx.write(buf);
            }
            buf = Unpooled.EMPTY_BUFFER;
        } finally {
            lock.unlock();
        }
    }

    private void checkClose() throws IOException {
        if (closed) {
            throw new ClientAbortException("user reset");
        }
    }


    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            if (closed) {
                return;
            }
            closed = true;
            performFlush(false);
            ChannelFuture future = ctx.writeAndFlush(DefaultLastHttpContent.EMPTY_LAST_CONTENT);
            if (!servletResponse.isKeepAlive()) {
                future.addListener(ChannelFutureListener.CLOSE);//如果不是keep-alive，写完后关闭channel
            }
        } finally {
            buf = Unpooled.EMPTY_BUFFER;
            lock.unlock();
        }
    }

    void resetBuffer() {
        Preconditions.checkArgument(!hasCommit, "can not perform after commit");
        buf.readerIndex(0);
        buf.writerIndex(0);
    }

    int getBufferSize() {
        if (outerBufferSize != null) {
            return outerBufferSize;
        }
        int choosedBufferSize = buf.nioBufferCount();
        if (choosedBufferSize < DEFAULT_BUFFER_SIZE) {
            choosedBufferSize = DEFAULT_BUFFER_SIZE;
        } else if (choosedBufferSize > MAX_BUFFER_SIZE) {
            choosedBufferSize = MAX_BUFFER_SIZE;
        }
        return choosedBufferSize;
    }

    void setBufferSize(int size) {
        outerBufferSize = size;
    }
}
