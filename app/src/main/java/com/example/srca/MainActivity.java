package com.example.srca;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private CardView fanCardView;
    private CardView lightCardView;
    private TextView fanStatusTextView;
    private TextView lightStatusTextView;
    private ListenerRegistration roomListener;
    private SeekBar curtainsSeekBar;
    private TextView curtainsTextView;
    private Switch switch1;
    private Switch switch2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button backButton = findViewById(R.id.backbtn);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);

                // You can add extra data to the intent if needed
                // intent.putExtra("key", value);

                // Start the destination activity
                startActivity(intent);

            }
        });

        db = FirebaseFirestore.getInstance();

        //fanCardView = findViewById(R.id.card_view_fan);
        lightCardView = findViewById(R.id.card_view_light);
        //fanStatusTextView = findViewById(R.id.status_text_view_fan);
        lightStatusTextView = findViewById(R.id.status_text_view_light);
        CardView curtainsCardView = findViewById(R.id.card_view_curtains);
        curtainsSeekBar = findViewById(R.id.seekbar_curtains);
        curtainsTextView = findViewById(R.id.status_text_view_curtains);
        switch1 = findViewById(R.id.switch1);
        switch2 = findViewById(R.id.switch2);

        // Set click listeners for the fan and light CardViews
        //fanCardView.setOnClickListener(v -> toggleFanState());
        lightCardView.setOnClickListener(v -> toggleLightState());

        // Assuming "house" is the collection name and "room" is the document name
        DocumentReference roomRef = db.collection("house").document("room");
        roomListener = roomRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                // Error handling
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                // Get the values of fan and light fields
                //Integer fanStatus = snapshot.getLong("fan").intValue();
                Integer lightStatus = snapshot.getLong("light").intValue();

                // Update UI based on the values
                //updateFanStateUI(fanStatus);
                updateLightStateUI(lightStatus);
                updateCurtainFromFirestore();
            }
        });

        curtainsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Map progress to your desired values
                int value = mapProgressToValue(progress);
                // Update your variable or do something with the value
                // For example, update a TextView to display the selected value
                curtainsTextView.setText(value + "%");
                saveCurtainValue(value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed for this example
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not needed for this example
            }
        });

        readAutomationState();
        readFeedbackState();

        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleAutomation(isChecked);
            }
        });
        switch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleFeedback(isChecked);
            }
        });

    }

    private void toggleAutomation(boolean isChecked) {
        // Convert the switch state to a value to store in Firestore
        int automationState = isChecked ? 1 : 0;

        // Write the value to Firestore
        db.collection("house").document("room")
                .update("automation", automationState)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Successfully updated Firestore
                        Toast.makeText(MainActivity.this, "Automation state updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to update Firestore
                        Toast.makeText(MainActivity.this, "Error updating automation state", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void toggleFeedback(boolean isChecked) {
        // Convert the switch state to a value to store in Firestore
        int feedbackState = isChecked ? 1 : 0;

        // Write the value to Firestore
        db.collection("house").document("room")
                .update("feedback", feedbackState)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Successfully updated Firestore
                        Toast.makeText(MainActivity.this, "Feedback state updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to update Firestore
                        Toast.makeText(MainActivity.this, "Error updating feedback state", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void readAutomationState() {
        // Read the value from Firestore
        db.collection("house").document("room")
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Long automationState = document.getLong("automation");
                                if (automationState != null && automationState == 1) {
                                    switch1.setChecked(true);
                                } else {
                                    switch1.setChecked(false);
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "No such document", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Get failed with " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void readFeedbackState() {
        // Read the value from Firestore
        db.collection("house").document("room")
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Long feedbackState = document.getLong("feedback");
                                if (feedbackState != null && feedbackState == 1) {
                                    switch2.setChecked(true);
                                } else {
                                    switch2.setChecked(false);
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "No such document", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Get failed with " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void toggleFanState() {
        DocumentReference roomRef = db.collection("house").document("room");
        roomRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Integer currentFanState = documentSnapshot.getLong("fan").intValue();
                int newFanState = (currentFanState == 1) ? 0 : 1;
                roomRef.update("fan", newFanState)
                        .addOnSuccessListener(aVoid -> {

                        })
                        .addOnFailureListener(e -> {
                            // Error updating fan state
                        });
                if (currentFanState == 0) {
                    SharedPreferences preferences = getSharedPreferences("FanTimeStampPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();

                    long currentFanTimestamp = System.currentTimeMillis();
                    editor.putLong("fanTimestamp", currentFanTimestamp);
                    editor.apply();
                }
            }
        }).addOnFailureListener(e -> {
            // Error getting document
        });
    }

    private void toggleLightState() {
        DocumentReference roomRef = db.collection("house").document("room");
        roomRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Integer currentLightState = documentSnapshot.getLong("light").intValue();
                int newLightState = (currentLightState == 1) ? 0 : 1;
                roomRef.update("light", newLightState)
                        .addOnSuccessListener(aVoid -> {

                        })
                        .addOnFailureListener(e -> {
                            // Error updating light state
                        });
                if (currentLightState == 0) {
                    SharedPreferences preferences = getSharedPreferences("LightTimeStampPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();

                    long currentLightTimestamp = System.currentTimeMillis();
                    editor.putLong("lightTimestamp", currentLightTimestamp);
                    editor.apply();
                }
            }
        }).addOnFailureListener(e -> {
            // Error getting document
        });
    }

    private void saveCurtainValue(float curtainValue) {
        DocumentReference roomRef = db.collection("house").document("room");
        roomRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                roomRef.update("curtain", curtainValue)
                        .addOnSuccessListener(aVoid -> {
                            // Curtain value updated successfully
                        })
                        .addOnFailureListener(e -> {
                            // Error updating curtain value
                        });
                // Assuming you also want to save a timestamp when the curtain value changes
                SharedPreferences preferences = getSharedPreferences("CurtainTimeStampPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                long currentCurtainTimestamp = System.currentTimeMillis();
                editor.putLong("curtainTimestamp", currentCurtainTimestamp);
                editor.apply();
            }
        }).addOnFailureListener(e -> {
            // Error getting document
        });
    }

    private void updateCurtainFromFirestore() {
        DocumentReference roomRef = db.collection("house").document("room");
        roomRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Get the curtain value from Firestore document
                int curtainValue = documentSnapshot.getLong("curtain").intValue();

                // Map curtain value to SeekBar progress
                int progress = mapValueToProgress(curtainValue);

                // Update SeekBar progress
                curtainsSeekBar.setProgress(progress);

                // Update TextView with the status
                curtainsTextView.setText((int)(curtainValue) + "%");
            }
        }).addOnFailureListener(e -> {
            // Error getting document
        });
    }



    /*private void updateFanStateUI(int fanStatus) {
        TextView fanTimestampTextView = findViewById(R.id.timestamp_textview_fan);

        // Update the light status text view
        if (fanStatus == 1) {
            fanStatusTextView.setText("On");
        } else {
            fanStatusTextView.setText("Off");

            // Retrieve the last turn off timestamp from SharedPreferences
            SharedPreferences preferences = getSharedPreferences("FanTimestampPrefs", MODE_PRIVATE);
            long fanTimestamp = preferences.getLong("fanTimestamp", 0);

            // Display the last turn off timestamp
            if (fanTimestamp != 0) {
                // Format the timestamp as needed
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                String formattedTimestamp = dateFormat.format(new Date(fanTimestamp));
                fanTimestampTextView.setText(formattedTimestamp);
            } else {
                fanTimestampTextView.setText(" -");
            }
        }
    }*/

    // Update the UI with the last turn off timestamp from SharedPreferences
    private void updateLightStateUI(int lightStatus) {
        TextView lightTimestampTextView = findViewById(R.id.timestamp_textview_light);

        // Update the light status text view
        if (lightStatus == 1) {
            lightStatusTextView.setText("On");
        } else {
            lightStatusTextView.setText("Off");

            // Retrieve the last turn off timestamp from SharedPreferences
            SharedPreferences preferences = getSharedPreferences("LightTimestampPrefs", MODE_PRIVATE);
            long lightTimestamp = preferences.getLong("lightTimestamp", 0);

            // Display the last turn off timestamp
            if (lightTimestamp != 0) {
                // Format the timestamp as needed
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                String formattedTimestamp = dateFormat.format(new Date(lightTimestamp));
                lightTimestampTextView.setText(formattedTimestamp);
            } else {
                lightTimestampTextView.setText(" -");
            }
        }
    }

    // Map SeekBar progress to your desired values
    private int mapProgressToValue(int progress) {
        switch (progress) {
            case 0:
                return 0;
            case 1:
                return 25;
            case 2:
                return 50;
            case 3:
                return 75;
            case 4:
                return 100;
            default:
                return 0;
        }
    }

    private int mapValueToProgress(int value) {
        switch ((int)(value)) {
            case 0:
                return 0;
            case 25:
                return 1;
            case 50:
                return 2;
            case 75:
                return 3;
            case 100:
                return 4;
            default:
                return 0;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the snapshot listener to avoid memory leaks
        if (roomListener != null) {
            roomListener.remove();
        }
    }
}
