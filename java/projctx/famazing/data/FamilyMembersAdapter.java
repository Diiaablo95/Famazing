package projctx.famazing.data;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.sql.Date;
import java.util.ArrayList;

import projctx.famazing.R;

/**
 * Adapter for the listview containing all the family members.
 */
public class FamilyMembersAdapter implements ListAdapter {

    ArrayList<User> users;

    public FamilyMembersAdapter() {
        users = new ArrayList<>();
    }

    public FamilyMembersAdapter(ArrayList<User> members) {
        users = members;
    }

    private static class ViewHolder {
        private TextView name_textView;
        private TextView membership_textView;

        ViewHolder(TextView name, TextView membership) {
            this.name_textView = name;
            this.membership_textView = membership;
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {}

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {}

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public Object getItem(int position) {
        return users.get(position);
    }

    @Override
    public long getItemId(int position) {
        return users.get(position).getId();
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
            convertView = inflater.inflate(R.layout.family_member_row_item, parent, false);

            TextView nameTextView = (TextView) convertView.findViewById(R.id.member_name_textView);
            TextView relationshipTextView = (TextView) convertView.findViewById(R.id.member_type_textView);

            holder = new ViewHolder(nameTextView, relationshipTextView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        User userToShow = users.get(position);

        holder.name_textView.setText(userToShow.getName());
        holder.membership_textView.setText(userToShow.getMembership().toString());

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
        return users.isEmpty();
    }
}
