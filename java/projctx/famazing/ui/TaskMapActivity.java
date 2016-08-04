package projctx.famazing.ui;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;

import projctx.famazing.data.DAO;
import projctx.famazing.data.Place;
import projctx.famazing.data.SQLRuntimeException;
import projctx.famazing.data.Task;

/**
 * Activity responsible of showing on the map the position of the task to complete.
 */
public class TaskMapActivity extends MapActivity {

    public static final String TASK_NAME_KEY = "task_name";
    public static final String TASK_DESCRIPTION_KEY = "task_description";
    public static final String TASK_PLACE_KEY = "task_place";

    private Task taskToComplete = new Task(null, null, -1, null, null, null, null, null, null);
    private LatLng taskLocation;

    public static final float ZOOM_ACTUAL_POSITION = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent generatingIntent = getIntent();
        if (generatingIntent != null) {
            taskToComplete.setName(generatingIntent.getStringExtra(TASK_NAME_KEY));
            taskToComplete.setDescription(generatingIntent.getStringExtra(TASK_DESCRIPTION_KEY));
            taskToComplete.setLocationId(generatingIntent.getIntExtra(TASK_PLACE_KEY, -1));
        }
    }

    @Override
    protected void onConnectActions() {
        int placeId = taskToComplete.getLocationId();
        if (placeId != -1) {
            dao.getPlace(placeId);
        }
    }

    @Override
    public void handleResult(HashMap<String, Object> result) {
        super.handleResult(result);
        Object res = result.get(DAO.RESULT_OBJECT_KEY);

        if (!(res instanceof SQLRuntimeException)) {
            int operationCode = (Integer) result.get(DAO.OPERATION_CODE_KEY);

            switch (operationCode) {
                case DAO.GET_PLACE_CODE: {
                    Place p = (Place) res;
                    taskLocation = p.getCoordinates();
                    if (map != null && !resultDisplayed) {
                        showTaskMarker();
                        resultDisplayed = true;
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        super.onMapReady(googleMap);
        if (taskLocation != null && !resultDisplayed) {
            showTaskMarker();
            resultDisplayed = true;
        }
    }

    private void showTaskMarker() {
        MarkerOptions options = new MarkerOptions().title(taskToComplete.getName()).position(taskLocation).visible(true);
        if (taskToComplete.getDescription() != null) {
            options.snippet(taskToComplete.getDescription());
        }
        map.addMarker(options).showInfoWindow();
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(taskLocation, ZOOM_ACTUAL_POSITION));
    }
}
