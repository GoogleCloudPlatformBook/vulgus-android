package ugia.vulgus.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import ugia.vulgus.api.data.object.ClassifiedLatLng;
import ugia.vulgus.app.BuildConfig;
import ugia.vulgus.app.constant.Color;

/**
 * Created by joseluisugia on 05/07/14.
 * TODO This logic belongs to backend. Currently outsourced to clients to avoid extra costs.
 */
public class LocationPoints {

    private static final int MIN_POLYGON_VERTICES = 3;

    private static final int EARTH_MAX_LATITUDE = 90;
    private static final int EARTH_MIN_LATITUDE = -90;
    private static final int EARTH_MAX_LATITUDE_GAP = EARTH_MAX_LATITUDE - EARTH_MIN_LATITUDE;

    private static final int ORIENTATION_COLINEAR = 0;
    private static final int ORIENTATION_CLOCKWISE = 1;
    private static final int ORIENTATION_COUNTERCLOCKWISE = 2;

    private static final float ROUGH_FIFTY_M_DEGREES_MODULE = 0.0005f;
    private static final float ROUGH_TWO_HUNDRED_M_DEGREES_MODULE = 0.002f;
    private static final float ROUGH_TWO_KM_DEGREES_MODULE = 0.02f;

    public static List<PolygonOptions> asMapPolygonOptionsList(ClassifiedLatLng[] locationPoints) {

        groupByDistance(locationPoints);

        ArrayList<ClassifiedLatLng> locationPointsList = new ArrayList<ClassifiedLatLng>(Arrays.asList
                (locationPoints));

        List<PolygonOptions> polygonOptionsList = new ArrayList<PolygonOptions>();
        while (locationPointsList.size() > MIN_POLYGON_VERTICES) {

            List<LatLng> polygonVertices = new ArrayList<LatLng>();

            // Retrieve edge point
            ClassifiedLatLng initialVertex, currentVertex;
            initialVertex = currentVertex = getTopNorthPoint(locationPointsList);
            polygonVertices.add(currentVertex.getLatLng());

            int currentDistanceGroup = initialVertex.distanceGroup;
            LatLng currentVector = new LatLng(0, 0);

            while (!currentVertex.equals(initialVertex) || polygonVertices.size() == 1) {

                double currentMaxAngle = -180;
                double distanceConstraint = EARTH_MAX_LATITUDE_GAP;
                int currentMaxAngleIndex = -1;

                int size = locationPointsList.size();
                for (int i = 0; i < size; i++) {

                    ClassifiedLatLng testPoint = locationPointsList.get(i);
                    if (testPoint.equals(currentVertex)) {
                        continue;
                    }

                    double pointSeparation = distanceAmongPoints(currentVertex, testPoint);
                    if (pointSeparation <= distanceConstraint) {

                        LatLng testVector = vectorForPoints(currentVertex.getLatLng(), testPoint.getLatLng());
                        double testAngle = angleForVectors(currentVector, testVector);
                        if (Math.abs(testAngle) == 180) {
                            continue; // No turning back
                        }

                        if (testAngle > currentMaxAngle) {
                            if (testPoint.distanceGroup == currentDistanceGroup) {
                                currentMaxAngleIndex = i;
                                currentMaxAngle = testAngle;
                            } else {
                                distanceConstraint = pointSeparation;
                            }
                        }
                    }
                }

                if (currentMaxAngleIndex > -1) {

                    ClassifiedLatLng nextVertex = locationPointsList.remove(currentMaxAngleIndex);
                    if (BuildConfig.DEBUG) {
                        Log.v("Angle between vectors: ", Double.toString(angleForVectors(currentVector,
                                vectorForPoints(currentVertex.getLatLng(), nextVertex.getLatLng()))));
                    }
                    polygonVertices.add(nextVertex.getLatLng());
                    currentVector = vectorForPoints(currentVertex.getLatLng(), nextVertex.getLatLng());
                    currentVertex = nextVertex;

                } else if (polygonVertices.size() <= MIN_POLYGON_VERTICES) {
                    polygonVertices = new ArrayList<LatLng>();
                    locationPointsList.remove(currentVertex);
                    if (locationPointsList.size() > 0) {
                        currentVertex = getTopNorthPoint(locationPointsList);
                    } else {
                        break;
                    }
                } else {
                    break; // Finish polygon
                }
            }

            if (polygonVertices.size() > MIN_POLYGON_VERTICES) {

                int densityColor = Color.withAlpha(Color.STRATOS_COLORS[currentDistanceGroup], 85);
                PolygonOptions polygonOptions = new PolygonOptions().strokeColor(densityColor).strokeWidth(2)
                                                                    .fillColor(densityColor);

                polygonOptionsList.add(polygonOptions.addAll(polygonVertices));
                removePointsWithinPolygon(locationPointsList, polygonVertices);
            }
        }

        return polygonOptionsList;
    }

    private static LatLng vectorForPoints(LatLng pointA, LatLng pointB) {
        return new LatLng(pointB.latitude - pointA.latitude, pointB.longitude - pointA.longitude);
    }

    private static double angleForVectors(LatLng vectorA, LatLng vectorB) {
        return Math.toDegrees(Math.atan2(vectorB.latitude, vectorB.longitude) -
                Math.atan2(vectorA.latitude, vectorA.longitude));
    }

    public static void groupByDistance(ClassifiedLatLng[] locationPoints) {

        int size = locationPoints.length;
        for (int i = 0; i < size - 1; ++i) {

            ClassifiedLatLng currentPoint = locationPoints[i];
            ClassifiedLatLng closestPoint = locationPoints[i + 1];
            double closestPointDistance = distanceAmongPoints(currentPoint, closestPoint);

            for (int j = i + 2; j < size; ++j) {

                ClassifiedLatLng testingPoint = locationPoints[j];
                double distance;

                if ((distance = distanceAmongPoints(currentPoint, testingPoint)) < closestPointDistance) {
                    closestPointDistance = distance;
                    closestPoint = testingPoint;
                }
            }

            int closestStratum = stratumForDistance(closestPointDistance);
            currentPoint.applyDistanceGroup(closestStratum);
            closestPoint.applyDistanceGroup(closestStratum);
        }
    }

    private static ClassifiedLatLng getTopNorthPoint(List<ClassifiedLatLng> locationPoints) {

        double topLatitude = EARTH_MIN_LATITUDE;
        ClassifiedLatLng topNorthPoint = null;

        int size = locationPoints.size();
        for (int i = 0; i < size; ++i) {

            ClassifiedLatLng currentPoint = locationPoints.get(i);
            if (currentPoint.getLatitude() > topLatitude) {
                topNorthPoint = currentPoint;
                topLatitude = currentPoint.getLatitude();
            }
        }

        return topNorthPoint;
    }

    private static void removePointsWithinPolygon(List<ClassifiedLatLng> points, List<LatLng> polygon) {

        Iterator<ClassifiedLatLng> iterator = points.iterator();
        while (iterator.hasNext()) {
            if (isPointInsidePolygon(polygon, iterator.next().getLatLng())) {
                iterator.remove();
            }
        }
    }

    private static boolean isPointInsidePolygon(List<LatLng> polygon, LatLng p) {

        int verticesCount = polygon.size();

        if (verticesCount < 3) {
            return false;
        }

        // Create a point for line segment from p to infinite
        LatLng extreme = new LatLng(181, p.latitude);

        // Count intersections of the above line with sides of polygon
        int count = 0, i = 0;
        do {
            int next = (i + 1) % verticesCount;

            LatLng currentVertex = polygon.get(i);
            LatLng nextVertex = polygon.get(next);

            if (doVectorsIntersect(currentVertex, nextVertex, p, extreme)) {

                if (orientationForPointInVector(currentVertex, p, nextVertex) == 0) {
                    return isPointOnSegment(currentVertex, p, nextVertex);
                }

                count++;
            }
            i = next;
        } while (i != 0);

        // Return true if count is odd, false otherwise
        return (count & 1) == 1;
    }

    private static boolean doVectorsIntersect(LatLng p1, LatLng q1, LatLng p2, LatLng q2) {

        // Find the four orientations needed for general and
        // special cases
        int o1 = orientationForPointInVector(p1, q1, p2);
        int o2 = orientationForPointInVector(p1, q1, q2);
        int o3 = orientationForPointInVector(p2, q2, p1);
        int o4 = orientationForPointInVector(p2, q2, q1);

        // General case
        if (o1 != o2 && o3 != o4) {
            return true;
        }

        // Special Cases
        // p1, q1 and p2 are colinear and p2 lies on segment p1q1
        if (o1 == ORIENTATION_COLINEAR && isPointOnSegment(p1, p2, q1)) {
            return true;
        }

        // p1, q1 and p2 are colinear and q2 lies on segment p1q1
        if (o2 == ORIENTATION_COLINEAR && isPointOnSegment(p1, q2, q1)) {
            return true;
        }

        // p2, q2 and p1 are colinear and p1 lies on segment p2q2
        if (o3 == ORIENTATION_COLINEAR && isPointOnSegment(p2, p1, q2)) {
            return true;
        }

        // p2, q2 and q1 are colinear and q1 lies on segment p2q2
        if (o4 == ORIENTATION_COLINEAR && isPointOnSegment(p2, q1, q2)) {
            return true;
        }

        return false; // Doesn't fall in any of the above cases
    }

    private static int orientationForPointInVector(LatLng p, LatLng q, LatLng r) {

        int orientation = (int) ((q.latitude - p.latitude) * (r.longitude - q.longitude) -
                (q.longitude - p.longitude) * (r.latitude - q.latitude));

        if (orientation == 0) {
            return ORIENTATION_COLINEAR;
        }

        return (orientation > 0) ? ORIENTATION_CLOCKWISE : ORIENTATION_COUNTERCLOCKWISE;
    }

    private static boolean isPointOnSegment(LatLng p, LatLng q, LatLng r) {

        return q.longitude <= Math.max(p.longitude, r.longitude)
                && q.longitude >= Math.min(p.longitude, r.longitude)
                && q.latitude <= Math.max(p.latitude, r.latitude)
                && q.latitude >= Math.min(p.latitude, r.latitude);
    }

    private static double distanceAmongPoints(ClassifiedLatLng pointA, ClassifiedLatLng pointB) {
        // Note: Ignoring cos adjustment to longitude as it is negligible within such small distances
        return Math.sqrt(
                Math.pow(pointB.getLatitude() - pointA.getLatitude(), 2) +
                        Math.pow(pointB.getLongitude() - pointA.getLongitude(), 2)
        );
    }

    private static int stratumForDistance(double distance) {
        if (distance < ROUGH_FIFTY_M_DEGREES_MODULE) {
            return 0;
        } else if (distance < ROUGH_TWO_HUNDRED_M_DEGREES_MODULE) {
            return 1;
        } else if (distance < ROUGH_TWO_KM_DEGREES_MODULE) {
            return 2;
        } else {
            return 3;
        }
    }

    public static ClassifiedLatLng[] generateRandomMap(float latMin, float latMax, float lonMin, float lonMax,
                                                       int n) {

        float latRange = latMax - latMin;
        float lonRange = lonMax - lonMin;

        ClassifiedLatLng[] randomPoints = new ClassifiedLatLng[n];

        for (int i = 0; i < n; i++) {
            randomPoints[i] = new ClassifiedLatLng(latMin + (Math.random() * latRange),
                    lonMin + (Math.random() * lonRange));
        }

        return randomPoints;
    }
}