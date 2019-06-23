package com.tyrata.tyrata.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tyrata.tyrata.R;

import java.io.FileNotFoundException;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Written by former developers (at Duke)
 * Activity to register user
 * @Todo Change after cloud connection
 */
public class SignUpActivity extends AppCompatActivity {
	private static final String TAG = SignUpActivity.class.getSimpleName();
	private static final int GET_FROM_GALLERY = 4;

	private CircleImageView mProfilePic;
	private Bitmap mBitmap;

	private FirebaseAuth mAuth;

	/**
	 * Check if entered Email is in valid format
	 */
	private static boolean isEmailValid(CharSequence email) {
		return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
	}

	/**
	 * Handles output from GET_FROM_GALLERY
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// If a picture is chosen from gallery, convert it to a BitMap
		if(requestCode == GET_FROM_GALLERY && resultCode == Activity.RESULT_OK) {
			// Get the selected image's path
			Uri selectedImage = data.getData();
			// Check if nothing is selected (will never be false)
			assert selectedImage != null;

			try {
				// Image(.png or .jpg) to Bitmap conversion
				mBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			// Show the image preview in a circular view
			mProfilePic.setImageBitmap(mBitmap);
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);

		// Get the shared instance of the FirebaseAuth object
		mAuth = FirebaseAuth.getInstance();

		// Get the default image (for profile picture)
		mBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.add_image);

		// Open Gallery if clicked on add-profile-image button
		mProfilePic = findViewById(R.id.register_upload);
		mProfilePic.setOnClickListener(view ->
				startActivityForResult(
						new Intent(Intent.ACTION_PICK,
								MediaStore.Images.Media.INTERNAL_CONTENT_URI),
						GET_FROM_GALLERY)
		);
	}

	/**
	 * Validates form fields
	 */
	public void registerUser(View view) {

		EditText userNameEditText = findViewById(R.id.edittext_register_username);
		String userName = userNameEditText.getText().toString();

		EditText emailEditText = findViewById(R.id.edittext_register_email);
		String email = emailEditText.getText().toString();

		EditText phoneEditText = findViewById(R.id.edittext_register_phone);
		String phone = phoneEditText.getText().toString();

		EditText passwordEditText = findViewById(R.id.edittext_register_password);
		String password = passwordEditText.getText().toString();

		EditText confirmPasswordEditText = findViewById(R.id.edittext_register_confirmpass);
		String confirmPassword = confirmPasswordEditText.getText().toString();

		if (userName.length() < 6) { // Check if the username is valid
			userNameEditText.setError("Username must be minimum 6 characters");
		} else if (!isEmailValid(email)) { // Check if the entered email is valid
			emailEditText.setError("Invalid email");
		} else if (!isPhoneValid(phone)) { // Check if the entered phone number is valid
			phoneEditText.setError("Invalid phone number");
		} else if (password.length() < 6) { // Check if the entered password is valid
			passwordEditText.setError("Password must be minimum 6 characters");
		} else if (!password.equals(confirmPassword)) { // Check if the passwords match
			confirmPasswordEditText.setError("Passwords doesn't match");
		} else {
			// Firebase Sign-up
			mAuth.createUserWithEmailAndPassword(email, password)
					.addOnCompleteListener(this, task -> {
						if (task.isSuccessful()) {
							FirebaseUser user = mAuth.getCurrentUser();
							String uniqueId = user.getUid();

							Log.d(TAG, "createUserWithEmail:success " + user);
							Toast.makeText(SignUpActivity.this,
									"Account created. Logging you in!",
									Toast.LENGTH_SHORT).show();
							startActivity(new Intent(
									SignUpActivity.this,
									MainActivity.class));

						} else {
							// If sign in fails, display a message to the user.
							Log.w(TAG, "createUserWithEmail:failure", task.getException());
							Toast.makeText(SignUpActivity.this,
									"Account with the email already exists!",
									Toast.LENGTH_SHORT).show();
						}
					});
		}
	}

	/**
	 * Check if the phone number is valid
	 */
	private boolean isPhoneValid(String phone_number) {
		return PhoneNumberUtils.isGlobalPhoneNumber(phone_number);
	}
}
