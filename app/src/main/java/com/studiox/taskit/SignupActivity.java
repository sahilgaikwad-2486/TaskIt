package com.studiox.taskit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignupActivity extends AppCompatActivity {

    EditText editTextEmail, editTextPassword;
    TextInputLayout edittextpasswordlayout;
    LinearLayout createaccountfirebase, siguppageback, googlesignup;
    TextView loginpage;
    FirebaseAuth mAuth;
    GoogleSignInClient googleSignInClient;
    FirebaseFirestore db;
    FirebaseUser user;
    LinearProgressIndicator progressBar;
    int RC_SIGN_IN = 20;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        progressBar = findViewById(R.id.progressBar);
        siguppageback = findViewById(R.id.signupbackbutton);
        loginpage = findViewById(R.id.loginpagebutton);
        googlesignup = findViewById(R.id.signupgooglebutton);
        editTextPassword = findViewById(R.id.signuppassword);
        editTextEmail = findViewById(R.id.signupemail);
        edittextpasswordlayout = findViewById(R.id.passwordTextInputLayout);
        createaccountfirebase = findViewById(R.id.createaccount);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();


        siguppageback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        loginpage.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

        googlesignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                googleSignIn();
            }
        });

        editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();

                if (password.length() >= 8) {
                    if (password.matches("[0-9]+")) {
                        edittextpasswordlayout.setHelperText("");
                        edittextpasswordlayout.setError("Password cannot contain only numbers!");
                    } else {
                        Pattern pattern = Pattern.compile("[^a-zA-Z0-9]");
                        Matcher matcher = pattern.matcher(password);
                        boolean isPwdContainsSpeChar = matcher.find();

                        if (isPwdContainsSpeChar) {
                            edittextpasswordlayout.setHelperText("Strong Password");
                            edittextpasswordlayout.setError("");
                        } else {
                            edittextpasswordlayout.setHelperText("");
                            edittextpasswordlayout.setError("Include minimum 1 special character!");
                        }
                    }
                } else {
                    edittextpasswordlayout.setHelperText("");
                    edittextpasswordlayout.setError("Enter Minimum 8 characters!");
                }
            }


            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        createaccountfirebase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                String password = editTextPassword.getText().toString();

                if (!isNetworkAvailable()) {
                    showThemedSnackbar(findViewById(android.R.id.content), "No internet connection. Please try again!");
                    return;
                }

                if (password.length() >= 8) {
                    if (password.matches("[0-9]+")) {
                        edittextpasswordlayout.setHelperText("");
                        edittextpasswordlayout.setError("Password cannot contain only numbers!");
                    } else {
                        Pattern pattern = Pattern.compile("[^a-zA-Z0-9]");
                        Matcher matcher = pattern.matcher(password);
                        boolean isPwdContainsSpeChar = matcher.find();

                        if (isPwdContainsSpeChar) {
                            edittextpasswordlayout.setHelperText("Strong Password");
                            edittextpasswordlayout.setError("");
                        } else {
                            edittextpasswordlayout.setHelperText("");
                            edittextpasswordlayout.setError("Include minimum 1 special character!");
                        }
                    }
                } else {
                    edittextpasswordlayout.setHelperText("");
                    edittextpasswordlayout.setError("Enter Minimum 8 characters!");
                }


                String email;
                email = editTextEmail.getText().toString();
                password = editTextPassword.getText().toString();

                if (email.isEmpty() && password.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Please enter email and password!", Toast.LENGTH_SHORT).show();
                    return;
                } else if (TextUtils.isEmpty(email)) {
                    Toast.makeText(SignupActivity.this, "Please enter the email!", Toast.LENGTH_SHORT).show();
                    return;
                } else if (TextUtils.isEmpty(password)) {
                    Toast.makeText(SignupActivity.this, "Please enter the password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);

                                if (task.isSuccessful()) {
                                    mAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@androidx.annotation.NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                FirebaseUser user = mAuth.getCurrentUser();
                                                if (user != null) {
                                                    String username = email.split("@")[0];
                                                    saveUserToFirestore(user.getUid(), user.getEmail(), username, "N/A");
                                                }
                                                if (mAuth.getCurrentUser().isEmailVerified()) {
                                                    Toast.makeText(SignupActivity.this, "Account Created. Please log in!",
                                                            Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                    TaskActivity.isLoggingOutManually = false;
                                                    TaskActivity.hasLoggedOutAutomatically = false;
                                                } else {
                                                    Toast.makeText(SignupActivity.this, "Acount Created. Please verify your email!", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                    TaskActivity.isLoggingOutManually = false;
                                                    TaskActivity.hasLoggedOutAutomatically = false;
                                                }
                                            } else {
                                                Toast.makeText(SignupActivity.this, "Error !", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                } else {
                                    try {
                                        throw task.getException();
                                    } catch (FirebaseAuthException e) {
                                        if (e.getErrorCode().equals("ERROR_EMAIL_ALREADY_IN_USE")) {
                                            Toast.makeText(SignupActivity.this, "Email is already registered. Please log in!", Toast.LENGTH_LONG).show();
                                        } else {
                                            // TODO: 05-01-2025 Toast message to add !
                                            Toast.makeText(SignupActivity.this, "Error !", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (Exception e) {
                                        Toast.makeText(SignupActivity.this, "Something went wrong !", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
            }
        });
    }

    private void saveUserToFirestore(String uid, String email, String name, String profileUrl) {
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.set(new User(name, email, profileUrl))
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignupActivity.this, "Failed to save user details!", Toast.LENGTH_SHORT).show();
                });
    }

    private void googleSignIn() {
        if (!isNetworkAvailable()) {
            showThemedSnackbar(findViewById(android.R.id.content), "No internet connection. Please try again!");
            return;
        }
        Intent intent = googleSignInClient.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void showThemedSnackbar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);

        View snackbarView = snackbar.getView();
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
        params.setMargins(70, 0, 70, 50);

        snackbarView.setLayoutParams(params);
        snackbar.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);
                firebaseAuth(account.getIdToken());
            } catch (Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Signup failed. Try again !", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuth(String idToken) {

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@androidx.annotation.NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);

                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                        if (task.isSuccessful()) {
                            if (user != null) {
                                String name = user.getDisplayName();
                                String email = user.getEmail();
                                String profileUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "N/A";

                                saveUserToFirestore(user.getUid(), email, name, profileUrl);

                                Intent intent = new Intent(SignupActivity.this, TaskActivity.class);
                                intent.putExtra("profileUrl", profileUrl);  // Passing profile photo URL to TaskActivity
                                startActivity(intent);
                                finish();

                                TaskActivity.isLoggingOutManually = false;
                                TaskActivity.hasLoggedOutAutomatically = false;
                            }
                            Toast.makeText(SignupActivity.this, "Signup successfully !",
                                    Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SignupActivity.this, TaskActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(SignupActivity.this, "Something went wrong !", Toast.LENGTH_SHORT).show();
                        }

                    }
                });

    }

    private void performHapticFeedback() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    public static class User {
        private String name;
        private String email;
        private String profileUrl;

        public User(String name, String email, String profileUrl) {
            this.name = name;
            this.email = email;
            this.profileUrl = profileUrl;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getProfileUrl() {
            return profileUrl;
        }

        public void setProfileUrl(String profileUrl) {
            this.profileUrl = profileUrl;
        }
    }
}