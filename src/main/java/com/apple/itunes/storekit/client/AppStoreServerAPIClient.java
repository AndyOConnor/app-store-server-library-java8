package com.apple.itunes.storekit.client;

import com.apple.itunes.storekit.model.Environment;
import okhttp3.*;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AppStoreServerAPIClient extends BaseAppStoreServerAPIClient {

    private final OkHttpClient httpClient;
    private final HttpUrl urlBase;

    /**
     * Create an App Store Server API client
     *
     * @param signingKey  Your private key downloaded from App Store Connect
     * @param keyId       Your private key ID from App Store Connect
     * @param issuerId    Your issuer ID from the Keys page in App Store Connect
     * @param bundleId    Your app’s bundle ID
     * @param environment The environment to target
     */
    public AppStoreServerAPIClient(String signingKey, String keyId, String issuerId, String bundleId, Environment environment) {
        this(new BearerTokenAuthenticator(signingKey, keyId, issuerId, bundleId), environment);
    }

    /**
     * Create an App Store Server API client using a custom Bearer token provider
     *
     * @param bearerTokenAuthenticator An implementation of {@link BearerTokenAuthenticatorInterface} that provides tokens
     * @param environment              The environment to target
     */
    public AppStoreServerAPIClient(BearerTokenAuthenticatorInterface bearerTokenAuthenticator, Environment environment) {
        super(bearerTokenAuthenticator, environment);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // If a proxy is configured via java.net.ProxySelector.setDefault, this will allow java.net.Authenticator.setDefault to serve as its auth source
        builder.proxyAuthenticator(new OkHttpAuthenticator("admin", "123456"));
        this.httpClient = builder.build();
        this.urlBase = HttpUrl.parse(this.url);
    }

    @Override
    protected HttpResponseInterface makeRequest(String path,
                                                String method,
                                                Map<String, List<String>> queryParameters,
                                                Map<String, String> headers,
                                                String contentType,
                                                String body) throws IOException {
        Request.Builder requestBuilder = new Request.Builder();
        headers.forEach(requestBuilder::addHeader);
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(urlBase.resolve(path)).newBuilder();
        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
            for (String queryValue : entry.getValue()) {
                urlBuilder.addQueryParameter(entry.getKey(), queryValue);
            }
        }
        requestBuilder.url(urlBuilder.build());
        if (body != null) {
            MediaType mediaType = contentType != null ? MediaType.parse(contentType) : null;
            requestBuilder.method(method, RequestBody.create(mediaType, body));
        } else {
            requestBuilder.method(method, null);
        }
        return new OkHttpResponse(getResponse(requestBuilder.build()));
    }

    protected Response getResponse(Request request) throws IOException {
        Call call = httpClient.newCall(request);
        return call.execute();
    }

    protected static class OkHttpResponse implements HttpResponseInterface {
        private final Response response;

        public OkHttpResponse(Response response) {
            this.response = response;
        }

        @Override
        public int statusCode() {
            return response.code();
        }

        @Override
        public Reader body() {
            ResponseBody responseBody = response.body();
            return responseBody != null ? responseBody.charStream() : null;
        }

        @Override
        public void close() {
            response.close();
        }
    }

    protected static class OkHttpAuthenticator implements Authenticator {

        private String username;
        private String password;

        public OkHttpAuthenticator(String user, String pass) {
            this.username = user;
            this.password = pass;
        }

        @Override
        public Request authenticate(Route route, Response response) throws IOException {
            String credential = Credentials.basic(username, password);
            return response.request().newBuilder()
                    .header("Authorization", credential)
                    .build();
        }
    }

}
