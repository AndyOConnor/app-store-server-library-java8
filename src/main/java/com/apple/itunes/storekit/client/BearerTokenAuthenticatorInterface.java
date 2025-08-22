package com.apple.itunes.storekit.client;

@FunctionalInterface
public interface BearerTokenAuthenticatorInterface {
    String generateToken();
}
