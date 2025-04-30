package com.studiox.taskit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.divider.MaterialDivider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AboutActivity extends AppCompatActivity {

    LinearLayout backbtn, deleteaccountsection;
    TextView feedbackbtn, deleteAccountbtn;
    MaterialDivider deletesection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        feedbackbtn = findViewById(R.id.feedbackbutton);
        backbtn = findViewById(R.id.backButton);
        deleteAccountbtn = findViewById(R.id.deleteAccountButton);
        deleteaccountsection = findViewById(R.id.deleteAccountSection);
        deletesection = findViewById(R.id.deletesection);

        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AboutActivity.this, SettingsActivity.class);
                startActivity(intent);
                finish();
            }
        });

        feedbackbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/cHNdp4vCCxFHgqZK6"));
                startActivity(intent);
            }
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {

            deleteaccountsection.setVisibility(View.VISIBLE);
            deletesection.setVisibility(View.VISIBLE);

            deleteAccountbtn.setOnClickListener(v -> {
                String userName = user.getDisplayName();

                String subject = "We're Sad to See You Go!";
                String body = "Hi " + (userName != null ? userName : "there") + ",\n\n" +
                        "As the developer of this app, I’m truly sorry to hear that you want to delete your account. We have truly appreciated having you as part of our community, and it deeply saddens us to see you go.\n\n" +
                        "Before you proceed, I want to ask if there's anything we can do to make your experience better. If there’s an issue, a suggestion, or a feature you’d like to see, please let us know. We value your feedback and would love the opportunity to improve and keep you with us.\n\n" +
                        "If, however, you’ve made up your mind and would still like to proceed with deleting your account, we completely respect your decision and will help you with that. Below are your account details for our reference:\n\n" +
                        "User ID: " + user.getUid() + "\n" +
                        "Email: " + user.getEmail() + "\n\n" +
                        "Thank you so much for being a part of our journey. We sincerely hope you will reconsider and stay with us.\n\n" +
                        "Best regards,\n" +
                        "The TaskIt Team";

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"officialstudiox24@gmail.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                intent.putExtra(Intent.EXTRA_TEXT, body);

                intent.setPackage("com.google.android.gm");

                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(AboutActivity.this, "Gmail app not installed.", Toast.LENGTH_SHORT).show();
                }
            });

        } else {

            deleteaccountsection.setVisibility(View.GONE);
            deletesection.setVisibility(View.GONE);
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(AboutActivity.this, SettingsActivity.class);
        startActivity(intent);
        finish();
    }
}