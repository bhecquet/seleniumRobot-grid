package com.infotel.seleniumrobot.grid.tests.aspects;

/*
Copied from Selenium code
 */

import org.openqa.selenium.remote.http.*;

import java.io.IOException;
import java.io.UncheckedIOException;

public class PassthroughHttpClient implements HttpClient {

    private final Routable handler;

    public PassthroughHttpClient(Routable handler) {
        this.handler = handler;
    }

    @Override
    public HttpResponse execute(HttpRequest request) {
        if (!handler.matches(request)) {
            throw new UncheckedIOException(new IOException("Doomed"));
        }

        return handler.execute(request);
    }

    @Override
    public WebSocket openSocket(HttpRequest request, WebSocket.Listener listener) {
        throw new UnsupportedOperationException("openSocket");
    }

    @Override
    public <T> java.util.concurrent.CompletableFuture<java.net.http.HttpResponse<T>> sendAsyncNative(
            java.net.http.HttpRequest request, java.net.http.HttpResponse.BodyHandler<T> handler) {
        throw new UnsupportedOperationException("sendAsyncNative");
    }

    @Override
    public <T> java.net.http.HttpResponse<T> sendNative(
            java.net.http.HttpRequest request, java.net.http.HttpResponse.BodyHandler<T> handler)
            throws java.io.IOException, InterruptedException {
        throw new UnsupportedOperationException("sendNative");
    }

    public static class Factory implements HttpClient.Factory {

        private final Routable handler;

        public Factory(Routable handler) {
            this.handler = handler;
        }

        public HttpClient createClient(ClientConfig config) {
            return new PassthroughHttpClient(config.filter().andFinally(handler));
        }

        @Override
        public void cleanupIdleClients() {
            // Does nothing
        }
    }
}