package com.wernicke.android.heracles;

import org.json.JSONException;
import org.json.JSONObject;

import com.wernicke.android.utils.Utils;
import com.wernicke.heracles.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class DeviceInfoActivity extends Activity {
	Context context;
	public ProgressDialog pDialog;
	String uuid,make,model,carrier,rom,version,user;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device);
		context = this;
		
		// android id
		uuid = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
		TextView uuidText = (TextView) findViewById(R.id.uuid);
		uuidText.setText(uuid);

		// make/model
		make = Build.MANUFACTURER;
		model = Build.MODEL;
		TextView modelText = (TextView) findViewById(R.id.model);
		modelText.setText(Utils.toDeviceName(make, model));

		// carrier
		carrier = Build.BRAND;
		TextView carrierText = (TextView) findViewById(R.id.carrier);
		carrierText.setText(carrier);

		// set rom
		rom = Build.DISPLAY;
		TextView romText = (TextView) findViewById(R.id.rom);
		romText.setText(rom);

		// set os version
		version = Build.VERSION.RELEASE;
		TextView osVersionText = (TextView) findViewById(R.id.osVersion);
		osVersionText.setText(version);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.device_info, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case R.id.action_view_permissions:
			i = new Intent(context, PermissionListActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT // bring activity to front of task stack
					| Intent.FLAG_ACTIVITY_NEW_TASK); // or start new activity
			context.startActivity(i);
			return true;
		case R.id.action_view_packages:
			i = new Intent(context, PackageListActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT // bring activity to front of task stack
					| Intent.FLAG_ACTIVITY_NEW_TASK); // or start new activity
			context.startActivity(i);
			return true;
		case R.id.action_submit_device:
			new SubmitDevice().execute();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	class SubmitDevice extends AsyncTask<String, String, String> {

		/**
		 * Before starting background thread, show progress dialog.
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(context);
			pDialog.setMessage("Submitting device...");
			pDialog.setIndeterminate(true);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		@Override
		protected String doInBackground(String... params) {
			JSONObject json = new JSONObject();
			JSONObject device = new JSONObject();
			try {
				device.put("uuid", uuid);
				device.put("make", make);
				device.put("model", model);
				device.put("carrier", carrier);
				device.put("rom", rom);
				device.put("version", version);
				json.accumulate("device", device);
				Log.d("submit device",json.toString());
			} catch (JSONException e) {
				Log.e("submit device", "could not build JSON");
			}
			Utils.submitDevice(uuid, make, model, carrier, rom, version, null);
			return null;
		}
		
		/**
		 * After completing background task dismiss the progress dialog
		 */
		@Override
		protected void onPostExecute(String result) {
			// dismiss the dialog once done
			pDialog.dismiss();
		}
	}
}
