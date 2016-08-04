package projctx.famazing.data;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import projctx.famazing.R;

/**
 * Adapter for the listview containing all the family alerts.
 */
public class AlertsListAdapter implements ListAdapter {

    private ArrayList<Alert> alerts;
    private Map<Integer, String> ids_names;

    public AlertsListAdapter() {
        super();
        alerts = new ArrayList<>();
        ids_names = new HashMap<>();
    }

    public AlertsListAdapter(ArrayList<Alert> alerts, HashMap<Integer, String> id_names) {
        this.alerts = alerts;
        this.ids_names = id_names;
    }

    private static class ViewHolder {
        private TextView name_textView;
        private TextView date_textView;

        ViewHolder(TextView name, TextView date) {
            this.name_textView = name;
            this.date_textView = date;
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
        return alerts.size();
    }

    @Override
    public Object getItem(int position) {
        return alerts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return alerts.get(position).getId();
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
            convertView = inflater.inflate(R.layout.alert_row_item, parent, false);

            TextView nameTextView = (TextView) convertView.findViewById(R.id.member_name_textView);
            TextView dateTextView = (TextView) convertView.findViewById(R.id.alert_date_textView);

            holder = new ViewHolder(nameTextView, dateTextView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Alert alertToShow = this.alerts.get(position);
        int creatorId = alertToShow.getUserId();
        String creatorName = ids_names.get(creatorId);

        holder.date_textView.setText(alertToShow.getAlertDate().toString());
        if (creatorName != null) {
            holder.name_textView.setText(creatorName);
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
        return alerts.isEmpty();
    }
}
