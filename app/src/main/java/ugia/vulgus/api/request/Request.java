package ugia.vulgus.api.request;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import ugia.vulgus.app.constant.Api;

/**
 * Created by joseluisugia on 10/04/14.
 */
public class Request {

    public static enum Method {GET, POST, PUT}

    private static OkHttpClient client;
    private static Gson gson;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private String uri;
    private Method method;
    private String body;

    public Request() {

        if (client == null) {
            client = new OkHttpClient();
            client.setConnectTimeout(60L, TimeUnit.SECONDS);
        }

        if (gson == null) {
            gson = new Gson();
        }
    }

    public Request forUri(String uri) {
        return forUri(uri, Method.GET);
    }

    public Request forUri(String uri, Method method) {
        this.uri = Api.URL + uri;
        this.method = method;
        return this;
    }

    public Request withBody(String body) {
        this.body = body;
        return this;
    }

    public Request withBody(Object body) {
        this.body = gson.toJson(body);
        return this;
    }

    public Response execute() {

        URL url = null;
        try {
            url = new URL("https", Api.HOST, uri);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Response result;
        com.squareup.okhttp.Request.Builder requestBuilder = new com.squareup.okhttp.Request.Builder().url(url);
        com.squareup.okhttp.Request request;

        if (body != null) {
            RequestBody requestBody = RequestBody.create(JSON, body);
            if (method == Method.POST) {
                requestBuilder = requestBuilder.post(requestBody);
            } else {
                requestBuilder = requestBuilder.put(requestBody);
            }
        }
        request = requestBuilder.build();

        try {
            result = client.newCall(request).execute();
        } catch (IOException e) {
            result = new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(500).build();
        }

        return result;
    }

}
