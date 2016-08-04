package projctx.famazing.ui;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.sql.Date;
import java.util.HashMap;

import projctx.famazing.data.DAO;
import projctx.famazing.data.SQLRuntimeException;
import projctx.famazing.data.User;

/**
 * Activity responsible of showing on the map the position of an alert chosen from the relative list.
 */
public class AlertMapActivity extends MapActivity {

    /**
     * Key used to store/get in/from the intent the id of the user who generated the alert.
     */
    public static final String USER_ID_INTENT_KEY = "user_id";

    /**
     * Key used to store/get in/from the intent the date of the generated alert.
     */
    public static final String ALERT_DATE_INTENT_KEY = "date";

    /**
     * Key used to store/get in/from the intent the location of the generated alert.
     */
    public static final String ALERT_LOCATION_INTENT_KEY = "location";

    private int userId;
    private String userName;
    private Date alertDate;
    private LatLng alertLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent sendingIntent = getIntent();
        if (sendingIntent != null) {
            userId = sendingIntent.getIntExtra(USER_ID_INTENT_KEY, 1);
            alertDate = (Date) sendingIntent.getSerializableExtra(ALERT_DATE_INTENT_KEY);
            double[] locationCoordinates = sendingIntent.getDoubleArrayExtra(ALERT_LOCATION_INTENT_KEY);
            alertLocation = new LatLng(locationCoordinates[0], locationCoordinates[1]);
        }
    }

    @Override
    protected void onConnectActions() {
        dao.getUser(userId);
    }

    @Override
    public void handleResult(HashMap<String, Object> result) {
        super.handleResult(result);
        Object res = result.get(DAO.RESULT_OBJECT_KEY);

        if (!(res instanceof SQLRuntimeException)) {
            int operationCode = (Integer) result.get(DAO.OPERATION_CODE_KEY);

            switch (operationCode) {
                case DAO.GET_USER_CODE: {
                    User user = (User) res;
                    userName = user.getName();
                    if (map != null && !resultDisplayed) {
                        showAlarmMarker();
                        resultDisplayed = true;
                    }
                    break;
                }
            }
        } else {
            if (dao.isConnected()) {
                dao.disconnect();
            }
            showErrorView();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        super.onMapReady(googleMap);
        if (userName != null && !resultDisplayed) {
            showAlarmMarker();
            resultDisplayed = true;
        }
    }

    private void showAlarmMarker() {
        map.addMarker(new MarkerOptions().title(userName).snippet(alertDate.toString()).position(alertLocation).visible(true)).showInfoWindow();
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(alertLocation, MapActivity.DEFAULT_ZOOM));
    }
}
