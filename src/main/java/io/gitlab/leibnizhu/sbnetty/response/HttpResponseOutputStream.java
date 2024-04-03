package io.gitlab.leibnizhu.sbnetty.response;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * 响应输出流
 */
public class HttpResponseOutputStream extends ServletOutputStream {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 8;

    private final ChannelHandlerContext ctx;
    private final NettyHttpServletResponse servletResponse;
    private WriteListener writeListener; //监听器，暂时没处理
    HttpResponseOutputStream(ChannelHandlerContext ctx, NettyHttpServletResponse servletResponse) {
        this.ctx = ctx;
        this.servletResponse = servletResponse;
        this.buf = new byte[DEFAULT_BUFFER_SIZE];
    }

    @Override
    public boolean isReady() {
        return true; // TODO implement
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        checkNotNull(writeListener);
        if(this.writeListener != null){
            return; //只能设置一次
        }
        this.writeListener = writeListener;
        // TODO ISE when called more than once
        // TODO ISE when associated request is not async
    }

    private byte[] buf; //缓冲区
    private int count; //缓冲区游标（记录写到哪里）
    private int totalLength;//内容总长度
    private boolean closed; //是否已经调用close()方法关闭输出流

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        totalLength += len;
        if (len > count) {
            flushBuffer();
            ByteBuf content = ctx.alloc().buffer(len);
            content.writeBytes(b, off, len);
            writeContent(content, false);
            return;
        }
        writeBufferIfNeeded(len);
        System.arraycopy(b, off, buf, count, len); //输入的b复制到缓存buf
        count += len;
    }

    @Override
    public void write(int b) throws IOException {
        writeBufferIfNeeded(1);
        buf[count++] = (byte) b;
        totalLength++;
    }

    private void writeBufferIfNeeded(int len) throws IOException {
        if (len > buf.length - count) { //buffer剩余空间不足则flush
            flushBuffer();
        }
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
    }

    private void flushBuffer() {
        flushBuffer(false);
    }

    private void flushBuffer(boolean lastContent) {
        if (count > 0) {
            ByteBuf content = ctx.alloc().buffer(count);
            content.writeBytes(buf, 0, count);//buf写入ByteBuf
            count = 0;//游标归位
            writeContent(content, lastContent);
        } else if (lastContent) { //如果是最后一次flush，即便内容为空也要执行ctx.write写入EMPTY_LAST_CONTENT
            writeContent(Unpooled.EMPTY_BUFFER, true);
        }
    }

    private void writeContent(ByteBuf content, boolean lastContent) {
        boolean hasBody = content.readableBytes() > 0;
        servletResponse.ensureResponseHeader(hasBody);
        if (hasBody) {
            assert content.refCnt() == 1;
            ctx.write(content, ctx.voidPromise());
        }
        if (lastContent) {
            ChannelFuture future = ctx.write(DefaultLastHttpContent.EMPTY_LAST_CONTENT);
            if (!servletResponse.isKeepAlive()) {
                future.addListener(ChannelFutureListener.CLOSE);//如果不是keep-alive，写完后关闭channel
            }
            servletResponse.commit();
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        try {
            flushBuffer(true);
            ctx.flush();
        } finally {
            buf = null;
        }
        closed = true;
    }

    void resetBuffer() {
        assert !servletResponse.isCommitted();
        count = 0;
    }

    int getBufferSize() {
        return buf.length;
    }

    void setBufferSize(int size) {
        assert !servletResponse.isCommitted();
        checkState(count == 0, "Response body content has been written");
        buf = new byte[size];
    }
}
