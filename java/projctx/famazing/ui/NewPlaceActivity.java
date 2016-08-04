package projctx.famazing.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import projctx.famazing.data.DAO;
import projctx.famazing.data.Place;
import projctx.famazing.data.SQLRuntimeException;

/**
 * Activity responsible of showing on the map the position of the places already pinned by the family members.
 * It gives also possibility to pin new places and give them a name.
 */
public class NewPlaceActivity extends MapActivity implements GoogleMap.OnMapLongClickListener {

    private MarkerOptions markerOptionsToCreate;
    private Set<Place> familyPlaces;

    public static final String PLACES_NAMES_KEY = "family_places_names";
    public static final String PLACES_COORDINATES_KEY = "family_places_coordinates";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent generatingIntent = getIntent();
        //It's the only way to pass LatLong parameters to an intent!!!!!
        if (generatingIntent != null) {
            String[] placesNames = generatingIntent.getStringArrayExtra(PLACES_NAMES_KEY);
            double[] placesCoordinates = generatingIntent.getDoubleArrayExtra(PLACES_COORDINATES_KEY);
            familyPlaces = new HashSet<>();
            for (int i = 0, j = 0; i < placesNames.length; i++) {
                Place place = new Place(null, placesNames[i], new LatLng(placesCoordinates[j++], placesCoordinates[j++]), HomeActivity.familyId);
                familyPlaces.add(place);
            }
        }
    }

    @Override
    public void handleResult(HashMap<String, Object> result) {
        super.handleResult(result);
        Object res = result.get(DAO.RESULT_OBJECT_KEY);

        if (!(res instanceof SQLRuntimeException)) {
            int operationCode = (Integer) result.get(DAO.OPERATION_CODE_KEY);

            switch (operationCode) {
                case DAO.CREATE_PLACE_CODE: {
                    int creationResult = (int) res;
                    if (map != null && markerOptionsToCreate != null && creationResult != -1) {
                        Marker userMarker = map.addMarker(markerOptionsToCreate);
                        userMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                        userMarker.showInfoWindow();
                        Toast.makeText(this, "New place added to the list!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Cannot create the place. Please try again.", Toast.LENGTH_SHORT).show();
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
    public void onMapLongClick(final LatLng latLng) {
        final EditText placeNameEditText = new EditText(this);
        Log.d("TAG", "Long click recognized");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog;
        builder.setTitle("Add a new place to the family places!")
                .setMessage("Give this place a name so that other members know what does it mean")
                .setView(placeNameEditText)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newPlaceName = placeNameEditText.getText().toString();
                        if (newPlaceName.length() == 0) {
                            Toast.makeText(NewPlaceActivity.this, "Place name must be at least one character long", Toast.LENGTH_SHORT).show();
                        } else {
                            Place newPlace = new Place(null, newPlaceName, latLng, HomeActivity.familyId);
                            Log.d("TAG", "Place creation started");
                            markerOptionsToCreate = new MarkerOptions().title(newPlaceName).position(latLng).visible(true);
                            dao.createNewPlace(newPlace);
                        }
                    }
                });
        dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        super.onMapReady(googleMap);
        map.setOnMapLongClickListener(this);
        if (familyPlaces != null) {
            Marker lastMarker = null;
            for (Place place : familyPlaces) {
                lastMarker = map.addMarker(new MarkerOptions().title(place.getName()).position(place.getCoordinates()));
            }
            if (lastMarker != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(lastMarker.getPosition(), MapActivity.DEFAULT_ZOOM));
            }
        }
    }

    @Override
    protected void onConnectActions() {}
}
