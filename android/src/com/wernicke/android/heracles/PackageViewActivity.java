package com.wernicke.android.heracles;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.wernicke.android.heracles.PackageListActivity.SubmitFullReport;
import com.wernicke.android.utils.AndroidUtils;
import com.wernicke.android.utils.Utils;
import com.wernicke.android.utils.RootTools.ResultCommand;
import com.wernicke.heracles.BuildConfig;
import com.wernicke.heracles.R;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.Settings.Secure;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class PackageViewActivity extends Activity {

	private static final int TIMEOUT = 100000;
	Context context;
	PackageManager pm;
	ProgressDialog pDialog;
	AlertDialog aDialog;
	PermissionListAdapter adapter; // permission list view adapter
	PackageInfo pkg; // the package we're viewing

	Package app;
	String name; // fully qualified name of package
	String label; // user displayed name of package
	String versionName; // package version code/label
	int versionCode;
	String created, location; // created/updated date & install location
	String sdk; // min/target sdk
	int targetSdk, minSdk;
	String checksum; // package md5 hash
	int size; // package size
	String category;
	float rating;
	int votes;
	int downloads;
	float price;
	String author;
	
	JSONObject json;
	
	String uuid,make,model,carrier,rom,version,user;

	ArrayList<PermissionInfo> requestedPermissions; // permissions requested by
													// this package
	
	private ShareActionProvider mShareActionProvider;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.package_view_layout);
		context = this;
		pm = context.getPackageManager();
		name = getIntent().getStringExtra("packageName");
		
		// get device info
		uuid = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
		make = Build.MANUFACTURER;
		model = Build.DISPLAY;
		carrier = Build.BRAND;
		rom = Build.DISPLAY;
		version = Build.VERSION.RELEASE;

		// all things that require PackageInfo go in the following block
		try {
			pkg = pm.getPackageInfo(name, PackageManager.GET_PERMISSIONS);
		} catch (Exception e) {
			// TODO: show an alert dialog that package could not be found &
			// return to previous activity
			this.finish();
		}
		/*
		 * // TODO: Have to create a custom title layout for this to work = LAME // set the window icon this.requestWindowFeature(Window.FEATURE_LEFT_ICON);
		 * getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, pkg.applicationInfo.icon);
		 */
		// get the package label
		label = pm.getApplicationLabel(pkg.applicationInfo).toString();

		// set the title bar to show app title & icon
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
			ActionBar actionBar = getActionBar();
			actionBar.setIcon(pkg.applicationInfo.loadIcon(pm));

			// set the window title to the package title
			actionBar.setTitle(label);
			
			created = new Date(pkg.lastUpdateTime).toString();
		} else {
			this.setTitle(label);
			created = new Date(new File(pkg.applicationInfo.sourceDir).lastModified()).toString();
		}

		// display the package name
		TextView textView = (TextView) findViewById(R.id.name);
		textView.setText(name);

		// get the version info
		versionName = pkg.versionName;
		versionCode = pkg.versionCode;
		// display the version info
		TextView versionView = (TextView) findViewById(R.id.version);
		versionView.setText(String.format("%s (%d)", versionName, versionCode));

		// get the date last updated
		// display the last updated time
		TextView createdView = (TextView) findViewById(R.id.created);
		createdView.setText(created);

		// get the install location
		location = pkg.applicationInfo.sourceDir;
		// display the install location
		TextView locationView = (TextView) findViewById(R.id.location);
		locationView.setText(location);
		locationView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				new ShareApk().execute();
			}
			
		});

		// get the min/target sdk
		targetSdk = pkg.applicationInfo.targetSdkVersion;
		sdk = String.format("%d (target)", targetSdk);
		// display the min/target sdk
		TextView sdkView = (TextView) findViewById(R.id.sdk);
		sdkView.setText(sdk);

		new GetPackageInfo().execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.package_view, menu);
		
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_submit_package:
			// upload package report
			new SubmitPackageReport().execute();
			return true;
		case R.id.action_share_apk:
			new ShareApk().execute();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * Gets package details that take a while to get.
	 * 
	 * @author james
	 * 
	 */
	class GetPackageInfo extends AsyncTask<String, String, String> {

		/**
		 * Before starting background thread, show progress dialog.
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(context);
			pDialog.setMessage("Getting package info...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		@Override
		protected String doInBackground(String... params) {
			app = new Package(pkg);
			app.label = pkg.applicationInfo.loadLabel(pm).toString();
			if (location != null) {
				// get package checksum
				app.checksum = AndroidUtils.getChecksum(app.apkPath);
				if (checksum == null)
					checksum = AndroidUtils.getChecksum(pkg);

				// get package size
				try {
					app.size = AndroidUtils.getSize(app.apkPath);
				} catch (Exception e) {
					e.getMessage();
				}
			}

			// get permissions
			requestedPermissions = new ArrayList<PermissionInfo>();
			try {
				PackageInfo pkg = pm.getPackageInfo(name, PackageManager.GET_PERMISSIONS);
				if (pkg.requestedPermissions != null) {
					for (String pi : pkg.requestedPermissions) {
						requestedPermissions.add(pm.getPermissionInfo(pi, 0));
					}
				}

			} catch (NameNotFoundException e) {
				Log.d(this.toString(), "couldn't get package info");
			}
			// build permission list adapter
			adapter = new PermissionListAdapter((Activity) context, requestedPermissions);
			return null;
		}

		/**
		 * After completing background task dismiss the progress dialog
		 */
		@Override
		protected void onPostExecute(String result) {
			// dismiss the dialog once done
			pDialog.dismiss();

			// display checksum & size
			if (checksum != null) {
				TextView checksumView = (TextView) findViewById(R.id.checksum);
				checksumView.setText(checksum);
			}

			// display size
			TextView sizeView = (TextView) findViewById(R.id.size);
			try {
				sizeView.setText(String.format("(%.01f MB)", (float) size / (1024 * 1024)));
			} catch (Exception e) {
				sizeView.setText("(size unknown)");
			}

			adapter.pkg = pkg;
			// display requested permission list
			ListView listView = (ListView) findViewById(R.id.list);
			// Assign adapter to ListView
			listView.setAdapter(adapter);
			// setListAdapter(adapter); // if using listactivity instead
			listView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					Intent i = new Intent(context, PermissionViewActivity.class);
					i.putExtra("permissionName", requestedPermissions.get(position).name);
					i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT // bring
																		// activity
																		// to
																		// front
																		// of
																		// task
																		// stack
							| Intent.FLAG_ACTIVITY_NEW_TASK); // or start new
																// activity
					context.startActivity(i);

				}
			});

			// display total number of permission requests
			int dangerous = 0;
			for (PermissionInfo pi : requestedPermissions)
				if (pi.protectionLevel == PermissionInfo.PROTECTION_DANGEROUS)
					dangerous++;
			TextView numView = (TextView) findViewById(R.id.num_perms);
			if (dangerous == 0)
				numView.setText(String.format("%d permissions found", requestedPermissions.size()));
			else
				numView.setText(String.format("%d permissions found (%d dangerous)", requestedPermissions.size(), dangerous));

		}
	}

	class SubmitPackageReport extends AsyncTask<String, String, String> {

		/**
		 * Before starting background thread, show progress dialog.
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(context);
			pDialog.setMessage(String.format("Submitting package info..."));
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		@Override
		protected String doInBackground(String... params) {
			String result = "";
			// submit device
			Utils.submitDevice(uuid, make, model, carrier, rom, version, null);
			// submit package
			//result = Utils.submitPackage(pkg, uuid);
			JSONObject data = new JSONObject();
			data.put("name", name);
			data.put("label", label);
			data.put()
			return result;
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
	
	class GetMarketData extends AsyncTask<String, String, String> {

		/**
		 * Before starting background thread, show progress dialog.
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			json = new JSONObject();
			pDialog = new ProgressDialog(context);
			pDialog.setMessage(String.format("Getting market data..."));
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		@Override
		protected String doInBackground(String... params) {
			String result = "";
			// submit device
			// TODO: get market data
			//Utils.getMarketData(checksum);
			// submit package
			result = Utils.submitPackage(pkg, uuid);
			return result;
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
	
	/** Presents a dialog to select an app to share the APK file with. */
	class ShareApk extends AsyncTask<String, String, String> {
		ProgressDialog pDialog;
		AlertDialog aDialog;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			json = new JSONObject();
			pDialog = new ProgressDialog(context);
			pDialog.setMessage(String.format("Copying package to " + Environment.getExternalStorageDirectory().getAbsolutePath() + "backup/apk..."));
			pDialog.setIndeterminate(true);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		@Override
		protected String doInBackground(String... params) {
			if (isExternalStorageWritable()) {
				File dir = new File(Environment.getExternalStorageDirectory(), "backup/apk");
				dir.mkdirs();
				File file = new File(dir, FilenameUtils.getBaseName(location) + "-" + checksum + "." + FilenameUtils.getExtension(location));
				String source = location,
						destination = file.getAbsolutePath();
				//Log.d("copy file", "Copying file from " + location + " to " + file.getAbsolutePath());
				if (RootTools.copyFile(source, destination, true, true)) {
					pDialog.dismiss();
					Intent intent = new Intent(Intent.ACTION_SEND);
					Uri uri = Uri.parse(destination);
					String type = URLConnection.guessContentTypeFromName(uri.toString());
					intent.setType(type);
					intent.putExtra(Intent.EXTRA_STREAM, uri);
					Log.d("open apk", "Opening " + uri + " (" + type + ")");
					startActivity(Intent.createChooser(intent, "Send APK via..."));
				}
			} else {
				// TODO figure out how to send APK file from protected storage
				pDialog.hide();
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage("Unable to copy APK file because external storage is unavailable.");
				aDialog = builder.create();
			}
			return null;
		}
	}
	
	public boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}
}
