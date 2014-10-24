package com.wernicke.android.heracles;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.wernicke.android.utils.Utils;
import com.wernicke.heracles.R;
import com.wernicke.utils.JSONParser;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class PackageListActivity extends Activity {

	public static final String TAG_SUCCESS = "success";
	ProgressDialog pDialog;
	AlertDialog aDialog;

	public static Context context;
	public static PackageManager pm;
	
	public static ArrayList<PackageInfo> packages; // list of installed packages
	String uuid,make,model,carrier,rom,version,user;

	PackageListAdapter adapter; // package list view adapter
	
	NotificationManager mNotifyManager;
	Builder mBuilder;
	Intent intent;
	PendingIntent pIntent;
	final static int notifyID = 1;

	JSONParser jsonParser = new JSONParser();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.package_list_layout);

		// get device info
		uuid = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
		make = Build.MANUFACTURER;
		model = Build.MODEL;
		carrier = Build.BRAND;
		rom = Build.DISPLAY;
		version = Build.VERSION.RELEASE;

		// set the title bar to show packages
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
			ActionBar actionBar = getActionBar();
			actionBar.setIcon(R.drawable.ic_package);
			actionBar.setTitle("Packages");
		} else {
			this.setTitle("Packages");
		}

		// global state vars
		context = this;
		pm = context.getPackageManager();
		
		// gets the list of packages and creates the package list adapter
		new EnumeratePackages().execute();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.package_list_view, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case R.id.action_view_permissions:
			i = new Intent(context, PermissionListActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT // bring activity
																// to front of
																// task stack
					| Intent.FLAG_ACTIVITY_NEW_TASK); // or start new activity
			context.startActivity(i);
			return true;
		case R.id.action_view_device:
			i = new Intent(context, DeviceInfoActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT // bring activity
																// to front of
																// task stack
					| Intent.FLAG_ACTIVITY_NEW_TASK); // or start new activity
			context.startActivity(i);
			return true;
		case R.id.action_submit_full_report:
			// upload everything
			new SubmitFullReport().execute();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Background AsyncTask to enumerate package infos.
	 */
	class EnumeratePackages extends AsyncTask<String, String, String> {

		/**
		 * Before starting background thread, show progress dialog.
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(context);
			pDialog.setMessage("Enumerating packages...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		/**
		 * Get all packages installed on this device.
		 */
		@Override
		protected String doInBackground(String... params) {
			packages = (ArrayList<PackageInfo>) pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
			adapter = new PackageListAdapter((Activity) context, packages);
			return null;
		}

		/**
		 * After completing background task Dismiss the progress dialog
		 */
		@Override
		protected void onPostExecute(String result) {
			// dismiss the dialog once done
			pDialog.dismiss();
			// cannot get the listview inside the background thread
			ListView listView = (ListView) findViewById(R.id.list);

			// Assign adapter to ListView
			listView.setAdapter(adapter);
			// setListAdapter(adapter);

			listView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					Intent i = new Intent(context, PackageViewActivity.class);
					i.putExtra("packageName", packages.get(position).packageName);
					i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT // bring activity to front of task stack
							| Intent.FLAG_ACTIVITY_NEW_TASK); // or start new activity
					context.startActivity(i);

				}
			});

			TextView numView = (TextView) findViewById(R.id.num_packages);
			numView.setText(String.format("Found %d packages", packages.size()));

		}

	}

	/**
	 * Send all the package infos.
	 */
	class SubmitFullReport extends AsyncTask<String, String, String> {

		/**
		 * Before starting background thread, show progress dialog.
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(context);
			pDialog.setMessage("Uploading report...");
			pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pDialog.setProgress(0);
			pDialog.setMax(100);
			pDialog.setCancelable(true);
			pDialog.show();
			
			mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mBuilder = new NotificationCompat.Builder(context);
			//mBuilder.setContentTitle("Uploading report...");
			mBuilder.setContentTitle("Uploading report...");
			mBuilder.setSmallIcon(R.drawable.ic_launcher);
			mBuilder.setProgress(100, 0, false);
			intent = new Intent(context, PackageListActivity.class);
			pIntent = PendingIntent.getActivity(context, 0, intent, 0);
			mBuilder.setContentIntent(pIntent);
			mNotifyManager.notify(notifyID, mBuilder.build());
		}

		@Override
		protected String doInBackground(String... args) {
			int i = 0, progress = 0;
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
			// submit device
			// Utils.submitDevice(uuid, make, model, carrier, rom, version, null);
			// submit all packages
			for (PackageInfo pkg : packages) {
				/*
				JSONObject pakg = new JSONObject();
				try {
					pakg.put("checksum", Utils.getChecksum(pkg));
				} catch (JSONException e) {
					Log.e("submit package", "could not build JSON");
				}
				Utils.submitPackage(pkg, uuid);
				*/
				JSONObject packagesJson = new JSONObject();
				try {
					JSONObject packageJson = new JSONObject();
					packageJson.put("name", pkg.packageName);
					try {
						packageJson.put("label",pm.getApplicationLabel(pkg.applicationInfo).toString());
					} catch (Exception e) {
						packageJson.put("label",pkg.packageName);
					}
					String checksum = Utils.getChecksum(pkg.applicationInfo.sourceDir);
					if (checksum == null) checksum = Utils.getChecksum(pkg);
					packageJson.put("versionName",pkg.versionName);
					packageJson.put("version",pkg.versionCode);
					int size = 0;
					try {
						size = Utils.getSize(pkg.applicationInfo.sourceDir);
					} catch (Exception e) {
						e.getMessage();
					}
					packageJson.put("size",size);
					packageJson.put("targetSdk",pkg.applicationInfo.targetSdkVersion);
					packageJson.put("lastUpdate",pkg.lastUpdateTime);
					for (PermissionInfo permission : pkg.permissions) {
						packageJson.accumulate("permissions", permission.name);
					}
					packagesJson.put(checksum, packageJson);
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				Log.d("submit package", packagesJson.toString());
				
				i++;
				progress = i * 100 / packages.size();
				pDialog.setProgress(progress);
				mBuilder.setProgress(100, progress, false);
				mNotifyManager.notify(notifyID, mBuilder.build());
			}
			return null;
		}

		/**
		 * After completing background task dismiss the progress dialog
		 */
		@Override
		protected void onPostExecute(String result) {
			// dismiss the dialog once done
			pDialog.dismiss();
			mNotifyManager.cancel(notifyID);
		}
	}
}
