package projctx.famazing.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.ProgressBar;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;

import butterknife.Bind;
import butterknife.ButterKnife;
import projctx.famazing.R;
import projctx.famazing.data.DAO;
import projctx.famazing.data.DAOEventListener;
import projctx.famazing.data.DuplicateFieldException;
import projctx.famazing.data.Family.Membership;
import projctx.famazing.data.SQLRuntimeException;
import projctx.famazing.data.User;

public class FamilySetupActivity extends CookieActivity implements DAOEventListener {

    @Bind(R.id.input_familyName) EditText _familyName;
    @Bind(R.id.membershipPicker) NumberPicker _familyRole;
    @Bind(R.id.btn_createFa) Button _createFaButton;
    @Bind(R.id.btn_joinFa) Button _joinFaButton;
    @Bind(R.id.loadingSpinner) ProgressBar loadingSpinner;

    public static final String GO_BACK_INTENT_ACTION = "GO_BACK_ACTIVITY";

    public static final String EMAIL_KEY = "email";
    public static final String PASSWORD_KEY = "password";
    public static final String NAME_KEY = "name";
    public static final String BIRTHDAY_KEY = "birthday";

    private HashMap<Integer, String> memberships = new HashMap<>();

    private DAO dao;

    private String email;
    private String password;
    private String name;
    private Date birthday;

    private boolean joinOperation = false;

    private static int totalRequestsNeeded = 2;
    private int requestsSatisfied = 0;

    private User userCreated;
    private int newFamilyCreatedId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_familysetup);
        dao = new DAO(this);
        ButterKnife.bind(this);

        _createFaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validate()) {
                    dao.connect();
                    loadingSpinner.setVisibility(View.VISIBLE);
                }
            }
        });

        _joinFaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validate()) {
                    joinOperation = true;
                    dao.connect();
                    loadingSpinner.setVisibility(View.VISIBLE);
                }
            }
        });

        ArrayList<String> membershipsAvailable = new ArrayList<>();

        for (Membership m : Membership.values()) {
            membershipsAvailable.add(m.toString());
            memberships.put(membershipsAvailable.size(), m.toString());
        }

        int totalOptions = membershipsAvailable.size() - 1;

        _familyRole.setMinValue(0);
        _familyRole.setDisplayedValues(membershipsAvailable.toArray(new String[totalOptions]));
        _familyRole.setMaxValue(totalOptions);

        email = getIntent().getStringExtra(EMAIL_KEY);
        password = getIntent().getStringExtra(PASSWORD_KEY);
        name = getIntent().getStringExtra(NAME_KEY);
        birthday = Date.valueOf(getIntent().getStringExtra(BIRTHDAY_KEY));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (dao.isConnected()) {
            dao.disconnect();
        }
    }

    private boolean validate() {
        boolean valid = true;

        String family = _familyName.getText().toString();

        if (family.isEmpty()) {
            _familyName.setError("Family name cannot be empty");
            valid = false;
        } else {
            _familyName.setError(null);
        }

        return valid;
    }

    @Override
    public void handleConnectionEvent(@Nullable SQLRuntimeException e) {
        if (e == null) {
            if (joinOperation) {
                dao.getFamilyId(_familyName.getText().toString());
            } else {
                dao.createNewFamily(_familyName.getText().toString());
            }
        } else {
            showErrorView();
        }
    }

    @Override
    public void handleDisconnectionEvent(@Nullable SQLRuntimeException e) {}

    @Override
    public void handleResult(HashMap<String, Object> result) {
        if (requestsSatisfied == totalRequestsNeeded && dao.isConnected()) {
            dao.disconnect();
        }
        Object res = result.get(DAO.RESULT_OBJECT_KEY);

        int operationCode = (Integer) result.get(DAO.OPERATION_CODE_KEY);
        if (!(res instanceof SQLRuntimeException)) {
            switch (operationCode) {
                case DAO.GET_FAMILY_ID_CODE: {
                    int familyId = (int) res;
                    if (familyId == -1) {
                        _familyName.setError("No family with specified name to join");
                        loadingSpinner.setVisibility(View.GONE);
                        if (!dao.isConnected()) {
                            dao.disconnect();
                        }
                    } else {
                        requestsSatisfied++;
                        userCreated = new User(null, name, birthday, Membership.valueOf(memberships.get(_familyRole.getValue())), familyId);
                        dao.createNewUser(email, password, userCreated);
                    }
                    break;
                }
                case DAO.CREATE_FAMILY_CODE: {
                    int familyId = (int) res;
                    requestsSatisfied++;
                    newFamilyCreatedId = familyId;
                    userCreated = new User(null, name, birthday, Membership.valueOf(memberships.get(_familyRole.getValue() + 1)), familyId);
                    dao.createNewUser(email, password, userCreated);
                    break;
                }
                case DAO.CREATE_USER_CODE: {
                    int newUserId = (int) res;
                    requestsSatisfied++;
                    if (userCreated != null) {
                        userCreated.setId(newUserId);
                        writeCookie(newUserId, userCreated.getName(), userCreated.getMembership(), userCreated.getFamilyId());

                        Intent finalIntent = new Intent(this, SignUpEndActivity.class);
                        startActivity(finalIntent);
                        loadingSpinner.setVisibility(View.GONE);
                    }
                    break;
                }
                case DAO.REMOVE_FAMILY_CODE: {
                    Intent signUpIntent = fillIntent();
                    signUpIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    signUpIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(signUpIntent);
                    loadingSpinner.setVisibility(View.GONE);
                    break;
                }
            }
        } else {
            if (dao.isConnected()) {
                dao.disconnect();
            }
            if (res.getClass().equals(DuplicateFieldException.class)) {
                switch (operationCode) {
                    case DAO.CREATE_FAMILY_CODE: {
                        _familyName.setError("Family with that name already existing. Please choose a new name for the family.");
                        loadingSpinner.setVisibility(View.GONE);
                        _familyName.requestFocus();
                        if (dao.isConnected()) {
                            dao.disconnect();
                        }
                        break;
                    }
                    case DAO.CREATE_USER_CODE: {
                        requestsSatisfied++;
                        dao.removeFamily(newFamilyCreatedId);       //Since family has already been created, there is need to remove it.
                        break;
                    }
                    default: break;
                }
            } else {
                showErrorView();
            }
        }
    }

    private Intent fillIntent() {
        Intent intent = new Intent(this, SignUpActivity.class);
        intent.putExtra(FamilySetupActivity.EMAIL_KEY, email);
        intent.putExtra(FamilySetupActivity.PASSWORD_KEY, password);
        intent.putExtra(FamilySetupActivity.NAME_KEY, name);
        intent.putExtra(FamilySetupActivity.BIRTHDAY_KEY, birthday.toString());
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.setAction(GO_BACK_INTENT_ACTION);

        return intent;
    }

    private void showErrorView() {
            new AlertDialog.Builder(this).setTitle("ERROR").setMessage("Connection error. Please retry.")
                    .setPositiveButton("RETRY", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            }).setNeutralButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            }).show();
    }
}
