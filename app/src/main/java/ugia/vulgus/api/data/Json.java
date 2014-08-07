package ugia.vulgus.api.data;

import java.io.IOException;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.Response;
import ugia.vulgus.api.data.object.ClassifiedLatLng;

/**
 * Created by joseluisugia on 01/07/14.
 */
public class Json {

    private static Gson gson;
    private static JsonParser parser;

    public static GsonBuilder getBuilder() {
        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        builder.registerTypeAdapter(ClassifiedLatLng.class, new ClassifiedLatLngDeserializer());
        return builder;
    }

    static {
        gson = getBuilder().create();
        parser = new JsonParser();
    }

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    /**
     * Parses a given JSON string into an object instance of the given type,
     * reflecting all the type parameters as appropriate.
     */
    public static <T> T fromJson(String json, Type typeOfT) {
        return gson.fromJson(json, typeOfT);
    }

    /**
     * Parses a given JSON element into an object instance of the given type,
     * reflecting all the type parameters as appropriate.
     */
    public static <T> T fromJson(JsonElement json, Type typeOfT) {
        return gson.fromJson(json, typeOfT);
    }

    public static <T> T fromJson(Response response, Type typeOfT) {
        try {
            return gson.fromJson(response.body().string(), typeOfT);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Parses a given JSON in a string form into a JSON element to which access its properties.
     */
    public static JsonObject parseJson(String jsonString) {
        return parser.parse(jsonString).getAsJsonObject();
    }

}