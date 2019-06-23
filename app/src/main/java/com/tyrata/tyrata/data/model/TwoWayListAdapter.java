package com.tyrata.tyrata.data.model;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.tyrata.tyrata.R;

import java.util.ArrayList;

/**
 * Custom List Adapter for displaying text left and right
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class TwoWayListAdapter extends BaseAdapter {
	private Context context;

	private boolean showDeviceIcon;
	private boolean showGearIcon;
	private boolean showMiddleValue;
	private ArrayList<String> left;
	private ArrayList<String> middle;
	private ArrayList<String> right;
	private static LayoutInflater inflater = null;

	// Firebase variables
//	private FirebaseAuth mAuth;
//	private FirebaseUser mUser;
//	private FirebaseDatabase mDatabase;
//	private DatabaseReference mSensorMetaRef;

	public TwoWayListAdapter(Context context,
							 ArrayList<String> left,
							 ArrayList<String> right,
							 ArrayList<String> middle,
							 boolean showDeviceIcon,
							 boolean showGearIcon,
							 boolean showMiddleValue) {
		this.context = context;
		this.left = left;
		this.middle = middle;
		this.right = right;
		this.showDeviceIcon = showDeviceIcon; // show device icon or not
		this.showGearIcon = showGearIcon; // show gear icon or not
		this.showMiddleValue = showMiddleValue; // show middle value (for difference) or not

		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// Get the shared instance of the FirebaseAuth object
//		mAuth = FirebaseAuth.getInstance();
//		mUser = mAuth.getCurrentUser(); // Get Current User
//		mDatabase = FirebaseDatabase.getInstance(); // Get Database Instance
//		// Get reference to the "sensors" node in the JSON tree (See Firebase console for more info)
//		mSensorMetaRef = mDatabase.getReference("users").child(mUser.getUid()).child("sensors");
	}

	@Override
	public int getCount() {
		return right.size();
	}

	@Override
	public Object getItem(int i) {
		return right.get(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		View vi = view;
		if (vi == null)
			vi = inflater.inflate(R.layout.item_sensor, null);

		ImageView deviceIcon = vi.findViewById(R.id.ic_device);
		ImageView gearIcon = vi.findViewById(R.id.ic_settings);
		TextView leftText = vi.findViewById(R.id.data_time);
		TextView middleText = vi.findViewById(R.id.data_capacitance);
		TextView rightText = vi.findViewById(R.id.data_voltage);

		// Name change dialog if gear icon is pressed
	//	gearIcon.setOnClickListener(view1 -> showNameChangeDialog(i));

		if (!showDeviceIcon) deviceIcon.setVisibility(View.GONE); // Hide Phone icon
		if (!showGearIcon) gearIcon.setVisibility(View.GONE); // Hide Gear icon
		if (!showMiddleValue) {
			LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
					0,
					ViewGroup.LayoutParams.MATCH_PARENT,
					2
			);
			rightText.setLayoutParams(param);
			middleText.setVisibility(View.GONE);
		} else middleText.setText(middle.get(i));

		leftText.setText(left.get(i));
		rightText.setText(right.get(i));

		return vi;
	}

	/**
	 * Show popup to change name when clicked on GEAR icon
	 *
	 * @param i ID of the list item (sensor)
	 */
//	private void showNameChangeDialog(int i) {
//		EditText input = new EditText(context);
//		input.setHint("Enter between 6 and 20 characters");
//		input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
//
//		AlertDialog alert =
//				new AlertDialog.Builder(context)
//						.setTitle("Change Sensor Name")
//						.setMessage("Characters remaining: 20")
//						.setView(input)
//						.setPositiveButton("OK", null)
//						.setNegativeButton("CANCEL", null)
//						.create();
//
//
//		// Live characters-remaining count
//		TextWatcher mTextEditorWatcher = new TextWatcher() {
//			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//			}
//
//			public void onTextChanged(CharSequence s, int start, int before, int count) {
//				alert.setMessage("Characters remaining: " + (20 - s.length()));
//			}
//
//			public void afterTextChanged(Editable s) {
//			}
//		};
//
//		input.addTextChangedListener(mTextEditorWatcher);
//
//		alert.setOnShowListener(dialogInterface -> {
//			Button okBtn = alert.getButton(DialogInterface.BUTTON_POSITIVE);
//			okBtn.setOnClickListener(view1 -> {
//				String newSensorName = input.getText().toString();
//				if (!newSensorName.equals("") && newSensorName.length() >= 6) {
//					String macAddress = right.get(i);
//
//					mSensorMetaRef.addListenerForSingleValueEvent(new ValueEventListener() {
//						@Override
//						public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//							boolean isNameTaken = false;
//							for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
//								// If name is already taken (already in DB), show error
//								if (snapshot.child("name").getValue().equals(newSensorName)) {
//									isNameTaken = true;
//									break;
//								}
//							}
//
//							if (isNameTaken)
//								input.setError("The name entered is already taken");
//							else { // else update displayed devices list
//								mSensorMetaRef.child(macAddress)
//										.child("name").setValue(newSensorName);
//								alert.dismiss();
//								Toast.makeText(context,
//										"Name change success!",
//										Toast.LENGTH_SHORT).show();
//							}
//						}
//
//						@Override
//						public void onCancelled(@NonNull DatabaseError databaseError) {
//						}
//					});
//				} else // If new name is less than 6 characters, show error
//					input.setError("The name cannot be less than 6 characters");
//			});
//		});
//		alert.show();
//	}
}