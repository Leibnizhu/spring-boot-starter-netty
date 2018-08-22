package io.gitlab.leibnizhu.sbnetty.request;

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 将Netty的http 解码结果解析成Servlet支持的
 */
public class HttpRequestInputStream extends ServletInputStream {
    private final Channel channel; //netty ChannelHandlerContext的channel
    private AtomicBoolean closed; //输入流是否已经关闭，保证线程安全
    private final BlockingQueue<HttpContent> queue; //HttpContent的队列，一次请求可能有多次加入
    private HttpContent current;
    private int currentLength;
    private ReadListener readListener;

    public HttpRequestInputStream(Channel channel) {
        this.channel = checkNotNull(channel);
        this.closed = new AtomicBoolean();
        queue = new LinkedBlockingQueue<>();
    }

    public void addContent(HttpContent httpContent) {
        checkNotClosed();
        queue.offer(httpContent.retain());
    }

    public int getCurrentLength() {
        return currentLength;
    }

    @Override
    public int readLine(byte[] b, int off, int len) throws IOException {
        checkNotNull(b);
        return super.readLine(b, off, len); //模板方法，会调用当前类实现的read()方法
    }

    /**
     * 本次请求没再有新的HttpContent输入，而且当前的内容全部被读完
     * @return true=读取完毕 反之false
     */
    @Override
    public boolean isFinished() {
        checkNotClosed();
        return isLastContent() && current.content().readableBytes() == 0;
    }

    /**
     * 已经传入本次请求所有HttpContent
     */
    private boolean isLastContent() {
        return current instanceof LastHttpContent;
    }

    /**
     * 已读入至少一次HttpContent且未读取完所有内容，或者HttpContent队列非空
     */
    @Override
    public boolean isReady() {
        checkNotClosed();
        return (current != null && current.content().readableBytes() > 0) || !queue.isEmpty();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        checkNotClosed();
        checkNotNull(readListener);
        this.readListener = readListener;
    }

    /**
     * 跳过n个字节
     */
    @Override
    public long skip(long n) throws IOException {
        checkNotClosed();
        ByteBuf content = current.content();
        long skipLen = Math.min(content.readableBytes(), n); //实际可以跳过的字节数
        content.skipBytes(Ints.checkedCast(skipLen));
        return skipLen;
    }

    /**
     * @return 可读字节数
     */
    @Override
    public int available() throws IOException {
        return null == current ? 0 : current.content().readableBytes();
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            closeHttpContentQueue();
            closeCurrentHttpContent();
        }
    }

    /**
     * 关闭当前HttpContent
     */
    public void closeCurrentHttpContent() {
        if(current != null){
            current.release();
            current = null;
        }
    }

    /**
     * 关闭HttpContent队列
     */
    private void closeHttpContentQueue() {
        while(!queue.isEmpty()){
            HttpContent content = queue.poll();
            if(content != null){
                content.release();
            }
        }
        queue.clear();
    }

    /**
     * 尝试更新current，然后读取len个字节并复制到b中（off下标开始）
     * @return 实际读取的字节数
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkNotNull(b);
        if (0 == len) {
            return 0;
        }
        poll();
        if (isFinished()) {
            return -1;
        }
        ByteBuf byteBuf = readContent(len);//读取len个字节
        int readableBytes = byteBuf.readableBytes();
        byteBuf.readBytes(b, off, readableBytes);//复制到b
        return readableBytes - byteBuf.readableBytes();//返回实际读取的字节数
    }

    /**
     * 尝试更新current，然后读取一个字节，并返回
     */
    @Override
    public int read() throws IOException {
        poll();
        if (isFinished()) {
            return -1;
        }
        return readContent(1).getByte(0);
    }

    /**
     * 从current的HttpContent中读取length个字节
     */
    private ByteBuf readContent(int length) {
        ByteBuf content = current.content();
        if (length < content.readableBytes()) {
            return content.readSlice(length);
        } else {
            return content;
        }
    }

    /**
     * 如果没有可读字节了，从HttpContent队列中获取一个到current中
     * 如果readListener非空，则非阻塞，读不到数据也直接返回
     * @throws IOException channel非激活状态
     */
    private void poll() throws IOException {
        checkNotClosed();
        if (null == current || current.content().readableBytes() == 0) {
            boolean blocking = null == readListener;
            while (!isLastContent()) { //current为空，或者current不是当前请求最后一个HttpContent
                try {
                    current = queue.poll(1000, TimeUnit.NANOSECONDS);
                } catch (InterruptedException ignored) {
                }
                if(current != null){
                    this.currentLength = current.content().readableBytes();
                }
                if (current != null || !blocking) { //队列中读取到数据，或者readListener非空（非阻塞），则退出
                    break;
                }
                if (!channel.isActive()) {
                    throw new IOException("Channel is not active");
                }
            }
        }
    }

    private void checkNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("Stream is closed");
        }
    }
}
