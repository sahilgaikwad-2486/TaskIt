package com.studiox.taskit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    TextInputEditText editTextEmail, editTextPasword;
    LinearLayout loginFirebase, backbutton, googlelogin;
    TextView forgetPassword;
    String email;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    GoogleSignInClient googleSignInClient;
    LinearProgressIndicator progressBar;
    int RC_SIGN_IN = 20;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        progressBar = findViewById(R.id.progressBar);
        backbutton = findViewById(R.id.loginpagebackbutton);
        editTextEmail = findViewById(R.id.loginemail);
        editTextPasword = findViewById(R.id.loginpassword);
        loginFirebase = findViewById(R.id.loginbutton);
        forgetPassword = findViewById(R.id.forgetpasswordbutton);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        googlelogin = findViewById(R.id.logingooglebutton);


        backbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

        googlelogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                googleLogin();
            }
        });

        loginFirebase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                if (!isNetworkAvailable()) {
                    showThemedSnackbar(findViewById(android.R.id.content), "No internet connection. Please try again!");
                    return;
                }

                String email, password;
                email = editTextEmail.getText().toString();
                password = editTextPasword.getText().toString();

                if (email.isEmpty() && password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter email and password!", Toast.LENGTH_SHORT).show();
                    return;
                } else if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LoginActivity.this, "Please enter the email!", Toast.LENGTH_SHORT).show();
                    return;
                } else if (TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, "Please enter the password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);

                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null && user.isEmailVerified()) {
                                        Toast.makeText(LoginActivity.this, "Login successfully!",
                                                Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(LoginActivity.this, TaskActivity.class);
                                        startActivity(intent);
                                        finish();

                                        TaskActivity.isLoggingOutManually = false;
                                        TaskActivity.hasLoggedOutAutomatically = false;
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Please verify your email!", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    try {
                                        throw task.getException();
                                    } catch (FirebaseAuthInvalidCredentialsException e) {
                                        Toast.makeText(LoginActivity.this, "Wrong email or password entered. Please check!", Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        Log.e("Firebase Signin", e.getMessage());
                                    }
                                }
                            }
                        });
            }
        });

        forgetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email = editTextEmail.getText().toString();

                if (email.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter the email to reset the password!", Toast.LENGTH_SHORT).show();
                } else {
                    ResetPassword();
                }
            }
        });

    }

    private void googleLogin() {
        if (!isNetworkAvailable()) {
            showThemedSnackbar(findViewById(android.R.id.content), "No internet connection. Please try again!");
            return;
        }
        Intent intent = googleSignInClient.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    private void showThemedSnackbar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);

        View snackbarView = snackbar.getView();
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
        params.setMargins(70, 0, 70, 50);

        snackbarView.setLayoutParams(params);
        snackbar.show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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
                Toast.makeText(this, "Login failed. Try again!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuth(String idToken) {

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);

                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                        if (task.isSuccessful()) {
                            if (user != null) {
                                String name = user.getDisplayName();
                                String email = user.getEmail();
                                String profileUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "N/A";

                                saveUserToFirestore(user.getUid(), email, name, profileUrl);

                                Intent intent = new Intent(LoginActivity.this, TaskActivity.class);
                                intent.putExtra("profileUrl", profileUrl);  // Passing profile photo URL to TaskActivity
                                startActivity(intent);
                                finish();
                            }
                            Toast.makeText(LoginActivity.this, "Login successfully !",
                                    Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, TaskActivity.class);
                            startActivity(intent);
                            finish();

                            TaskActivity.isLoggingOutManually = false;
                            TaskActivity.hasLoggedOutAutomatically = false;
                        } else {
                            Toast.makeText(LoginActivity.this, "Something went wrong !", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserToFirestore(String uid, String email, String name, String profileUrl) {
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.set(new SignupActivity.User(name, email, profileUrl))
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Failed to save user details!", Toast.LENGTH_SHORT).show();
                });
    }

    private void ResetPassword() {
        mAuth.sendPasswordResetEmail(email).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(LoginActivity.this, "Password reset email sent. Check you inbox!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(LoginActivity.this, "Failed to send reset email!", Toast.LENGTH_SHORT).show();
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

    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
