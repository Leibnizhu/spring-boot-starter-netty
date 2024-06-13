package io.gitlab.leibnizhu.sbnetty.request;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;

public class NettyPart implements Part {
    private final HttpData httpData;
    private final HttpHeaders httpHeaders = new DefaultHttpHeaders();

    private NettyPart(HttpData httpData) {
        this.httpData = httpData;

        if (httpData instanceof FileUpload) {
            FileUpload fileUpload = (FileUpload) httpData;
            httpHeaders.set(HttpHeaderNames.CONTENT_DISPOSITION, HttpHeaderValues.FORM_DATA + "; " + HttpHeaderValues.NAME + "=\"" + fileUpload.getName() +
                    "\"; " + HttpHeaderValues.FILENAME + "=\"" + fileUpload.getFilename() + "\"");
            httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, fileUpload.getContentType() +
                    (fileUpload.getCharset() != null ? "; " + HttpHeaderValues.CHARSET + '=' + fileUpload.getCharset().name() : ""));
            httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, fileUpload.length());
            httpHeaders.set("Completed", fileUpload.isCompleted());
            httpHeaders.set("IsInMemory", fileUpload.isInMemory());
        } else {
            httpHeaders.set(HttpHeaderNames.CONTENT_DISPOSITION, HttpHeaderValues.FORM_DATA + "; " + HttpHeaderValues.NAME + "=\"" + httpData.getName() + "\"");
        }
    }

    public static NettyPart of(InterfaceHttpData httpData) {
        if (!(httpData instanceof HttpData)) {
            return null;
        }
        return new NettyPart((HttpData) httpData);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (httpData.isInMemory()) {
            return new ByteArrayInputStream(httpData.get());
        }
        File file = httpData.getFile();
        return Files.newInputStream(file.toPath());
    }

    @Override
    public String getContentType() {
        if (httpData instanceof FileUpload) {
            return ((FileUpload) httpData).getContentType();
        }
        return null;
    }

    @Override
    public String getName() {
        return httpData.getName();
    }

    @Override
    public String getSubmittedFileName() {
        if (httpData instanceof FileUpload) {
            return ((FileUpload) httpData).getFilename();
        }
        return null;
    }

    @Override
    public long getSize() {
        return httpData.length();
    }

    @Override
    public void write(String fileName) throws IOException {
        if (httpData instanceof FileUpload) {
            httpData.renameTo(new File(fileName));
        }
    }

    @Override
    public void delete() throws IOException {
        if (httpData instanceof FileUpload) {
            httpData.delete();
        }
    }

    @Override
    public String getHeader(String name) {
        return httpHeaders.get(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return httpHeaders.getAllAsString(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return httpHeaders.names();
    }
}
