package projctx.famazing.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import projctx.famazing.R;
import projctx.famazing.data.DAO;
import projctx.famazing.data.DAOEventListener;
import projctx.famazing.data.Family.Membership;
import projctx.famazing.data.SQLRuntimeException;
import projctx.famazing.data.Task;
import projctx.famazing.data.Task.Status;
import projctx.famazing.data.User;

/**
 * Fragment responsible of showing the main page to the user: the main page contains a recap of some user's information as well as some tasks information.
 */
public class MainPageFragment extends Fragment implements DAOEventListener {

    private static MainPageFragment self;

    private ArrayList<Task> tasksPending;
    private ArrayList<Task> tasksCompleted;
    private Task taskToComplete;
    private String giverName;

    private DAO dao;

    private Date actualDate;

    private FragmentActivity activity;

    private TextView tasks_completed_textView;
    private TextView tasks_pending_textView;
    private TextView task_giver_textView;
    private TextView task_name_textView;
    private TextView next_task_textView;
    private FrameLayout nextTaskOverview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (self == null) {
            self = this;
        }
        dao = new DAO(this);
        activity = getActivity();
        actualDate = new Date(System.currentTimeMillis());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!dao.isConnected()) {
            Log.w("TAG", "MainPageFragment dao not connected");
            dao.connect();
        } else {
            Log.w("TAG", "MainPageFragment dao connected");
            onConnectActions();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View interfaceView = inflater.inflate(R.layout.content_main_fragment, container, false);

        TextView name_textView = (TextView) interfaceView.findViewById(R.id.name_textView);
        TextView membership_textView = (TextView) interfaceView.findViewById(R.id.membership_textView);
        tasks_completed_textView = (TextView) interfaceView.findViewById(R.id.tasks_completed_textView);
        tasks_pending_textView = (TextView) interfaceView.findViewById(R.id.tasks_pending_textView);
        task_giver_textView = (TextView) interfaceView.findViewById(R.id.task_giver_textView);
        task_name_textView = (TextView) interfaceView.findViewById(R.id.task_name_textView);
        next_task_textView = (TextView) interfaceView.findViewById(R.id.next_task_textView);
        nextTaskOverview = (FrameLayout) interfaceView.findViewById(R.id.nextTaskOverview);

        LinearLayout taskView = (LinearLayout) interfaceView.findViewById(R.id.taskView);
        if (taskView != null) {
            taskView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Integer locationId = taskToComplete != null ? taskToComplete.getLocationId() : null;
                    if (taskToComplete != null && locationId != null) {
                        Intent taskIntent = new Intent(activity, TaskMapActivity.class);
                        taskIntent.putExtra(TaskMapActivity.TASK_NAME_KEY, taskToComplete.getName());
                        taskIntent.putExtra(TaskMapActivity.TASK_DESCRIPTION_KEY, taskToComplete.getDescription());
                        taskIntent.putExtra(TaskMapActivity.TASK_PLACE_KEY, locationId);
                        startActivity(taskIntent);
                    } else if (locationId == null) {
                        Toast.makeText(activity, "This task has no location.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }


        if (name_textView != null) {
            name_textView.setText(HomeActivity.userName);
        }
        if (membership_textView != null) {
            membership_textView.setText(HomeActivity.userMembership.toString());
        }

        ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.main_page_title));
        }
        return interfaceView;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (dao.isConnected()) {
            dao.disconnect();
        }
    }

    @Override
    public void handleConnectionEvent(SQLRuntimeException e) {
        if (e == null) {
            onConnectActions();
        } else {
            showErrorView();
        }
    }

    private void onConnectActions() {
        dao.getTasks(HomeActivity.userId, Status.PENDING, null, null);
        dao.getTasks(HomeActivity.userId, Status.COMPLETED, actualDate, actualDate);
    }

    @Override
    public void handleDisconnectionEvent(SQLRuntimeException e) {}

    @Override
    public void handleResult(HashMap<String, Object> result) {
        Object res = result.get(DAO.RESULT_OBJECT_KEY);

        if (!(res instanceof SQLRuntimeException)) {
            int operationCode = (Integer) result.get(DAO.OPERATION_CODE_KEY);
            switch (operationCode) {
                case DAO.GET_TASKS_COMPLETED_CODE: {
                    tasksCompleted = (ArrayList<Task>) res;
                    if (tasksPending != null && giverName != null && dao.isConnected()) {
                        dao.disconnect();
                    }
                    break;
                }
                case DAO.GET_TASKS_PENDING_CODE: {
                    tasksPending = (ArrayList<Task>) res;
                    //Tasks are returned in chronological order, so the first in the list is the first in order (even already past).
                    taskToComplete = tasksPending.size() > 0 ? tasksPending.get(0) : null;
                    if (taskToComplete != null) {
                        dao.getUser(taskToComplete.getGiver());
                    }
                    break;
                }
                case DAO.GET_USER_CODE: {
                    if (tasksCompleted != null && tasksPending != null && dao.isConnected()) {
                        dao.disconnect();
                    }
                    giverName = ((User) res).getName();
                    break;
                }
            }
            //updateUI called in any case so that everytime we get an answer, the corresponding UI element is shown
            updateUI();
        } else {    //Connection error!
            if (dao.isConnected()) {
                dao.disconnect();
            }
            showErrorView();
        }
    }

    //Refresh the UI based on the element just received from the database.
    private void updateUI() {
        if (tasks_completed_textView != null && tasksCompleted != null) {
            if (tasksCompleted.size() != 1) {
                tasks_completed_textView.setText(String.format(Locale.ENGLISH, "%d tasks completed today.", tasksCompleted.size()));
            } else {
                tasks_completed_textView.setText(String.format(Locale.ENGLISH, "%d task completed today.", tasksCompleted.size()));
            }
        }
        if (tasks_pending_textView != null && tasksPending != null) {
            if (tasksPending.size() != 1) {
                tasks_pending_textView.setText(String.format(Locale.ENGLISH, "%d tasks still to complete.", tasksPending.size()));
            } else {
                tasks_pending_textView.setText(String.format(Locale.ENGLISH, "%d task still to complete.", tasksPending.size()));
            }
        }

        if (taskToComplete != null) {
            if (nextTaskOverview != null && next_task_textView != null && isAdded()) {
                nextTaskOverview.setVisibility(View.GONE);
            }
            if (task_giver_textView != null) {
                task_giver_textView.setText(giverName);
            }
            if (task_name_textView != null) {
                task_name_textView.setText(taskToComplete.getName());
            }
        } else if (tasksPending != null && next_task_textView != null && isAdded()) {   //If tasks have been fetched and there is no pending task
            next_task_textView.setText(getString(R.string.no_next_task_label));
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
                        activity.finish();
                    }
                }).setCancelable(false).show();
    }
}