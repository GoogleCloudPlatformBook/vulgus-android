package ugia.vulgus.app.activity;

import java.util.List;
import java.util.UUID;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.gson.JsonObject;
import com.squareup.okhttp.Response;
import ugia.vulgus.api.data.Json;
import ugia.vulgus.api.data.object.ClassifiedLatLng;
import ugia.vulgus.api.request.Request;
import ugia.vulgus.api.util.LocationPoints;
import ugia.vulgus.app.R;
import ugia.vulgus.app.constant.Color;

public class MainActivity extends FragmentActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private final static int DEFAULT_ZOOM = 12;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private final static String INSTANCE_ID_KEY = "instance_id";

    private GoogleMap mMap;
    private View mRefreshButton;
    private Animator mRefreshButtonAnimator;

    private boolean isRefreshingMap;

    private LocationClient mLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLocationClient.disconnect();
    }

    private void initialize() {
        bindViews();
        mLocationClient = new LocationClient(this, this, this);
    }

    private void bindViews() {

        mRefreshButton = findViewById(R.id.main_button_refresh);
        setUpAnimator();

        setUpMap();
    }

    private void setUpAnimator() {

        mRefreshButtonAnimator = AnimatorInflater.loadAnimator(this, R.animator.rotation_animator);
        mRefreshButtonAnimator.setTarget(mRefreshButton);
        mRefreshButtonAnimator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                if (!isRefreshingMap) {
                    mRefreshButtonAnimator.end();
                }
            }
        });
    }

    private void setUpMap() {

        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        if (mMap != null) {
            mMap.getUiSettings().setZoomControlsEnabled(false);
        }
    }

    /**
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        refreshState();
    }

    /**
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Error: " + connectionResult.getErrorCode(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setRefreshingMap(final boolean refreshing) {

        isRefreshingMap = refreshing;

        mRefreshButton.post(new Runnable() {
            @Override
            public void run() {
                mRefreshButton.setEnabled(!refreshing);
                if (refreshing) {
                    mRefreshButtonAnimator.start();
                }
            }
        });
    }

    private void refreshState() {

        Location location = mLocationClient.getLastLocation();

        moveMapToLocation(location);
        fetchSurroundingData(location);
        //generateRandomSurroundingData(); Generates random points within a given area for debugging purposes

        pushUserLocation(location);
    }

    private void moveMapToLocation(Location location) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                location.getLongitude()), DEFAULT_ZOOM));
    }

    public void buttonClicked(View v) {
        switch (v.getId()) {
        case R.id.main_button_refresh:
            refreshState();
        }
    }

    /**
     * Pushes the current location of the user with the corresponding anonynomous instance id
     *
     * @param location The location to send to our api.
     */
    private void pushUserLocation(final Location location) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                JsonObject body = new JsonObject();
                body.addProperty("instance_id", getInstanceId());
                body.addProperty("location", String.format("%.6f,%.6f", location.getLatitude(),
                        location.getLongitude()));

                new Request().forUri("/location_status_search", Request.Method.PUT).withBody(body).execute();
            }
        }).start();
    }

    /**
     * Retrieves the surrounding location information for a given position in the map,
     * supposed to be the current user location.
     *
     * @param location The current location of the user.
     */
    private void fetchSurroundingData(final Location location) {

        setRefreshingMap(true);

        new Thread(new Runnable() {
            @Override
            public void run() {

                JsonObject body = new JsonObject();
                body.addProperty("current_location", String.format("%.6f,%.6f", location.getLatitude(),
                        location.getLongitude()));

                final Response response = new Request().forUri("/location_status_search",
                        Request.Method.POST).withBody(body).execute();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setRefreshingMap(false);
                        if (response.isSuccessful()) {
                            drawLocations((ClassifiedLatLng[]) Json.fromJson(response, ClassifiedLatLng[].class));
                        } else {
                            Toast.makeText(MainActivity.this, "There was a error trying to fetch new data. " +
                                    "Try again later", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * Generates a random set of points for debugging purposes.
     */
    private void generateRandomSurroundingData() {
        drawLocations(LocationPoints.generateRandomMap(52.511f, 52.537f, 13.393f, 13.442f, 80));
    }

    private void drawLocations(ClassifiedLatLng[] locationPoints) {
        mMap.clear();
        drawPointsInMap(locationPoints);
        //drawPolygonsInMap(locationPoints);
    }

    private void drawPointsInMap(ClassifiedLatLng[] points) {
        LocationPoints.groupByDistance(points);
        for (ClassifiedLatLng point : points) {
            mMap.addCircle(new CircleOptions().center(point.getLatLng()).radius(120).fillColor
                    (Color.STRATOS_COLORS[point.distanceGroup]).strokeWidth(0));
        }
    }

    private void drawPolygonsInMap(ClassifiedLatLng[] points) {
        List<PolygonOptions> polygonOptionsList = LocationPoints.asMapPolygonOptionsList(points);
        for (PolygonOptions polygonOptions : polygonOptionsList) {
            mMap.addPolygon(polygonOptions);
        }
    }

    /**
     * Retrieves or creates a unique and anonymous instance id to avoid location duplicates for the same user on
     * the server side. This instance id is stored on shared preferences for further re-use.
     *
     * @return the generated/stored instance id as a string
     */
    private String getInstanceId() {

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        String instanceId = preferences.getString(INSTANCE_ID_KEY, null);
        if (instanceId == null) {
            instanceId = UUID.randomUUID().toString();

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(INSTANCE_ID_KEY, instanceId);
            editor.commit();
        }

        return instanceId;
    }
}
