package com.tyrata.tyrata.ui.demo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.tyrata.tyrata.ui.MainActivity;
import com.tyrata.tyrata.R;

/**
 * Static version for Demo
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Demov2 extends AppCompatActivity {
	private final static String leftFrontColor = "red";
	private final static String rightFrontColor = "yellow";
	private final static String leftBackColor = "green";
	private final static String rightBackColor = "green";
	private Rect mRect;

	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_demov2);

		setTirePropertiesAndListeners();
	}

	/**
	 * Go back to MainActivity on back button press
	 */
	@Override
	public void onBackPressed() {
		startActivity(
				new Intent(Demov2.this, MainActivity.class));
	}

	/**
	 * Does UI updates; Sets colored images for tires (based on color and alert type)
	 */
	private void setTirePropertiesAndListeners() {
		ImageView leftFront = findViewById(R.id.left_front);
		ImageView rightFront = findViewById(R.id.right_front);
		ImageView leftBack = findViewById(R.id.left_back);
		ImageView rightBack = findViewById(R.id.right_back);

		leftFront.setImageResource(getTireImgId(false, leftFrontColor));
		rightFront.setImageResource(getTireImgId(false, rightFrontColor));
		leftBack.setImageResource(getTireImgId(false, leftBackColor));
		rightBack.setImageResource(getTireImgId(false, rightBackColor));

		leftFront.setOnTouchListener((view, motionEvent) -> {
			tireClickHandler(motionEvent, leftFront, leftFrontColor, "LF");
			return true;
		});
		rightFront.setOnTouchListener((view, motionEvent) -> {
			tireClickHandler(motionEvent, rightFront, rightFrontColor, "RF");
			return true;
		});
		leftBack.setOnTouchListener((view, motionEvent) -> {
			tireClickHandler(motionEvent, leftBack, leftBackColor, "LB");
			return true;
		});
		rightBack.setOnTouchListener((view, motionEvent) -> {
			tireClickHandler(motionEvent, rightBack, rightBackColor, "RB");
			return true;
		});
	}

	/**
	 * Handles Tire Click animation
	 */
	private void tireClickHandler(MotionEvent motionEvent, ImageView imageView, String color, String pos) {
		// Get id of colored tire image
		int idOriginal = getTireImgId(false, color);
		// Get id of darker colored tire image (to represent pressed)
		int idWhenClicked = getTireImgId(true, color);

		if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) { // Finger on image
			// Drawing a rectangle using image view's boundaries
			mRect = new Rect(
					imageView.getLeft(),
					imageView.getTop(),
					imageView.getRight(),
					imageView.getBottom());
			// Set to darker image (to represent pressed)
			imageView.setImageResource(idWhenClicked);
		}

		if (motionEvent.getAction() == MotionEvent.ACTION_UP) { // Finger lifted up from image
			// Finger is not moved away from image and lifted/released
			if (mRect.contains(imageView.getLeft() + (int) motionEvent.getX(),
					imageView.getTop() + (int) motionEvent.getY())) {
				// Set tire's image to original image (non-pressed state)
				imageView.setImageResource(idOriginal);
				startTireInfoActivity(color, pos);
			}
		}

		if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) { // Finger moved away from image
			if (!mRect.contains(imageView.getLeft() + (int) motionEvent.getX(),
					imageView.getTop() + (int) motionEvent.getY())) {
				// Set tire's image to original image (non-pressed state)
				imageView.setImageResource(idOriginal);
			}
		}
	}

	/**
	 * Start TireInfoDisplayActivity activity (called by tireClickHandler)
	 */
	private void startTireInfoActivity(String color, String pos) {
		Intent intent = new Intent(Demov2.this, Demov2Graph.class);
		intent.putExtra("VIN", pos);
		intent.putExtra("TIRE_COLOR", color);
		intent.putExtra("ALERT", false);
		startActivity(intent);
	}

	/**
	 * Tire Image Name Getter
	 */
	private int getTireImgId(boolean isClicked, String color) {
		String tireImg = "tire_" + color;
		tireImg += isClicked ? "_clicked" : "";
		return getResources().getIdentifier(tireImg, "drawable", getPackageName());
	}
}

