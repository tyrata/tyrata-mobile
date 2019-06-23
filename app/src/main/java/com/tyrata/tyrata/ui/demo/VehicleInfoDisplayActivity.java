package com.tyrata.tyrata.ui.demo;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import com.tyrata.tyrata.ui.MainActivity;
import com.tyrata.tyrata.R;
import com.tyrata.tyrata.data.remote.BluetoothLeService;
import com.tyrata.tyrata.util.CommonUtil;



/**
 * Activity to display Vehicle info, tire colors and tire info
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class VehicleInfoDisplayActivity extends AppCompatActivity {
	private final static String TAG = VehicleInfoDisplayActivity.class.getSimpleName();

	private BluetoothLeService mBluetoothLeService;

	private String mVin;
	private String mTireColor;
	private boolean mIsTireAlert;
	private Rect mRect;

	/**
	 * Code to manage Service lifecycle.
	 */
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			// Asynchronous call; BLE Service is successfully created/bound to the activity
//			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
//			if (mBluetoothLeService.initialize()) {
//				Log.d(TAG, "Initialized BLE Service");
//			} else {
//				Log.e(TAG, "Unable to initialize Bluetooth");
//				finish();
//			}
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	/**
	 * Handles various events fired by the Service.
	 * ACTION_DATA_AVAILABLE: received data from the sensor.
	 * ACTION_INACTIVITY_FOUND: sensor is inactive
	 * ACTION_DEVICE_FOUND: sensor is active
	 * If data available, update UI (tire colors, info and animations)
	 */
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				// Get broadcast message (data) from BluetoothLeService
				String encodedMsg = intent.getStringExtra(BluetoothLeService.ACTION_DATA_AVAILABLE);
				// Decode numeric message to color (Eg. "80" to "green")
				String decodedMsg = CommonUtil.decodeMessage(encodedMsg);

				if (decodedMsg.equals("default")) {
					mIsTireAlert = true; // required for setting alert icon
				} else {
					if(!mTireColor.equals(decodedMsg))
						// Changed color; animate arrow to represent update
						animateTire();
					mTireColor = decodedMsg;
					mIsTireAlert = false;
				}

				final ImageView leftFront = findViewById(R.id.left_front);
				// Set tire colors
				setTireProperties(leftFront, mTireColor, mIsTireAlert);

			} // If an inactive device is found, show exclamation symbol
			else if (BluetoothLeService.ACTION_INACTIVITY_FOUND.equals(action)) {
				final ImageView leftFront = findViewById(R.id.left_front);
				// Set exclamation symbol for tire indication disconnection
				setTireProperties(leftFront, mTireColor, true);
				// Display a message saying that the sensor disconnected
				Toast.makeText(VehicleInfoDisplayActivity.this,
						"Sensor Disconnected",
						Toast.LENGTH_SHORT).show();
			} // If a sensor is found, show message that says Sensor is connected
			else if (BluetoothLeService.ACTION_DEVICE_FOUND.equals(action)) {
				Toast.makeText(VehicleInfoDisplayActivity.this,
						"Sensor Connected",
						Toast.LENGTH_SHORT).show();
			}
		}
	};

	/**
	 * Intent Filter for the BLE Service.
	 * Add user-defined GATT connection states here.
	 */
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.ACTION_DEVICE_FOUND);
		intentFilter.addAction(BluetoothLeService.ACTION_INACTIVITY_FOUND);
		return intentFilter;
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Unbind service and free variables (to avoid dangling)
		unbindService(mServiceConnection);
		mBluetoothLeService = null;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_vehicle_info);

		// Start or bind to the BLE background service
		Intent gattServiceIntent =
				new Intent(VehicleInfoDisplayActivity.this, BluetoothLeService.class);
		startService(gattServiceIntent);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

		// Extract strings from the intents
		Intent intent = getIntent();
		mVin = intent.getStringExtra("VIN");
		mTireColor = intent.getStringExtra("TIRE_COLOR");
		mIsTireAlert = intent.getBooleanExtra("ALERT", true);

		// Set tire to obtained (or default) color and show its stats
		final ImageView leftFront = findViewById(R.id.left_front);
		setTireProperties(leftFront, mTireColor, mIsTireAlert);
//        showTireStats();

		// Set touch listeners for the tires
		leftFront.setOnTouchListener((view, motionEvent) -> {
			tireClickHandler(motionEvent, leftFront);
			return true;
		});
	}

	/**
	 * Go back to MainActivity on back button press
	 */
	@Override
	public void onBackPressed() {
		startActivity(
				new Intent(VehicleInfoDisplayActivity.this, MainActivity.class));
	}

	/**
	 * Does UI updates; Sets colored images for tires (based on color and alert type)
	 */
	private void setTireProperties(ImageView tireImg, String tireColor, boolean isTireAlert) {
		this.mTireColor = tireColor;
		this.mIsTireAlert = isTireAlert;
		tireImg.setImageResource(getTireImgId(false));
	}

	/**
	 * Arrow animation (indicates new data received)
	 */
	private void animateTire() {
		AnimationSet tireAnim = new AnimationSet(true);

		// zoom out animation
		final ImageView tireImg = findViewById(R.id.left_front);
		Animation scaleAnim = new ScaleAnimation(
				2f, 0.7f, // Start and end values for the X axis scaling
				2f, 0.7f, // Start and end values for the Y axis scaling
				Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
				Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
		scaleAnim.setDuration(1000);
		tireAnim.addAnimation(scaleAnim);

		// zoom in animation
		scaleAnim = new ScaleAnimation(
				0.7f, 2f, // Start and end values for the X axis scaling
				0.7f, 2f, // Start and end values for the Y axis scaling
				Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
				Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
		scaleAnim.setDuration(1000);
		scaleAnim.setStartOffset(1000);
		tireAnim.addAnimation(scaleAnim);

		tireImg.startAnimation(tireAnim);
	}

	/**
	 * Handles Tire Click animation
	 */
	private void tireClickHandler(MotionEvent motionEvent, ImageView imageView) {
		if(mTireColor.equals("default")) // Do nothing if clicked on the default (Gray) button
			return;

		// Get id of colored tire image
		int idOriginal = getTireImgId(false);
		// Get id of darker colored tire image (to represent pressed)
		int idWhenClicked = getTireImgId(true);

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
				startTireInfoActivity();
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
	private void startTireInfoActivity() {
		Intent intent = new Intent(VehicleInfoDisplayActivity.this, TireInfoDisplayActivity.class);
		intent.putExtra("VIN", mVin);
		intent.putExtra("TIRE_COLOR", mTireColor);
		intent.putExtra("ALERT", mIsTireAlert);
		startActivity(intent);
	}

	/**
	 * Tire Image Name Getter
	 * IMPORTANT: The tire images MUST be named tire_color or tire_color_warn
	 * @param isClicked boolean value indicating tire press
	 * @return If isClicked true, return tire_color_warn's ID else tire_color's ID
	 */
	private int getTireImgId(boolean isClicked) {
		String tireImg = mIsTireAlert ? "tire_"+ mTireColor +"_warn" : "tire_"+ mTireColor;
		tireImg += (isClicked && !mTireColor.equals("default")) ? "_clicked" : "";
		return getResources().getIdentifier(tireImg, "drawable", getPackageName());
	}
}