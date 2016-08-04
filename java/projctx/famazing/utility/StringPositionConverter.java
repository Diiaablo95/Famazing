package projctx.famazing.utility;

import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

/**
 * Utility class able to transform objects representing locations (latitude-longitude) into a string with format suitable for the database.
 */
public final class StringPositionConverter {

    /**
     * Separator used to separate values of latitude and longitude in the location string.
     */
    public static final char COORDINATES_SEPARATOR = ';';

    /**
     * Return the string corresponding to the location value. String follows the format "LATITUDE;LONGITUDE".
     * @param location : the location to convert into string.
     * @return the string obtained by the transformation or null if location passed is null.
     */
    public static String convert(LatLng location) {
        String result = null;
        if (location != null) {
            result = String.format(Locale.ENGLISH, "%f%c%f", location.latitude, COORDINATES_SEPARATOR, location.longitude);
        }
        return result;
    }

    /**
     * Return the location corresponding to the string value. String follows the format "LATITUDE;LONGITUDE".
     * @param stringLocation : the string containing the two values of latitude and longitude separated by the <a href = "COORDINATES_SEPARATOR">separator</a>.
     * @return the couple of coordinates obtained by the transformation or null if string passed is null.
     */
    public static LatLng convert(String stringLocation) {
        LatLng result = null;

        if (stringLocation != null) {
            int splitIndex = stringLocation.indexOf(COORDINATES_SEPARATOR);
            int stringLength = stringLocation.length();

            try {
                double latitude = Double.valueOf(stringLocation.substring(0, splitIndex));
                double longitude = Double.valueOf(stringLocation.substring(splitIndex + 1, stringLength));

                result = new LatLng(latitude, longitude);
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }
}
