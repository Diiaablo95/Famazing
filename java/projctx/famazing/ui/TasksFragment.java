package projctx.famazing.ui;

import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.content.Intent;
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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

import projctx.famazing.R;
import projctx.famazing.data.DAO;
import projctx.famazing.data.DAOEventListener;
import projctx.famazing.data.SQLRuntimeException;
import projctx.famazing.data.Task;
import projctx.famazing.data.TasksListAdapter;
import projctx.famazing.data.User;

/**
 * Fragment responsible of showing the tasks related to a family members.
 */
public class TasksFragment extends Fragment implements DAOEventListener {

    private static TasksFragment self;

    private ArrayList<Task> tasks;
    private HashMap<Integer, String> id_name_map = new HashMap<>();
    private DAO dao;
    private TasksListAdapter adapter;

    private TextView tasks_count_textView;
    private ListView tasksList;
    private TextView loading_textView;
    private TextView noTaskTextView;

    private FragmentActivity activity;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (self == null) {
            self = this;
        }
        dao = new DAO(this);
        activity = getActivity();
        super.onStart();
        if (!dao.isConnected()) {
            Log.w("TAG", "TasksFragment dao not connected");
            dao.connect();
        } else {
            Log.w("TAG", "TasksFragment dao connected");
            onConnectActions();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!dao.isConnected()) {
            Log.w("TAG", "TasksFragment dao not connected");
            dao.connect();
        } else {
            Log.w("TAG", "TasksFragment dao connected");
            onConnectActions();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View viewToShow = inflater.inflate(R.layout.content_tasks_fragment, container, false);

        tasks_count_textView = (TextView) viewToShow.findViewById(R.id.tasks_count_textView);
        tasksList = (ListView) viewToShow.findViewById(R.id.tasks_listView);
        loading_textView = (TextView) viewToShow.findViewById(R.id.loading_textView);
        noTaskTextView = (TextView) viewToShow.findViewById(R.id.noTaskTextView);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.tasks_fragment_title);
        }

        if (tasksList != null) {
            tasksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Task taskClicked = (Task) parent.getItemAtPosition(position);
                    Integer locationId = taskClicked.getLocationId();

                    if (locationId != null) {
                        Intent taskIntent = new Intent(getActivity(), TaskMapActivity.class);
                        taskIntent.putExtra(TaskMapActivity.TASK_NAME_KEY, taskClicked.getName());
                        taskIntent.putExtra(TaskMapActivity.TASK_DESCRIPTION_KEY, taskClicked.getDescription());
                        taskIntent.putExtra(TaskMapActivity.TASK_PLACE_KEY, locationId);
                        startActivity(taskIntent);
                    } else {
                        Toast.makeText(getActivity(), "This task has no location.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        Button taskCreationButton = (Button) viewToShow.findViewById(R.id.create_task_button);
        if (taskCreationButton != null) {
            taskCreationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent taskCreationIntent = new Intent(getActivity(), TaskCreationActivity.class);
                    startActivity(taskCreationIntent);
                }
            });
        }
        return viewToShow;
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
        if (tasks == null) {
            dao.getTasks(HomeActivity.userId, null, null, null);
        }
    }

    @Override
    public void handleDisconnectionEvent(SQLRuntimeException e) {}

    @Override
    public void handleResult(HashMap<String, Object> result) {
        Object res = result.get(DAO.RESULT_OBJECT_KEY);

        if (!(res instanceof SQLRuntimeException)) {
            int operationCode = (Integer) result.get(DAO.OPERATION_CODE_KEY);
            switch (operationCode) {
                case DAO.GET_TASKS_CODE: {
                    tasks = (ArrayList<Task>) res;
                    Collections.sort(tasks, new Comparator<Task>() {
                        //Order among tasks status (first priority): pending-completed-refused tasks.
                        //Order among tasks deadline (second priority): earlier date - later date - no date.
                        @Override
                        public int compare(Task lhs, Task rhs) {
                            int result;
                            if (lhs.getStatus() == rhs.getStatus()) {
                                Date lDate = lhs.getDeadline();
                                Date rDate = rhs.getDeadline();
                                if (lDate != null && rDate == null) {
                                    result = -1;
                                } else if (lDate == null && rDate != null) {
                                    result = 1;
                                } else if (lDate == null && rDate == null) {
                                    result = 0;
                                } else {
                                    result = lDate.compareTo(rDate);
                                }
                            } else {
                                result = lhs.getStatus().compareTo(rhs.getStatus());
                            }
                            return result;
                        }
                    });
                    for (Task task : this.tasks) {
                        int taskGiverId = task.getGiver();
                        if (id_name_map.get(taskGiverId) == null) {
                            dao.getUser(taskGiverId);
                        }
                    }
                    if (tasksList != null) {
                        adapter = new TasksListAdapter(tasks, id_name_map, this, HomeActivity.userId);
                        tasksList.setAdapter(adapter);
                        tasksList.setEmptyView(noTaskTextView);
                        tasks_count_textView.setText(String.format(Locale.ENGLISH, "%d tasks", adapter.getCount()));
                        tasksList.invalidateViews();
                    }
                    loading_textView.setVisibility(View.GONE);
                    break;
                }
                case DAO.GET_USER_CODE: {
                    User user = (User) res;
                    int userId = user.getId();
                    String userName = user.getName();

                    if (id_name_map.get(userId) == null) {
                        id_name_map.put(userId, userName);
                    }
                    if (tasksList != null) {
                        tasksList.invalidateViews();
                    }
                    break;
                }
                default: break;
            }
        } else {    //Connection error!
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
}