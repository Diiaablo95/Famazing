package projctx.famazing.utility;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

import projctx.famazing.data.DAO;
import projctx.famazing.data.DAOEventListener;
import projctx.famazing.data.SQLRuntimeException;
import projctx.famazing.ui.CookieActivity;

public class PositionService extends Service implements DAOEventListener, LocationListener {

    private LocationManager locationManager;
    private DAO dao;

    private static LatLng position;

    public static boolean running;

    /**
     * Maximum distance within which the location of the user is considered to be the same.
     * If the user modifies is position of a distance greater then this, his position will be updated and other members will see it.
     */
    public static final float MIN_DISTANCE_LOCATION_REFRESH = 100;      //100 m

    public static final long MIN_TIME_LOCATION_REFRESH = 1000 * 20;     //60 s

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        dao = new DAO(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("TAG", "Positions service started!");
            running = true;
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_LOCATION_REFRESH, MIN_DISTANCE_LOCATION_REFRESH, this);
            Location lastPosition = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastPosition == null) {
                Log.d("TAG", "Position with GPS not available.");
                lastPosition = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (lastPosition != null) {
                Log.d("TAG", "Position with network available.");
                position = new LatLng(lastPosition.getLatitude(), lastPosition.getLongitude());
                if (!dao.isConnected()) {
                    dao.connect();
                } else {
                    onConnectionActions();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && locationManager != null) {
            locationManager.removeUpdates(this);
        }
        running = false;
        Log.d("TAG", "Position service destroyed!");
    }

    @Override
    public void handleConnectionEvent(@Nullable SQLRuntimeException e) {
        if (e == null) {
            onConnectionActions();
        }
    }

    private void onConnectionActions() {
        int userId = PreferenceManager.getDefaultSharedPreferences(this).getInt(CookieActivity.USER_ID_KEY, -1);
        if (userId != -1) {
            dao.updateUserLocation(userId, position);
        } else {
            dao.disconnect();
        }
    }

    @Override
    public void handleDisconnectionEvent(@Nullable SQLRuntimeException e) {}

    @Override
    public void handleResult(HashMap<String, Object> result) {
        Log.d("TAG", "Position updated!");
        if (dao.isConnected()) {
            dao.disconnect();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("TAG", "Location changed!");
        position = new LatLng(location.getLatitude(), location.getLongitude());
        if(!dao.isConnected()) {
            dao.connect();
        } else {
            onConnectionActions();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    public static LatLng getPosition() {
        return position;
    }
}
