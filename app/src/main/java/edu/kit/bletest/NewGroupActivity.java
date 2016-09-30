package edu.kit.bletest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import edu.kit.privateadhocpeering.BeaconManager;

public class NewGroupActivity extends AppCompatActivity {

    private EditText identifierInput;
    private CheckBox proximityCheckBox;
    private Button addPeerButton;
    private TextView peerList;
    private Button doneButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_group);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Configure New Discovery Group");

        identifierInput = (EditText) findViewById(R.id.editText);

        proximityCheckBox = (CheckBox) findViewById(R.id.checkBox);

        doneButton = (Button) findViewById(R.id.button2);

        identifierInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                doneButton.setEnabled(s.length() != 0);
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = identifierInput.getText().toString();
                BeaconManager.initInstance(getApplicationContext()).addAdvertisementSet(id, proximityCheckBox.isChecked());
                finish();
                Intent intent = new Intent(getApplicationContext(), GroupInfoActivity.class);
                intent.putExtra("id", id);
                startActivity(intent);
            }
        });
    }

}
