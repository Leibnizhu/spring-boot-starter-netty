package io.gitlab.leibnizhu.sbnetty.request;

import javax.servlet.ReadListener;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpRequestInputStreamReadListenerOp {
    private ReadListener readListener;
    private volatile boolean dataAvailableCalled;

    public void setReadListener(ReadListener readListener) {
        checkNotNull(readListener);
        if (this.readListener != null) {
            throw new IllegalStateException("ReadListener is already set");
        }
        this.readListener = readListener;
    }

    void notifyDataAvailable() {
        if (dataAvailableCalled) {
            return;
        }
        dataAvailableCalled = true;
        if (readListener == null) {
            return;
        }
        try {
            readListener.onDataAvailable();
        } catch (Throwable t) {
            readListener.onError(t);
        }
    }

    void notifyAllRead() {
        if (readListener == null) {
            return;
        }
        try {
            readListener.onAllDataRead();
        } catch (Throwable t) {
            readListener.onError(t);
        }
    }
}
