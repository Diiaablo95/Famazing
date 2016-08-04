package projctx.famazing.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import butterknife.Bind;
import butterknife.ButterKnife;
import projctx.famazing.R;

public class SignUpEndActivity extends AppCompatActivity {

    @Bind(R.id.btn_Fin) Button _finButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_end);

        ButterKnife.bind(this);

        _finButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent checkActivityIntent = new Intent(SignUpEndActivity.this, CheckActivity.class);
                checkActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(checkActivityIntent);
                finish();
            }
        });
    }
}
