package projctx.famazing.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.HashMap;

import projctx.famazing.R;
import projctx.famazing.data.DAO;
import projctx.famazing.data.DAOEventListener;
import projctx.famazing.data.SQLRuntimeException;

public abstract class MapActivity extends AppCompatActivity implements DAOEventListener, OnMapReadyCallback {

    protected GoogleMap map;
    protected boolean resultDisplayed = false;
    protected ProgressBar loadingSpinner;

    protected DAO dao;

    public static final float DEFAULT_ZOOM = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_map);
        dao = new DAO(this);
        loadingSpinner = (ProgressBar) findViewById(R.id.loadingSpinner);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        MapsInitializer.initialize(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!dao.isConnected()) {
            Log.w("TAG", "AlertMapActivity dao not connected");
            dao.connect();
        } else {
            Log.w("TAG", "AlertMapActivity dao connected");
            onConnectActions();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (dao.isConnected()) {
            dao.disconnect();
        }
    }

    @Override
    public void handleConnectionEvent(@Nullable SQLRuntimeException e) {
        if (e == null) {
            onConnectActions();
        } else {
            showErrorView();
        }
    }

    protected abstract void onConnectActions();

    @Override
    public void handleDisconnectionEvent(@Nullable SQLRuntimeException e) {}

    @Override
    public void handleResult(HashMap<String, Object> result) {
        if (dao.isConnected()) {
            dao.disconnect();
        }
    }

    protected void showErrorView() {
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(View.GONE);
        }
        new AlertDialog.Builder(this).setMessage("NO CONNECTION").setMessage("No connection. Please try again later")
                .setNeutralButton("RETRY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        Intent restartIntent = new Intent(MapActivity.this, getClass());
                        startActivity(restartIntent);
                        finish();
                    }
                })
                .setNegativeButton("BACK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        finish();
                    }
                }).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }
        loadingSpinner.setVisibility(View.GONE);
    }
}
