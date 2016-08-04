package projctx.famazing.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import projctx.famazing.R;
import projctx.famazing.data.Family.Membership;
import projctx.famazing.utility.AlertService;
import projctx.famazing.utility.FragmentUtility;
import projctx.famazing.utility.PositionService;
import projctx.famazing.utility.SettingsActivity;

/**
 * Main activity which contains all the main fragments that form the application.
 * It provides a central navigation drawer as well as the "settings" button.
 */
public class HomeActivity extends AppCompatActivity {

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private String[] features;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;

    private int viewSelectedIndex = -1;
    private static String logout_name;

    static int userId;
    static int familyId;
    static String userName;
    static Membership userMembership;

    public static final int LOCATION_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_layout);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        userId = prefs.getInt(CookieActivity.USER_ID_KEY, 1);
        familyId = prefs.getInt(CookieActivity.USER_FAMILY_KEY, 1);
        userName = prefs.getString(CookieActivity.USER_NAME_KEY, "James");
        userMembership = Membership.valueOf(prefs.getString(CookieActivity.USER_MEMBERSHIP_KEY, "DAD"));

        this.features = getResources().getStringArray(R.array.features);
        logout_name = this.features[features.length - 1];
        this.drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        this.drawerList = (ListView) findViewById(R.id.left_drawer);

        if (this.drawerList != null) {
            this.drawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, features));
            this.drawerList.setOnItemClickListener(new DrawerItemClickListener());
            this.drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, true, R.drawable.nav_drawer_indicator, R.string.drawer_open, R.string.drawer_close) {
                public void onDrawerClosed(View view) {
                    super.onDrawerClosed(view);
                }
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                }
            };
        }
        if (this.drawerLayout != null) {
            this.drawerLayout.setDrawerListener(drawerToggle);
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.nav_drawer_indicator);
            actionBar.setHomeButtonEnabled(true);
        }

        MainPageFragment mainPageFragment = new MainPageFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.mainContent, mainPageFragment).commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(Gravity.LEFT);
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStackImmediate("mainPageChange", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            super.onBackPressed();
        }
        if (viewSelectedIndex != -1) {
            drawerList.setItemChecked(viewSelectedIndex, false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (!PositionService.running) {
                        Intent positionServiceIntent = new Intent(this, PositionService.class);
                        startService(positionServiceIntent);
                    }
                    if (!AlertService.running) {
                        Intent alertServiceIntent = new Intent(this, AlertService.class);
                        //startService(alertServiceIntent);
                    }
                }
                break;
            }
            default: break;
        }
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
        if (features[position].equals(logout_name)) {
            logout();
        } else {
            Fragment fragment = FragmentUtility.getFragment(position);
            // Insert the fragment by replacing any existing fragment
            FragmentManager fragmentManager = getSupportFragmentManager();

            if (fragmentManager.getBackStackEntryCount() == 0) {        //If the fragment shown is the main page one
                //add the new fragment to the backstack (so that pressing back, user returns to the main page).
                fragmentManager.beginTransaction().replace(R.id.mainContent, fragment).addToBackStack("mainPageChange").commit();
            } else {
                //Otherwise there is just a change but pressing back the user always returns to the main page.
                fragmentManager.beginTransaction().replace(R.id.mainContent, fragment).addToBackStack(null).commit();
            }

            // Highlight the selected item, update the title, and close the drawer
            this.viewSelectedIndex = position;
            drawerList.setItemChecked(position, true);
        }
        drawerLayout.closeDrawer(drawerList);
    }

    private void logout() {
        deleteCookie();
        stopService(new Intent(this, AlertService.class));
        stopService(new Intent(this, PositionService.class));
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }

    private void deleteCookie() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.remove(CookieActivity.USER_ID_KEY);
        editor.remove(CookieActivity.USER_NAME_KEY);
        editor.remove(CookieActivity.USER_FAMILY_KEY);
        editor.remove(CookieActivity.USER_MEMBERSHIP_KEY);
        editor.remove(CookieActivity.EMERGENCY_NUMBER_KEY);
        editor.apply();
    }
}