package io.gitlab.leibnizhu.sbnetty.core;

import java.io.IOException;

public class ClientAbortException extends IOException {
    public ClientAbortException(String message) {
        super(message);
    }
}
