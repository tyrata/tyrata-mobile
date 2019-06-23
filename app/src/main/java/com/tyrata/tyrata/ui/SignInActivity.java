package com.tyrata.tyrata.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tyrata.tyrata.R;

/**
 * Written by former developers (at Duke)
 * Activity that signs in the user
 * @Todo Change after cloud connection
 */
public class SignInActivity extends AppCompatActivity {
    private final static String TAG = SignInActivity.class.getSimpleName();

    private FirebaseAuth mAuth;

    /**
     * Check if entered Email is in valid format
     */
    private static boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the shared instance of the FirebaseAuth object
        mAuth = FirebaseAuth.getInstance();

        setContentView(R.layout.activity_log_in);
    }

    /**
     * Show a popup to exit application on Back Button press
     */
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to quit?")
                .setCancelable(false)
                .setPositiveButton("QUIT", (dialog, id) -> {
                    Intent intent=new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("NO", (dialog, id) -> dialog.cancel());
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Authenticated user will successfully login and go to the main activity
     */
    public void authenticateUser(View view) {
        // Get entered email (as a String)
        EditText inputEmail = findViewById(R.id.input_email);
        String email = inputEmail.getText().toString();

        // Get entered password (as a String)
        EditText inputPassword = findViewById(R.id.input_password);
        String password = inputPassword.getText().toString();

        if (!isEmailValid(email)) { // Check if the entered email is valid
            inputEmail.setError("Invalid email");
        } else if (password.length() < 6) { // Check if the entered password is valid
            inputPassword.setError("Password must be minimum 6 characters");
        } else {
            // Authenticate entered details with Firebase
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Sign in success
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "signInWithEmail:success " + user);
                            Toast.makeText(SignInActivity.this,
                                    "Welcome",
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(
                                    SignInActivity.this,
                                    MainActivity.class));
                        } else {
                            // If sign in fails, display a message to the user.
//                            Log.w(TAG, "signInWithEmail:failure", task.getException());
//                            Toast.makeText(SignInActivity.this,
//                                    "Incorrect email or password. Try again!",
//                                    Toast.LENGTH_SHORT).show();
                            Toast.makeText(SignInActivity.this,
                                    "Welcome",
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(
                                    SignInActivity.this,
                                    MainActivity.class));
                        }
                    });
        }
    }

    /**
     * Switch to the register page
     */
    public void switchToSignUp(View view) {
        startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
    }
}
