package ugia.vulgus.api.data.object;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by joseluisugia on 05/07/14.
 */
public class ClassifiedLatLng {

    private static final int INVALID_GROUP = -1;

    public final LatLng latLng;
    public int distanceGroup;

    public ClassifiedLatLng(double latitude, double longitude) {
        this(new LatLng(latitude, longitude));
    }

    public ClassifiedLatLng(LatLng latLng) {
        this(latLng, INVALID_GROUP);
    }

    public ClassifiedLatLng(LatLng latLng, int distanceGroup) {
        this.latLng = latLng;
        this.distanceGroup = distanceGroup;
    }

    public double getLatitude() {
        return latLng.latitude;
    }

    public double getLongitude() {
        return latLng.longitude;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public boolean belongsToGroup() {
        return distanceGroup > INVALID_GROUP;
    }

    public void applyDistanceGroup(int distanceGroup) {
        if (!belongsToGroup() || distanceGroup < this.distanceGroup) {
            this.distanceGroup = distanceGroup;
        }
    }
}
