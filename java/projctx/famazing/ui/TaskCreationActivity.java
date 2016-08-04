package projctx.famazing.ui;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import java.sql.Date;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import projctx.famazing.R;
import projctx.famazing.data.DAO;
import projctx.famazing.data.DAOEventListener;
import projctx.famazing.data.Place;
import projctx.famazing.data.SQLRuntimeException;
import projctx.famazing.data.Task;
import projctx.famazing.data.User;
import projctx.famazing.utility.DateEditText;

/**
 * Activity responsible of showing to user the form to create a new task.
 */
public class TaskCreationActivity extends AppCompatActivity implements DAOEventListener {

    private DAO dao;

    private ArrayList<Place> familyPlaces;
    private List<AbstractMap.SimpleEntry<Integer, String>> id_name_members;

    private EditText nameEditText;
    private NumberPicker doerPicker;
    private DateEditText dateEditText;
    private NumberPicker locationPicker;
    private EditText descriptionEditText;
    private Button createTaskButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_creation);
        dao = new DAO(this);

        Calendar c = Calendar.getInstance();
        final int day = c.get(Calendar.DAY_OF_MONTH);
        final int month = c.get(Calendar.MONTH);
        final int year = c.get(Calendar.YEAR);

        doerPicker = (NumberPicker) findViewById(R.id.doerPicker);
        dateEditText = (DateEditText) findViewById(R.id.date_editText);
        locationPicker = (NumberPicker) findViewById(R.id.locationPicker);
        nameEditText = (EditText) findViewById(R.id.nameEditText);
        descriptionEditText = (EditText) findViewById(R.id.descriptionEditText);
        createTaskButton = (Button) findViewById(R.id.create_task_button);

        if (doerPicker != null) {
            doerPicker.setMinValue(0);
            doerPicker.setMaxValue(0);
            doerPicker.setDisplayedValues(new String[]{getString(R.string.loading_message)});
        }

        if (locationPicker != null) {
            locationPicker.setMinValue(0);
            locationPicker.setMaxValue(0);
            locationPicker.setDisplayedValues(new String[]{getString(R.string.loading_message)});
        }

        if (dateEditText != null) {
            dateEditText.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(final View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        int yearToShow = year;
                        int monthToShow = month;
                        int dayToShow = day;
                        if (dateEditText.isDateComplete()) {
                            yearToShow = dateEditText.getYear();
                            monthToShow = dateEditText.getMonth() - 1;
                            dayToShow = dateEditText.getDay();
                        }
                        new DatePickerDialog(TaskCreationActivity.this, new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                 ((DateEditText) v).setText(String.format(Locale.ENGLISH, "%d-%02d-%02d", year, monthOfYear + 1, dayOfMonth));
                            }
                        }, yearToShow, monthToShow, dayToShow).show();
                    }
                    return true;
                }
            });
            dateEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(final View v, boolean hasFocus) {
                    if (hasFocus) {
                        int yearToShow = year;
                        int monthToShow = month;
                        int dayToShow = day;
                        if (dateEditText.isDateComplete()) {
                            yearToShow = dateEditText.getYear();
                            monthToShow = dateEditText.getMonth() - 1;
                            dayToShow = dateEditText.getDay();
                        }
                        new DatePickerDialog(TaskCreationActivity.this, new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                ((DateEditText) v).setText(String.format(Locale.ENGLISH, "%d-%02d-%d", year, monthOfYear + 1, dayOfMonth));
                            }
                        }, yearToShow, monthToShow, dayToShow).show();
                    }
                }
            });
        }

        Button newPlaceButton = (Button) findViewById(R.id.newPlaceButton);
        if (newPlaceButton != null) {
            newPlaceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent placeCreationIntent = new Intent(TaskCreationActivity.this, NewPlaceActivity.class);
                    if (familyPlaces != null) {
                        String[] names = new String[familyPlaces.size() - 1];
                        double[] coordinates = new double[(familyPlaces.size() - 1) * 2];
                        for (int i = 1, j = 0; i < familyPlaces.size(); i++) {  //i = 1 cause we discard the dummy "UNDEFINED" place
                            Place place = familyPlaces.get(i);
                            names[i - 1] = place.getName();
                            coordinates[j++] = place.getCoordinates().latitude;
                            coordinates[j++] = place.getCoordinates().longitude;
                        }
                        placeCreationIntent.putExtra(NewPlaceActivity.PLACES_NAMES_KEY, names);
                        placeCreationIntent.putExtra(NewPlaceActivity.PLACES_COORDINATES_KEY, coordinates);
                    }
                    startActivity(placeCreationIntent);
                }
            });
        }

        if (createTaskButton != null) {
            createTaskButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String taskName = nameEditText.getText().toString();
                    String deadlineText = dateEditText.getText().toString();
                    if (taskName.length() == 0) {
                        nameEditText.setError("Please enter a name for the task.");
                    } else {
                        if (deadlineText.length() > 0) {
                            try {
                                int day = dateEditText.getDay();
                                int month = dateEditText.getMonth() - 1;
                                int year = dateEditText.getYear();
                                GregorianCalendar deadline = new GregorianCalendar(year, month, day);
                                GregorianCalendar actualDate = new GregorianCalendar();
                                actualDate.set(Calendar.MILLISECOND, 0);
                                actualDate.set(Calendar.SECOND, 0);
                                actualDate.set(Calendar.MINUTE, 0);
                                actualDate.set(Calendar.HOUR_OF_DAY, 0);
                                if (deadline.before(actualDate)) {
                                    dateEditText.setError("Data cannot be past.");
                                    dateEditText.requestFocus();
                                } else {
                                    Task taskToCreate = new Task(null, taskName, HomeActivity.userId, id_name_members.get(doerPicker.getValue()).getKey(), null,
                                            Date.valueOf(dateEditText.getText().toString()), familyPlaces.get(locationPicker.getMaxValue()).getId(),
                                            descriptionEditText.getText().toString(), Task.Status.PENDING);
                                    dao.createTask(taskToCreate);
                                }
                            } catch (IllegalArgumentException ignored) {
                                dateEditText.setError("Data format must be the following: \"YYYY-MM-DD\"");
                                dateEditText.requestFocus();
                            }
                        } else {
                            Task taskToCreate = new Task(null, taskName, HomeActivity.userId, id_name_members.get(doerPicker.getValue()).getKey(), null,
                                    null, familyPlaces.get(locationPicker.getMaxValue()).getId(),
                                    descriptionEditText.getText().toString(), Task.Status.PENDING);
                            dao.createTask(taskToCreate);
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!dao.isConnected()) {
            Log.w("TAG", "TaskCreationActivity dao not connected");
            dao.connect();
        } else {
            Log.w("TAG", "TaskCreationActivity dao connected");
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

    private void onConnectActions() {
        dao.getFamilyPlaces(HomeActivity.familyId);
        dao.getFamilyMembers(HomeActivity.familyId);
    }

    @Override
    public void handleDisconnectionEvent(@Nullable SQLRuntimeException e) {}

    @Override
    public void handleResult(HashMap<String, Object> result) {
        Object res = result.get(DAO.RESULT_OBJECT_KEY);

        if (!(res instanceof SQLRuntimeException)) {
            int operationCode = (Integer) result.get(DAO.OPERATION_CODE_KEY);
            switch (operationCode) {
                case DAO.GET_MEMBERS_CODE: {
                    id_name_members = new ArrayList<>();
                    Set<User> members = (Set<User>) res;
                    id_name_members.add(new AbstractMap.SimpleEntry<Integer, String>(null, "UNDEFINED"));
                    for (User member : members) {
                        int memberId = member.getId();
                        if (memberId != HomeActivity.userId) {
                            id_name_members.add(new AbstractMap.SimpleEntry<>(memberId, member.getName()));
                        }
                    }
                    updateUI();
                    break;
                }
                case DAO.GET_PLACES_CODE: {
                    familyPlaces = new ArrayList<>();
                    Set<Place> places = (Set<Place>) res;
                    familyPlaces.add(new Place(null, "UNDEFINED", null, HomeActivity.familyId));
                    for (Place place : places) {
                        familyPlaces.add(place);
                    }
                    updateUI();
                    break;
                }
                case DAO.CREATE_TASK_CODE: {
                    Toast.makeText(this, "Task created!", Toast.LENGTH_LONG).show();
                    resetUI();
                    break;
                }
            }
            if (id_name_members != null && familyPlaces != null && dao.isConnected()) {
                createTaskButton.setEnabled(true);
            }
        } else {    //Connection error!
            if (dao.isConnected()) {
                dao.disconnect();
            }
            if ((Integer) result.get(DAO.OPERATION_CODE_KEY) == DAO.CREATE_TASK_CODE) {
                Toast.makeText(this, ((SQLRuntimeException) res).getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateUI() {
        if (id_name_members != null && doerPicker != null) {
            ArrayList<String> values = new ArrayList<>();

            for (AbstractMap.SimpleEntry<Integer, String> entry : id_name_members) {
                values.add(entry.getValue());
            }
            doerPicker.setDisplayedValues(values.toArray(new String[values.size()]));
            doerPicker.setMaxValue(id_name_members.size() - 1);
        }
        if (familyPlaces != null && locationPicker != null) {
            ArrayList<String> values = new ArrayList<>();
            for (Place place : familyPlaces) {
                values.add(place.getName());
            }
            locationPicker.setDisplayedValues(values.toArray(new String[values.size()]));
            locationPicker.setMaxValue(familyPlaces.size() - 1);
        }
    }

    protected void showErrorView() {
        new AlertDialog.Builder(this).setMessage("NO CONNECTION").setMessage("No connection. Please try again later")
                .setNeutralButton("RETRY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        Intent restartIntent = new Intent(TaskCreationActivity.this, getClass());
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

    private void resetUI() {
        nameEditText.setText("");
        doerPicker.setValue(0);
        dateEditText.setText("");
        locationPicker.setValue(0);
        descriptionEditText.setText("");
    }
}
