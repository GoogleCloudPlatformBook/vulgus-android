package ugia.vulgus.api.data;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import ugia.vulgus.api.data.object.ClassifiedLatLng;

/**
 * Created by joseluisugia on 01/07/14.
 */
public class ClassifiedLatLngDeserializer implements JsonDeserializer<ClassifiedLatLng> {

    @Override
    public ClassifiedLatLng deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws
            JsonParseException {

        if (json == null) {
            return null;
        }

        JsonObject jsonObject = json.getAsJsonObject();
        if (jsonObject.has("lat") && jsonObject.has("lon")) {
            return new ClassifiedLatLng(jsonObject.get("lat").getAsFloat(), jsonObject.get("lon").getAsFloat());
        } else {
            return null;
        }
    }

}