package projctx.famazing.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.Bind;
import projctx.famazing.R;
import projctx.famazing.data.DAO;
import projctx.famazing.data.DAOEventListener;
import projctx.famazing.data.Family.Membership;
import projctx.famazing.data.SQLRuntimeException;
import projctx.famazing.data.User;

public class LoginActivity extends CookieActivity implements DAOEventListener {

    @Bind(R.id.input_email) EditText _emailText;
    @Bind(R.id.input_password) EditText _passwordText;
    @Bind(R.id.btn_login) Button _loginButton;
    @Bind(R.id.link_signup) TextView _signUpLink;
    @Bind(R.id.loadingSpinner) ProgressBar loadingSpinner;

    private DAO dao;

    private static final int TOTAL_REQUESTS_NEEDED = 2;
    private int requestsSatisfied = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        dao = new DAO (this);
        ButterKnife.bind(this);
        
        _loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isValid = validate();
                if (isValid && !dao.isConnected()) {
                    loadingSpinner.setVisibility(View.VISIBLE);
                    dao.connect();
                } else if (isValid) {
                    onConnectActions();
                }
            }
        });

        _signUpLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent signUpIntent = new Intent(LoginActivity.this, SignUpActivity.class);
                signUpIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(signUpIntent);
            }
        });
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

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("Enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty()) {
            _passwordText.setError("Password cannot be empty. Please enter a password");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }

    @Override
    public void handleConnectionEvent(@Nullable SQLRuntimeException e) {
        if (e == null) {
            onConnectActions();
        } else {
            showErrorView(true);
        }
    }

    //We are sure that this gets called only when input is correct, so no need to check anything particular.
    private void onConnectActions() {
        dao.authenticateUser(_emailText.getText().toString(), _passwordText.getText().toString());
    }

    @Override
    public void handleDisconnectionEvent(@Nullable SQLRuntimeException e) {}

    @Override
    public void handleResult(HashMap<String, Object> result) {
        if (requestsSatisfied == TOTAL_REQUESTS_NEEDED && dao.isConnected()) {
            dao.disconnect();
        }
        Object res = result.get(DAO.RESULT_OBJECT_KEY);

        if (!(res instanceof SQLRuntimeException)) {
            int operationCode = (Integer) result.get(DAO.OPERATION_CODE_KEY);

            switch (operationCode) {
                case DAO.AUTHENTICATE_USER_CODE: {
                    int userId = (int) res;
                    if (userId != -1) {
                        dao.getUser(userId);
                        requestsSatisfied++;
                    } else {
                        showErrorView(false);
                        if (dao.isConnected()) {
                            dao.disconnect();
                        }
                    }
                    break;
                }
                case DAO.GET_USER_CODE: {
                    requestsSatisfied++;
                    User loggedUser = (User) res;
                    int userId = loggedUser.getId();
                    String userName = loggedUser.getName();
                    Membership userMembership = loggedUser.getMembership();
                    int familyId = loggedUser.getFamilyId();

                    writeCookie(userId, userName, userMembership, familyId);

                    Intent checkActivityIntent = new Intent(this, CheckActivity.class);
                    startActivity(checkActivityIntent);
                    finish();
                    break;
                }
            }
        } else {    //Connection error!
            if (dao.isConnected()) {
                dao.disconnect();
            }
            showErrorView(true);
        }
    }

    //If true, error due to connection. If false, error due to authentication
    private void showErrorView(boolean connection) {
        if (connection) {
            new AlertDialog.Builder(this).setTitle("ERROR").setMessage("Connection error. Please retry.").setNeutralButton("RETRY", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    startActivity(getIntent());
                }
            }).show();
        } else {
            new AlertDialog.Builder(this).setTitle("ERROR").setMessage("Wrong credentials. Please try again.").setNeutralButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    _emailText.requestFocus();
                }
            }).show();
        }
    }
}
