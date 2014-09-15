package com.wernicke.android.heracles;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.wernicke.android.heracles.PackageListActivity.SubmitFullReport;
import com.wernicke.android.utils.ProtectionLevel;
import com.wernicke.heracles.R;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

@SuppressWarnings("unused")
public class PermissionViewActivity extends Activity {
	public static final String url = "https://nmtsfs.org/jwernicke/heracles";

	Context context;
	PackageManager pm;
	ProgressDialog pDialog;

	PermissionInfo permission; // the permission displayed in this view
	String name; // qualified name of permission (e.g., android.permission.INTERNET)
	String label; // user displayed name of permission (e.g., full network access)
	String description; // description of permission
	String group; // qualified name of group to which permission belongs
	String groupLabel;
	int protectionLevel; // protection level of permission (e.g., PermissionInfo.PROTECTION_DANGEROUS)
	String definer; // package in which this permission is defined

	ArrayList<PackageInfo> packages; // packages requesting this permission
	PackageListAdapter adapter; // package list view adapter

	TextView labelView;
	TextView definerView;
	TextView protectionView;
	TextView descriptionView;
	TextView groupView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		pm = context.getPackageManager();

		setContentView(R.layout.permission_view_layout);
		name = getIntent().getStringExtra("permissionName");

		try {
			permission = pm.getPermissionInfo(name, PackageManager.GET_META_DATA);
			name = permission.name;
			label = permission.loadLabel(pm).toString();
			description = permission.loadDescription(pm).toString();
			group = permission.group;
			groupLabel = pm.getPermissionGroupInfo(group, PackageManager.GET_META_DATA).loadLabel(pm).toString();
			protectionLevel = permission.protectionLevel;
			definer = permission.packageName;
		} catch (NameNotFoundException e) {
			Log.d("Get Permission", name + " could not be retrieved");
		} catch (NullPointerException e) {
			Log.d("Get Permission", "null pointer exception");
		}

		// set the title bar to show packages
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
			ActionBar actionBar = getActionBar();
			actionBar.setIcon(R.drawable.ic_permission);
			actionBar.setTitle(label);
		} else {
			this.setTitle(label);
		}

		// display permission group
		groupView = (TextView) findViewById(R.id.group);
		if (groupLabel == null)
			groupView.setText(String.format("This permission does not belong to a group."));
		else
			groupView.setText(String.format("%s (%s)", groupLabel, group));

		// display permission label
		labelView = (TextView) findViewById(R.id.label);
		labelView.setText(String.format("%s (%s)", label, name));

		// display permission description
		descriptionView = (TextView) findViewById(R.id.description);
		if (description == null)
			descriptionView.setText("No description available.");
		else
			descriptionView.setText(description);

		// display protection level
		protectionView = (TextView) findViewById(R.id.protectionLevel);
		protectionView.setText(String.format("%s", ProtectionLevel.toString(protectionLevel)));

		// display package that defines this permission
		definerView = (TextView) findViewById(R.id.definer);

		// set text to red if dangerous permission
		if (permission.protectionLevel == PermissionInfo.PROTECTION_DANGEROUS) {
			this.setTitleColor(Color.RED);
			labelView.setTextColor(Color.RED);
			descriptionView.setTextColor(Color.RED);
			groupView.setTextColor(Color.RED);
			protectionView.setTextColor(Color.RED);
			definerView.setTextColor(Color.RED);
		}

		new EnumeratePackages().execute();

		try {
			definerView.setText(String.format("%s (%s)", pm.getApplicationInfo(definer, 0).loadLabel(pm), definer));
		} catch (NameNotFoundException e) {
			definerView.setText(String.format("%s", definer));
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.permission_view, menu);
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

	class EnumeratePackages extends AsyncTask<String, String, String> {

		/**
		 * Before starting background thread, show progress dialog.
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(context);
			pDialog.setMessage("Enumerating requesting packages...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			pDialog.show();
		}

		/**
		 * Get all packages that request this permission.
		 */
		@Override
		protected String doInBackground(String... params) {
			packages = new ArrayList<PackageInfo>();
			for (PackageInfo pkg : pm.getInstalledPackages(PackageManager.GET_PERMISSIONS))
				if (pkg.requestedPermissions != null)
					for (String permission : pkg.requestedPermissions)
						if (permission.equals(name))
							packages.add(pkg);
			adapter = new PackageListAdapter((Activity) context, packages);
			return null;
		}

		/**
		 * After completing background task dismiss the progress dialog
		 */
		@Override
		protected void onPostExecute(String result) {
			// dismiss the dialog once done
			pDialog.dismiss();

			// display package list
			ListView listView = (ListView) findViewById(R.id.list);
			listView.setAdapter(adapter);
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

			// display number of packages requesting this permission
			TextView numView = (TextView) findViewById(R.id.num_packages);
			numView.setText(packages.size() + " packages found");

			// update definer if necessary
			if (definer == null || definer.equals(""))
				for (PackageInfo pkg : packages)
					if (pkg.permissions != null)
						for (PermissionInfo pi : pkg.permissions)
							if (pi.name.equals(name)) {
								definer = pkg.packageName;
								try {
									definerView.setText(String.format("%s (%s)", pm.getApplicationInfo(definer, 0).loadLabel(pm), definer));
								} catch (NameNotFoundException e) {
									definerView.setText(String.format("%s", definer));
								}
								return;
							}
		}
	}
}
