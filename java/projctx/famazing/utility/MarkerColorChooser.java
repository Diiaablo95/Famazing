package projctx.famazing.utility;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import projctx.famazing.data.Family;

/**
 * Utility class used to get the right marker color depending on the membership of the user whose position is getting shown.
 */
public class MarkerColorChooser {

    public static float colorFromMembership(Family.Membership membership) {
        float color;

        switch (membership) {
            case DAD: {
                color = BitmapDescriptorFactory.HUE_ORANGE;
                break;
            }
            case MOM: {
                color = BitmapDescriptorFactory.HUE_RED;
                break;
            }
            case SON: {
                color = BitmapDescriptorFactory.HUE_AZURE;
                break;
            }
            case DAUGHTER: {
                color = BitmapDescriptorFactory.HUE_ROSE;
                break;
            }
            default : color = -1;
        }

        return color;
    }
}
