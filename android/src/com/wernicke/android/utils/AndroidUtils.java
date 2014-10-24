package com.wernicke.android.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipOutputStream;

import com.wernicke.android.heracles.PackageListActivity;
import com.wernicke.android.heracles.StringUtils;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.util.Log;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.wernicke.android.utils.RootTools.ResultCommand;
import com.wernicke.utils.JSONParser;
import com.wernicke.utils.Validator;

/**
 * A bunch of useful methods.
 * 
 * @author james
 * 
 */

public class AndroidUtils {
	public static final boolean DEBUG = false;
	public static final String TAG_SUCCESS = "success", TAG_MESSAGE = "message", TAG_QUERY = "query";
	public static final String METHOD = "POST";
	public static final String URL = "http://wernicke.strangled.net:5984";
	public static final int TIMEOUT = 100000;
	static PackageManager pm;

	public static final String[] permissionFlags = { "", "costsMoney" };

	/**
	 * Generates MD5 hash of file at path and returns the first 8 characters of the Base-64 encoded hash.
	 * 
	 * @param path
	 * @return
	 * @throws DecoderException 
	 */
	public static String getChecksum(String path) throws DecoderException {
		// if the device has su & busybox, get package checksum, else do
		RootTools.debugMode = DEBUG; // turn on root tools debug mode

		// build command
		ResultCommand command = new ResultCommand(0, TIMEOUT, "md5sum " + path);
		// execute command
		String checksum = command.getResult().split(" ")[0];
		if (Validator.isMD5(checksum)) {
			return Base64.encode(Hex.decodeHex(checksum.toCharArray())).substring(0,8);
		}
		return null;
	}

	/**
	 * Generates MD5 hash of package name and version code and returns the first 8 characters of the Base-64 encoded hash.
	 * Used when a file for the package can't be found.
	 * @param path
	 * @return
	 * @throws DecoderException 
	 */
	public static String getChecksum(PackageInfo pkg) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest;
			digest = md.digest((pkg.packageName + pkg.versionCode).getBytes("UTF-8"));
			return Base64.encode(digest).substring(0,8);
		} catch (NoSuchAlgorithmException e) {
			Log.d("digest", "could not find MD5 algorithm");
		} catch (UnsupportedEncodingException e) {
			Log.d("digest", "unsupported encoding");
		}
		return null;
	}

	/**
	 * Get size in bytes for path.
	 * 
	 * @param pkg
	 * @return
	 * @throws RootDeniedException 
	 * @throws TimeoutException 
	 * @throws IOException 
	 */
	public static int getSize(String path) throws IOException, TimeoutException, RootDeniedException {
		int size = 0;

		// check the device has su and busybox
		if (RootTools.isAccessGiven() && RootTools.isBusyboxAvailable()) {
			RootTools.debugMode = DEBUG; // turn on root tools debug mode

			// build command
			ResultCommand cmd = new ResultCommand(0, 10000, "stat -t " + path);
			while (!RootTools.getShell(true).add(cmd).isFinished()) {
				synchronized(cmd) {
					try {
						if (!cmd.isFinished()) {
							cmd.wait(2000);
						}
					} catch (InterruptedException e) {
						e.getMessage();
					}
				}
			}
			size = Integer.parseInt(cmd.getResult().split(" ")[1]);
		}
		return size;
	}

	/**
	 * Get device manufacturer/model. Basically concatenates {@link Build.MANUFACTURER} and
	 * {@link Build.MODEL}, but some model names include manufacturer
	 * already so this checks for that and eliminates the duplicate manufacturer name.
	 */
	public static String toDeviceName(String make, String model) {
		if (model.startsWith(make)) {
			return StringUtils.capitalize(model);
		} else {
			return StringUtils.capitalize(make) + " " + model;
		}
	}

	/**
	 * Builds post params for querying packages table.
	 * 
	 * @param pkg
	 * @return
	 */
	public static List<NameValuePair> buildPackageParams(PackageInfo pkg) {
		pm = PackageListActivity.pm;
		String packageName = pkg.packageName;
		String packageLabel;
		try {
			packageLabel = pm.getApplicationLabel(pkg.applicationInfo).toString();
		} catch (Exception e) {
			packageLabel = packageName;
		}
		String checksum = getChecksum(pkg.applicationInfo.sourceDir);
		if (checksum == null)
			checksum = getChecksum(pkg);
		String versionLabel = pkg.versionName;
		int versionCode = pkg.versionCode;
		int size = 0;
		try {
			size = Utils.getSize(pkg.applicationInfo.sourceDir);
		} catch (Exception e) {
			e.getMessage();
		}
		int targetSdk = pkg.applicationInfo.targetSdkVersion;
		// long lastUpdate = pkg.lastUpdateTime;
		// String date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date(lastUpdate));

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("checksum", checksum));
		params.add(new BasicNameValuePair("name", packageName));
		params.add(new BasicNameValuePair("label", packageLabel));
		params.add(new BasicNameValuePair("versionCode", String.valueOf(versionCode)));
		params.add(new BasicNameValuePair("versionLabel", versionLabel));
		params.add(new BasicNameValuePair("targetSdk", String.valueOf(targetSdk)));
		params.add(new BasicNameValuePair("size", String.valueOf(size)));

		return params;
	}

	/**
	 * Builds post params for querying permissions table.
	 * 
	 * @param permission
	 * @return
	 */
	public static List<NameValuePair> buildPermissionParams(PermissionInfo permission) {
		pm = PackageListActivity.pm;

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("name", permission.name));
		params.add(new BasicNameValuePair("label", (String) permission.loadLabel(pm)));
		params.add(new BasicNameValuePair("protectionLevel", String.valueOf(permission.protectionLevel)));
		params.add(new BasicNameValuePair("group", permission.group));
		return params;
	}

	public static List<NameValuePair> buildDeviceParams(String uuid, String make, String model, String carrier, String rom, String version, String user) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("uuid", uuid));
		params.add(new BasicNameValuePair("make", make));
		params.add(new BasicNameValuePair("model", model));
		params.add(new BasicNameValuePair("carrier", carrier));
		params.add(new BasicNameValuePair("rom", rom));
		params.add(new BasicNameValuePair("version", version));
		if (user != null)
			params.add(new BasicNameValuePair("user", user));
		return params;
	}

	/**
	 * Submits device info to database.
	 */
	public static String submitDevice(String uuid, String make, String model, String carrier, String rom, String version, String user) {
		String result = null;

		// build device params
		List<NameValuePair> params = buildDeviceParams(uuid, make, model, carrier, rom, version, user);

		// send device infos
		JSONParser jsonParser = new JSONParser();
		JSONObject json;
		try {
			json = jsonParser.makeHttpRequest(URL + "/devices/create.php", METHOD, params);
			try {
				result = json.getString(TAG_MESSAGE);
			} catch (JSONException e) {
				result = "No response.";
			}
		} catch (Exception e) {
			result = "Could not connect to server!";
		}
		Log.d(TAG_QUERY, result);
		return result;
	}

	public JSONObject buildJSON(String uuid) {
		JSONObject device = new JSONObject();
		try {
			device.put("uuid", uuid);
			device.put("make", Build.MANUFACTURER);
			device.put("model", Build.DISPLAY);
			device.put("carrier", Build.BRAND);
			device.put("rom", Build.DISPLAY);
			device.put("version", Build.VERSION.RELEASE);
			Log.d("device json",device.toString());
			return device;
		} catch (JSONException e) {
			Log.e("device json", e.getMessage());
		}
		return null;
		
	}
	
	public JSONObject buildJSON(PackageInfo pkg) {
		pm = PackageListActivity.pm;
		JSONObject json = new JSONObject();
		try {
			// add package attributes
			json.put("checksum", Utils.getChecksum(pkg));
			int size = 0;
			try {
				size = Utils.getSize(pkg.applicationInfo.sourceDir);
			} catch (Exception e) {
				e.getMessage();
			}
			json.put("size", size);
			json.put("name", pkg.packageName);
			json.put("label", pkg.applicationInfo.loadLabel(pm));
			json.put("versionCode", pkg.versionCode);
			json.put("versionLabel", pkg.versionName);
			json.put("targetSdk", pkg.applicationInfo.targetSdkVersion);
			for (String name : pkg.requestedPermissions)
				json.accumulate("requests", name);
			for (PermissionInfo permission : pkg.permissions)
				json.accumulate("permissions", permission.name);
			return json;
		} catch (JSONException e) {
			Log.e("package json", e.getMessage());
		}
		return null;
		
	}
	
	public JSONObject buildJSON(PermissionInfo permission) {
		pm = PackageListActivity.pm;
		JSONObject perm = new JSONObject();
		try {
			perm.put("name", permission.name);
			perm.put("label", permission.loadLabel(pm));
			perm.put("protectionLevel", permission.protectionLevel);
			perm.put("group", permission.group);
			return perm;
		} catch (JSONException e) {
			Log.e("permission json", e.getMessage());
		}
		return null;
	}
	
	public JSONObject buildJSON(String uuid, ArrayList<PackageInfo> packages) {
		JSONObject json = new JSONObject();
		try {
			json.put("device", buildJSON(uuid));
			for (PackageInfo pkg: packages) {
				json.accumulate("packages",buildJSON(pkg));
				// add permissions
				for (String name : pkg.requestedPermissions) {
					PermissionInfo permission = pm.getPermissionInfo(name, PackageManager.GET_META_DATA);
					JSONObject perm =  buildJSON(permission);
					int i = 0;
					// append permission to list if not already in list
					while (i < json.getJSONArray("permissions").length() && json.getJSONArray("permissions").getJSONObject(i).getString("name") != perm.getString("name"))
						i++;
					if (i == json.getJSONArray("permissions").length())
						json.accumulate("permissions",perm);
				}
			}
			Log.d("full report", json.toString());
		} catch (JSONException e) {
			Log.e("full report json", e.getMessage());
		} catch (NameNotFoundException e) {
			Log.e("full report json", e.getMessage());
		}
		return null;
		
	}
}

