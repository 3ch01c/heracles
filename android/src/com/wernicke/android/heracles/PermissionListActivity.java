package com.wernicke.android.heracles;

import java.util.ArrayList;

import com.wernicke.heracles.R;
import com.wernicke.utils.JSONParser;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class PermissionListActivity extends Activity {

	public static final String TAG_SUCCESS = "success";
	public static final String base_url = "http://192.168.0.11/~james/heracles";
	ProgressDialog pDialog;
	AlertDialog aDialog;

	public Context context;
	public static PackageManager pm;
	public ArrayList<PackageInfo> packages; // list of installed packages

	ArrayList<PermissionInfo> permissions; // list of permissions defined on device
	PermissionListAdapter adapter; // permission list view adapter

	JSONParser jsonParser = new JSONParser();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.permission_list_layout);

		// set the title bar to show packages
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
			ActionBar actionBar = getActionBar();
			actionBar.setIcon(R.drawable.ic_permission);
			actionBar.setTitle("Permissions");
		} else {
			this.setTitle("Permissions");
		}

		// global state vars
		context = this;
		pm = context.getPackageManager();

		// gets the list of packages and creates the package list adapter
		new EnumeratePermissions().execute();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.permission_list_view, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case R.id.action_view_packages:
			i = new Intent(context, PackageListActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT // bring activity to front of task stack
					| Intent.FLAG_ACTIVITY_NEW_TASK); // or start new activity
			context.startActivity(i);
			return true;
		case R.id.action_view_device:
			i = new Intent(context, DeviceInfoActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT // bring activity to front of task stack
					| Intent.FLAG_ACTIVITY_NEW_TASK); // or start new activity
			context.startActivity(i);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Background AsyncTask to enumerate package infos.
	 */
	class EnumeratePermissions extends AsyncTask<String, String, String> {

		/**
		 * Before starting background thread, show progress dialog.
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(context);
			pDialog.setMessage("Enumerating permissions...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		/**
		 * Get all permission requests on the device.
		 */
		@Override
		protected String doInBackground(String... params) {
			permissions = new ArrayList<PermissionInfo>();
			packages = (ArrayList<PackageInfo>) pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
			ArrayList<String> packageNames = new ArrayList<String>();

			for (PackageInfo pkg : packages)
				if (pkg.requestedPermissions != null)
					for (String name : pkg.requestedPermissions)
						try {
							if (!packageNames.contains(name)) {
								PermissionInfo permission = pm.getPermissionInfo(name, PackageManager.GET_META_DATA);
								permissions.add(permission);
								packageNames.add(name);
							}
						} catch (NameNotFoundException e) {
							Log.d(this.toString(), "unable to get " + name);
						}

			// add requests to list adapter
			adapter = new PermissionListAdapter((Activity) context, permissions);

			return null;
		}

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
					Intent i = new Intent(context, PermissionViewActivity.class);
					i.putExtra("permissionName", permissions.get(position).name);
					i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT // bring activity to front of task stack
							| Intent.FLAG_ACTIVITY_NEW_TASK); // or start new activity
					context.startActivity(i);

				}
			});

			// display total number of permission requests
			int dangerous = 0;
			for (PermissionInfo pi : permissions)
				if (pi.protectionLevel == PermissionInfo.PROTECTION_DANGEROUS)
					dangerous++;
			TextView numView = (TextView) findViewById(R.id.num_perms);
			if (dangerous == 0)
				numView.setText(String.format("%d permissions found", permissions.size()));
			else
				numView.setText(String.format("%d permissions found (%d dangerous)", permissions.size(), dangerous));
		}

	}
}
