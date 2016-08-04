package projctx.famazing.ui;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.Bind;
import projctx.famazing.R;
import projctx.famazing.utility.DateEditText;

public class SignUpActivity extends AppCompatActivity {

    @Bind(R.id.input_name) EditText _nameText;
    @Bind(R.id.input_email) EditText _emailText;
    @Bind(R.id.input_password) EditText _passwordText;
    @Bind(R.id.input_birthday) DateEditText _inputBirthday;
    @Bind(R.id.btn_signup) Button _signUpButton;

    private static final int DEFAULT_YEAR = 1980;
    private static final int DEFAULT_MONTH = 0;
    private static final int DEFAULT_DAY = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.bind(this);

        _signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validate()) {
                    Intent nextStepIntent = fillIntent();
                    startActivity(nextStepIntent);
                }
            }
        });

        _inputBirthday.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View v, boolean hasFocus) {
                if (hasFocus) {
                    Log.d("TAG", "Focus!");
                    int yearToShow = DEFAULT_YEAR;
                    int monthToShow = DEFAULT_MONTH;
                    int dayToShow = DEFAULT_DAY;

                    if (_inputBirthday.isDateComplete()) {
                        yearToShow = _inputBirthday.getYear();
                        monthToShow = _inputBirthday.getMonth() - 1;
                        dayToShow = _inputBirthday.getDay();
                    }
                    new DatePickerDialog(SignUpActivity.this, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            ((DateEditText) v).setText(String.format(Locale.ENGLISH, "%d-%02d-%02d", year, monthOfYear + 1, dayOfMonth));
                        }
                    }, yearToShow, monthToShow, dayToShow).show();
                }
            }
        });
        _inputBirthday.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    int yearToShow = DEFAULT_YEAR;
                    int monthToShow = DEFAULT_MONTH;
                    int dayToShow = DEFAULT_DAY;

                    if (_inputBirthday.isDateComplete()) {
                        yearToShow = _inputBirthday.getYear();
                        monthToShow = _inputBirthday.getMonth() - 1;
                        dayToShow = _inputBirthday.getDay();
                    }
                    new DatePickerDialog(SignUpActivity.this, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            ((DateEditText) v).setText(String.format(Locale.ENGLISH, "%d-%02d-%02d", year, monthOfYear + 1, dayOfMonth));
                        }
                    }, yearToShow, monthToShow, dayToShow).show();
                }
                return true;
            }
        });

        Intent intent = getIntent();
        if (intent.getAction() != null && intent.getAction().equals(FamilySetupActivity.GO_BACK_INTENT_ACTION)) {
            _nameText.setText(intent.getStringExtra(FamilySetupActivity.NAME_KEY));
            _emailText.setText(intent.getStringExtra(FamilySetupActivity.EMAIL_KEY));
            _passwordText.setText(intent.getStringExtra(FamilySetupActivity.PASSWORD_KEY));
            _inputBirthday.setText(intent.getStringExtra(FamilySetupActivity.BIRTHDAY_KEY));

            _emailText.setError("Email already associated to another user. Please enter another one.");
            _emailText.requestFocus();
        }
    }

    private Intent fillIntent() {
        Intent intent = new Intent(this, FamilySetupActivity.class);
        intent.putExtra(FamilySetupActivity.EMAIL_KEY, _emailText.getText().toString());
        intent.putExtra(FamilySetupActivity.PASSWORD_KEY, _passwordText.getText().toString());
        intent.putExtra(FamilySetupActivity.NAME_KEY, _nameText.getText().toString());
        intent.putExtra(FamilySetupActivity.BIRTHDAY_KEY, _inputBirthday.getText().toString());
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        return intent;
    }

    private boolean validate() {
        boolean valid = true;

        String name = _nameText.getText().toString();
        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();
        String date = _inputBirthday.getText().toString();

        if (name.isEmpty()) {
            _nameText.setError("Name cannot be empty");
            valid = false;
        } else {
            _nameText.setError(null);
        }

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

        if (date.isEmpty()) {
            _inputBirthday.setError("Birthday cannot be empty. Please enter your birthday");
            valid = false;
        } else {
            _inputBirthday.setError(null);
        }

        return valid;
    }
}