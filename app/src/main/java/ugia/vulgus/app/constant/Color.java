package ugia.vulgus.app.constant;

/**
 * Created by joseluisugia on 07/07/14.
 */
public class Color {

    public static final String POLYGON_BLUE = "#2982e6";
    public static final String POLYGON_YELLOW = "#dabe02";
    public static final String POLYGON_ORANGE = "#da6302";
    public static final String POLYGON_RED = "#e73411";

    public static final int[] STRATOS_COLORS = new int[] {
            android.graphics.Color.parseColor(POLYGON_RED),
            android.graphics.Color.parseColor(POLYGON_ORANGE),
            android.graphics.Color.parseColor(POLYGON_YELLOW),
            android.graphics.Color.parseColor(POLYGON_BLUE) };

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00ffffff);
    }
}
