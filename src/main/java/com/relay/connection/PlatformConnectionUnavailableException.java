package com.relay.connection;

public class PlatformConnectionUnavailableException extends RuntimeException {
    public PlatformConnectionUnavailableException() {
        super("Platform OAuth connections are not available yet");
    }
}
