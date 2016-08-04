package projctx.famazing.ui;

import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.location.LocationListener;
import android.widget.ProgressBar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import projctx.famazing.R;
import projctx.famazing.data.DAO;
import projctx.famazing.data.DAOEventListener;
import projctx.famazing.data.SQLRuntimeException;
import projctx.famazing.data.User;
import projctx.famazing.utility.MarkerColorChooser;
import projctx.famazing.utility.PositionService;

/**
 * Fragment responsible of showing the positions of all the family members on a map.
 * Users' positions are refreshed at fixed time intervals.
 */
public class PositionsFragment extends Fragment implements OnMapReadyCallback, DAOEventListener {

    /**
     * Interval of time in milliseconds between two successive fetches of family members' locations.
     */
    public static final long POSITIONS_REFRESH_RATE = 1000 * 30;        //30 s

    public static final float ZOOM_ACTUAL_POSITION = 15;

    private static GoogleMap map;
    private FragmentActivity activity;
    private static View viewToShow;

    private static PositionsFragment self;

    private DAO dao;
    private HashMap<User, LatLng> locations;
    private boolean resultDisplayed = false;
    private ProgressBar loadingSpinner;
    private Marker myMarker;
    private MarkerOptions myMarkerOptions = new MarkerOptions().title("You").snippet("Your location").visible(true);
    private HashMap<User, Marker> familyMarkers = new HashMap<>();

    private Timer timer;
    private TimerTask timerTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("TAG", "PositionsFragment onCreate()");
        super.onCreate(savedInstanceState);
        activity = getActivity();
        dao = new DAO(this);

        if (self == null) {
            self = this;
        }
        MapsInitializer.initialize(activity);
    }

    //TODO: FIX BUG THAT HAPPENS WHEN MAP IS OPENED A SECOND TIME
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("TAG", "PositionsFragment onCreateView()");
        if (viewToShow == null) {
            viewToShow = inflater.inflate(R.layout.content_positions_fragment, container, false);
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.location_fragment_title));
        }
        myMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        loadingSpinner = (ProgressBar) viewToShow.findViewById(R.id.loadingSpinner);

        return viewToShow;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!dao.isConnected()) {
            Log.w("TAG", "PositionsFragment dao not connected");
            dao.connect();
        } else {
            Log.w("TAG", "PositionsFragment dao connected");
            onConnectActions();
        }
    }

    @Override
    public void onStop() {
        Log.d("TAG", "PositionsFragment onStop()");
        super.onStop();
        if (dao.isConnected()) {
            dao.disconnect();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("TAG", "PositionsFragment onMapReady()");
        map = googleMap;
        if (locations != null && !resultDisplayed) {
            displayLocations();
        }
    }

    @Override
    public void handleConnectionEvent(SQLRuntimeException e) {
        if (e == null) {
            Log.d("TAG", "PositionsFragment handleConnectionEvent() with no error");
            onConnectActions();
        } else {
            Log.d("TAG", "PositionsFragment handleConnectionEvent() with error");
            showErrorView();
        }
    }

    @Override
    public void handleDisconnectionEvent(SQLRuntimeException e) {
        if (e == null) {
            Log.d("TAG", "PositionsFragment handleDisconnectionEvent() with no error");
            if (timer != null) {
                timer.cancel();
                timer = null;
                timerTask = null;
            }
        } else {
            Log.d("TAG", "PositionsFragment handleDisconnectionEvent() with error");
        }
    }

    @Override
    public void handleResult(HashMap<String, Object> result) {
        Object res = result.get(DAO.RESULT_OBJECT_KEY);

        if (!(res instanceof SQLRuntimeException)) {
            int operationCode = (Integer) result.get(DAO.OPERATION_CODE_KEY);

            switch(operationCode) {
                case DAO.GET_MEMBERS_LOCATIONS_CODE: {
                    locations = (HashMap<User, LatLng>) res;
                    if (map != null) {
                        resultDisplayed = true;
                        displayLocations();
                    }
                    break;
                }
            }
        } else {
            Log.d("TAG", "PositionsFragment handleResult() with error");
            if (dao.isConnected()) {
                dao.disconnect();
            }
            showErrorView();
        }
    }

    private void showErrorView() {
        new AlertDialog.Builder(activity).setMessage("NO CONNECTION").setMessage("No connection. Please try again later")
                .setNeutralButton("RETRY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        getFragmentManager().beginTransaction().detach(self).attach(self).commit();
                    }
                })
                .setNegativeButton("BACK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        activity.getSupportFragmentManager().popBackStack();
                    }
                }).setCancelable(false).show();
    }

    private void onConnectActions() {
        Log.d("TAG", "PositionsFragment onConnectActions()");
        timerTask = new TimerTask() {
            @Override
            public void run() {
                //Update in real time also other member positions
                dao.getMembersLastLocations(HomeActivity.userId);
            }
        };
        timer = new Timer();
        timer.schedule(timerTask, 0, POSITIONS_REFRESH_RATE);
    }

    private void displayLocations() {
        Log.d("TAG", "PositionsFragment displayLocations()");
        map.clear();        //Remove all previous markers on the map
        for (User user : locations.keySet()) {
            LatLng userLocation = locations.get(user);
            Marker previousMarker = familyMarkers.get(user);
            //If previous marker refers to a different position, then remove and add the new one.
            //If there was no marker for that user, add a new marker.
            //(Implicit) if marker refers to the same user's previous position, then do nothing.
            if (previousMarker != null && (previousMarker.getPosition().longitude != userLocation.longitude || previousMarker.getPosition().latitude != userLocation.latitude)) {
                previousMarker.remove();
                Marker newMarker = map.addMarker(new MarkerOptions().title(user.getName()).snippet(user.getMembership().toString()).position(userLocation));
                newMarker.setIcon(BitmapDescriptorFactory.defaultMarker(MarkerColorChooser.colorFromMembership(user.getMembership())));
                familyMarkers.put(user, newMarker);
            } else if (previousMarker == null) {
                Marker newMarker = map.addMarker(new MarkerOptions().title(user.getName()).snippet(user.getMembership().toString()).position(userLocation));
                newMarker.setIcon(BitmapDescriptorFactory.defaultMarker(MarkerColorChooser.colorFromMembership(user.getMembership())));
                familyMarkers.put(user, newMarker);
            }
        }
        LatLng lastPosition = PositionService.getPosition();
        if (lastPosition != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, ZOOM_ACTUAL_POSITION));
            myMarker = map.addMarker(myMarkerOptions.position(lastPosition));
            myMarker.setIcon(BitmapDescriptorFactory.defaultMarker(MarkerColorChooser.colorFromMembership(HomeActivity.userMembership)));
            myMarker.showInfoWindow();
        }
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(ProgressBar.GONE);
        }
        resultDisplayed = true;
    }
}