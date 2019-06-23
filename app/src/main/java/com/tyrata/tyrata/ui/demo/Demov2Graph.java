package com.tyrata.tyrata.ui.demo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.tyrata.tyrata.R;
import com.tyrata.tyrata.data.model.Graph;
import com.tyrata.tyrata.util.CommonUtil;

import at.grabner.circleprogress.CircleProgressView;
import at.grabner.circleprogress.TextMode;

/**
 * Activity to display Tire info, data graph and tire-life predictions
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Demov2Graph extends AppCompatActivity {
	private String mPos;
	private String mTireColor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_demov2_graph);

		Intent intent = getIntent();
		mPos = intent.getStringExtra("VIN");
		mTireColor = intent.getStringExtra("TIRE_COLOR");

		GraphView graphView = findViewById(R.id.graph);
		TextView x_label = findViewById(R.id.x_label);
		TextView y_label = findViewById(R.id.y_label);
		Graph graph = new Graph(graphView, "", "", mTireColor);
		graph.setGraphProperties();
		graph.setGlobalDataVariables();
		graph.plotGraph();

		setAllTireColors();
		showTireInfo();

		// Change units if clicked on X or Y axes (labels)
		x_label.setOnClickListener(view -> {
			graph.changeXUnits();
			graph.removeAllSeries();
			graph.rePlotGraph();
		});
		y_label.setOnClickListener(view -> {
			graph.changeYUnits();
			graph.removeAllSeries();
			graph.rePlotGraph();
		});

		// Redraw graph
		ImageView resetGraphBtn = findViewById(R.id.btn_reset);
		resetGraphBtn.setOnClickListener(view -> {
			graph.removeAllSeries();
			graph.rePlotGraph();
		});
	}

	/**
	 * Go back to VehicleInfo activity on back press
	 */
	@Override
	public void onBackPressed() {
		Intent vehicleInfoIntent = new Intent(Demov2Graph.this, Demov2.class);
		vehicleInfoIntent.putExtra("VIN", mPos);
		vehicleInfoIntent.putExtra("TIRE_COLOR", "");
		vehicleInfoIntent.putExtra("ALERT", false);
		startActivity(vehicleInfoIntent);
	}

	/**
	 * Updates UI; Sets colored images for tires
	 */
	private void setAllTireColors() {
		ImageView tireSelected;

		switch (mPos) {
			case "LF":
				tireSelected = findViewById(R.id.left_front);
				break;
			case "RF":
				tireSelected = findViewById(R.id.right_front);
				break;
			case "LB":
				tireSelected = findViewById(R.id.left_back);
				break;
			default:
				tireSelected = findViewById(R.id.right_back);
		}

		// IMPORTANT: The tire images MUST be named tire_color or tire_color_warn
		String tireType = "tire_" + mTireColor;
		int tireImg = getResources().getIdentifier(tireType, "drawable", getPackageName());

		// @Todo Set other tire's images after Demo
		// Set Left Front's tire image
		tireSelected.setImageResource(tireImg);
	}

	/**
	 * Updates UI; Displays tire information (thickness, life)
	 */
	private void showTireInfo() {
		// A animated circular progress bar that shows remaining tire life
		CircleProgressView tireLife = findViewById(R.id.tireinfo_life);
		tireLife.setBarColor(getBarColorCode());
		tireLife.setFillCircleColor(getFillColorCode());
		tireLife.setTextMode(TextMode.VALUE);
		tireLife.setUnitToTextScale(0.8f);
		tireLife.setUnit("days");
		tireLife.setMaxValue(900);
		tireLife.setMaxValueAllowed(900);
		tireLife.setValueAnimated(CommonUtil.tireValues[1] * 30);

		// A animated circular progress bar that shows remaining tire mileage
		CircleProgressView tireMileage = findViewById(R.id.tireinfo_mileage);
		tireMileage.setBarColor(getBarColorCode());
		tireMileage.setFillCircleColor(getFillColorCode());
		tireMileage.setTextMode(TextMode.VALUE);
		tireMileage.setUnitToTextScale(0.8f);
		tireMileage.setUnit("miles");
		tireMileage.setMaxValue(33000);
		tireMileage.setMaxValueAllowed(33000);
		tireMileage.setValueAnimated(CommonUtil.tireValues[0]);

		// A animated circular progress bar that shows tread thickness
		CircleProgressView treadThickness = findViewById(R.id.tireinfo_tread);
		treadThickness.setBarColor(getBarColorCode());
		treadThickness.setFillCircleColor(getFillColorCode());
		treadThickness.setTextMode(TextMode.VALUE);
		treadThickness.setUnitToTextScale(0.8f);
		treadThickness.setUnit("/32\"");
		treadThickness.setMaxValue(10);
		treadThickness.setMaxValueAllowed(10);
		treadThickness.setValueAnimated(CommonUtil.tireValues[2]);
	}

	public int getBarColorCode() {
		int colorCode = Color.GRAY;
		switch (mTireColor) {
			case "yellow":
				colorCode = Color.parseColor("#f9e917");
				break;
			case "green":
				colorCode = Color.parseColor("#0af10a");
				break;
			case "red":
				colorCode = Color.parseColor("#ff1010");
				break;
		}
		return colorCode;
	}

	public int getFillColorCode() {
		int colorCode = Color.GRAY;
		switch (mTireColor) {
			case "yellow":
				colorCode = Color.parseColor("#fdf7ae");
				break;
			case "green":
				colorCode = Color.parseColor("#c0ffc0");
				break;
			case "red":
				colorCode = Color.parseColor("#ffc0c0");
				break;
		}
		return colorCode;
	}
}