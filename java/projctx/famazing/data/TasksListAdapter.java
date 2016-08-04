package projctx.famazing.data;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import projctx.famazing.R;

/**
 * Adapter for the listview containing all the family members.
 */
public class TasksListAdapter implements ListAdapter {

    private List<Task> tasks;
    private Map<Integer, String> ids_names;
    private DAOEventListener listenerActivity;
    private DAO dao;
    private int completerId;

    public TasksListAdapter(DAOEventListener listenerActivity, int completerId) {
        super();
        tasks = new ArrayList<>();
        ids_names = new HashMap<>();
        if (listenerActivity != null) {
            this.listenerActivity = listenerActivity;
            dao = new DAO(this.listenerActivity);
        }
        this.completerId = completerId;
    }

    public TasksListAdapter(List<Task> tasks, Map<Integer, String> ids_names, DAOEventListener listenerActivity, int completerId) {
        this(listenerActivity, completerId);
        this.tasks = tasks;
        this.ids_names = ids_names;
    }

    private static class ViewHolder {
        TextView descriptionTextView;
        TextView giverTextView;
        TextView dateTextView;
        ImageButton acceptButton;
        ImageButton declineButton;
        TextView statusTextView;
        LinearLayout buttonsLayout;
        private int completerId;
        private DAOEventListener list;
        private DAO dao;

        ViewHolder(TextView description, TextView giver, TextView date, LinearLayout _buttonsLayout, ImageButton accept, ImageButton decline, TextView status, DAOEventListener _list, DAO _dao, final int _completerId) {
            this.descriptionTextView = description;
            this.giverTextView = giver;
            this.dateTextView = date;
            this.acceptButton = accept;
            this.declineButton = decline;
            this.statusTextView = status;
            this.completerId = _completerId;
            this.list = _list;
            this.dao = _dao;
            this.buttonsLayout = _buttonsLayout;

            final View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ImageButton buttonTouched = (ImageButton) v;

                    int taskId = (int) buttonTouched.getTag();
                    switch (buttonTouched.getId()) {
                        case R.id.acceptButton: {
                            Log.w("TAG", "Accept button pressed!");
                            if (list != null && dao != null && dao.isConnected()) {
                                dao.completeTask(taskId, completerId);
                            }
                            break;
                        }
                        case R.id.declineButton: {
                            Log.w("TAG", "Decline button pressed!");
                            if (list != null && dao != null && dao.isConnected()) {
                                dao.refuseTask(taskId);
                            }
                            break;
                        }
                        default: break;
                    }
                }
            };

            this.acceptButton.setOnClickListener(listener);
            this.declineButton.setOnClickListener(listener);
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {}

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {}

    @Override
    public int getCount() {
        return tasks.size();
    }

    @Override
    public Object getItem(int position) {
        return tasks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return tasks.get(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            Context context = parent.getContext();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.task_row_item, parent, false);

            TextView descriptionTextView = (TextView) convertView.findViewById(R.id.task_description_textView);
            TextView giverTextView = (TextView) convertView.findViewById(R.id.giverName_textView);
            TextView dateTextView = (TextView) convertView.findViewById(R.id.date_completion_textView);
            TextView statusTextView = (TextView) convertView.findViewById(R.id.statusTextView);
            ImageButton acceptButton = (ImageButton) convertView.findViewById(R.id.acceptButton);
            ImageButton declineButton = (ImageButton) convertView.findViewById(R.id.declineButton);
            LinearLayout buttonsLayout = (LinearLayout) convertView.findViewById(R.id.buttonsLayout);

            holder = new ViewHolder(descriptionTextView, giverTextView, dateTextView, buttonsLayout, acceptButton, declineButton, statusTextView, listenerActivity, dao, completerId);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Task taskToShow = tasks.get(position);

        String taskDescription = taskToShow.getDescription();
        Date taskDeadline = taskToShow.getDeadline();
        Task.Status taskStatus = taskToShow.getStatus();

        holder.descriptionTextView.setText(taskDescription != null ? taskDescription : "NONE");
        holder.dateTextView.setText(taskDeadline != null ? taskDeadline.toString() : "NONE");

        String giverName = ids_names.get(taskToShow.getGiver());
        if (giverName != null) {
            holder.giverTextView.setText(giverName);
        }

        //If task has been completed/refused, no more actions are available
        holder.statusTextView.setText(taskStatus.toString());
        holder.acceptButton.setTag((int) getItemId(position));
        holder.declineButton.setTag((int) getItemId(position));
        if (taskStatus != Task.Status.PENDING) {
            holder.buttonsLayout.setVisibility(View.GONE);
        }

        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return tasks.isEmpty();
    }
}