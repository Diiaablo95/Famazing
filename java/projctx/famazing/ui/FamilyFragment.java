package projctx.famazing.ui;

import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import projctx.famazing.R;
import projctx.famazing.data.DAO;
import projctx.famazing.data.DAOEventListener;
import projctx.famazing.data.Family;
import projctx.famazing.data.FamilyMembersAdapter;
import projctx.famazing.data.SQLRuntimeException;
import projctx.famazing.data.User;

/**
 * Fragment responsible of showing the list with all the family components.
 */
public class FamilyFragment extends Fragment implements DAOEventListener {

    private static FamilyFragment self;

    private ArrayList<User> members = new ArrayList<>();
    private DAO dao;
    private FamilyMembersAdapter adapter;

    private TextView familyTextView;
    private TextView membersTextView;
    private ListView familyListView;

    private FragmentActivity activity;

    private int requestsSatisfied = 0;
    private static final int TOTAL_REQUESTS_NEEDED = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (self == null) {
            self = this;
        }
        dao = new DAO(this);
        activity = getActivity();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!dao.isConnected()) {
            Log.w("TAG", "FamilyFragment dao not connected");
            dao.connect();
        } else {
            Log.w("TAG", "FamilyFragment dao connected");
            onConnectActions();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View viewToDisplay = inflater.inflate(R.layout.content_family_fragment, container, false);

        familyTextView = (TextView) viewToDisplay.findViewById(R.id.family_textView);
        membersTextView = (TextView) viewToDisplay.findViewById(R.id.members_textView);

        familyListView = (ListView) viewToDisplay.findViewById(R.id.family_members_listView);

        ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.family_fragment_title));
        }

        return viewToDisplay;
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
        dao.getFamilyMembers(HomeActivity.familyId);
        dao.getFamily(HomeActivity.familyId);
    }

    @Override
    public void handleDisconnectionEvent(SQLRuntimeException e) {}

    @Override
    public void handleResult(HashMap<String, Object> result) {
        //We know that 2 requests are sent to the DB for this fragments
        if (++requestsSatisfied == TOTAL_REQUESTS_NEEDED && dao.isConnected()) {
            dao.disconnect();
        }
        Object res = result.get(DAO.RESULT_OBJECT_KEY);

        if (!(res instanceof SQLRuntimeException)) {
            int operationCode = (Integer) result.get(DAO.OPERATION_CODE_KEY);

            switch (operationCode) {
                case DAO.GET_MEMBERS_CODE: {
                    members = new ArrayList<>();
                    members.addAll((Set<User>) res);
                    if (familyListView != null) {
                        adapter = new FamilyMembersAdapter(members);
                        familyListView.setAdapter(adapter);
                        membersTextView.setText(String.format(Locale.ENGLISH, "%d members", adapter.getCount()));
                        familyListView.invalidateViews();
                    }
                    break;
                }
                case DAO.GET_FAMILY_NAME_CODE: {
                    familyTextView.setText(((Family) res).getName());
                    break;
                }
            }
            if (familyListView != null) {
                familyListView.invalidate();
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