package com.wernicke.android.heracles;

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

/**
 * A bunch of useful methods.
 * 
 * @author james
 * 
 */

public class Utils {
	public static final String TAG_SUCCESS = "success", TAG_MESSAGE = "message", TAG_QUERY = "query";
	public static final String METHOD = "POST";
	// public static final String URL = "http://192.168.0.43/~james/heracles";
	public static final String URL = "http://129.138.132.29/~james/heracles";
	//public static final String URL = "https://nmtsfs.org/jwernicke/heracles";
	public static final int TIMEOUT = 100000;
	static PackageManager pm;

	public static final String[] permissionFlags = { "", "costsMoney" };

	/**
	 * Check if a string is numeric.
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isNumeric(String s) {
		try {
			// try to parse string as numeric
			Double.parseDouble(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Check if a string represents a hex value
	 * 
	 * @param in
	 * @return
	 */
	public static boolean isHex(String in) {
		final String HEX_CHARS = "0123456789abcdefABCDEF";
		for (char c : in.toCharArray())
			if (HEX_CHARS.indexOf(c) < 0)
				return false;
		return true;
	}

	/**
	 * Convert byte array to hex string.
	 * 
	 * @param bytes
	 * @return
	 */
	public static String toHex(byte[] bytes) {
		BigInteger bi = new BigInteger(1, bytes);
		return String.format("%0" + (bytes.length << 1) + "x", bi);
	}

	/**
	 * Checks is a string is a MD5 hash.
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isHash(String s) {
		if (isHex(s) && s.length() == 32)
			return true;
		return false;
	}

	/**
	 * Gets checksum of package.
	 * 
	 * @param pkg
	 * @return
	 */
	public static String getChecksum(String path) {
		// if the device has su & busybox, get package checksum, else do
		// pseudo-checksum
		try {
			RootTools.debugMode = true; // turn on root tools debug mode

			// build command
			ResultCommand command = new ResultCommand(0, TIMEOUT, "md5sum " + path);
			runCommand(command);
			String checksum = command.getResult().split(" ")[0];
			if (isHash(checksum))
				return checksum;
		} catch (Exception e) {
		}
		return null;
	}

	public static String getChecksum(PackageInfo pkg) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest;
			digest = md.digest((pkg.packageName + pkg.versionCode).getBytes("UTF-8"));
			return toHex(digest);
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
	 */
	public static int getSize(String path) {
		int size = 0;

		// if the device has su & busybox, get size, else just return 0
		if (RootTools.isAccessGiven() && RootTools.isBusyboxAvailable()) {
			RootTools.debugMode = true; // turn on root tools debug mode

			// build command
			ResultCommand command = new ResultCommand(0, 10000, "stat -t " + path);
			runCommand(command);
			try {
				size = Integer.parseInt(command.getResult().split(" ")[1]);
			} catch (Exception e) {

			}
		}
		return size;
	}

	/**
	 * Get device manufacturer/model. Basically concatenates {@link Build.MANUFACTURER} and {@link Build.MODEL}, but some model names include manufacturer
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
	 * Runs a command.
	 * 
	 * @param command
	 * @return
	 */
	public static void runCommand(Command command) {
		// RootTools.debugMode = true; // turn on root tools debug mode
		try {
			RootTools.getShell(true).add(command).waitForFinish();
		} catch (InterruptedException e) {
			e.getMessage();
		} catch (IOException e) {
			e.getMessage();
		} catch (TimeoutException e) {
			e.getMessage();
		} catch (RootDeniedException e) {
			e.getMessage();
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
		int size = Utils.getSize(pkg.applicationInfo.sourceDir);
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

	/**
	 * Submits package info to database.
	 * 
	 * @param URL
	 * @param pkg
	 * @param pm
	 * @return response message
	 */
	public static String submitPackage(PackageInfo pkg, String uuid) {
		String result = null;
		pm = PackageListActivity.pm;

		try {
			List<NameValuePair> params = buildPackageParams(pkg);
			String checksum = params.get(0).getValue();

			// create package
			JSONParser jsonParser = new JSONParser();
			JSONObject json = jsonParser.makeHttpRequest(URL + "/packages/create.php", METHOD, params);
			Log.d(TAG_QUERY, json.getString(TAG_MESSAGE));
			if (json.getInt(TAG_SUCCESS) == 1)
				result = "Package created.";

			for (String name : pkg.requestedPermissions) {
				// create permission
				PermissionInfo pi = pm.getPermissionInfo(name, PackageManager.GET_META_DATA);
				params = buildPermissionParams(pi);
				jsonParser = new JSONParser();
				json = jsonParser.makeHttpRequest(URL + "/permissions/create.php", METHOD, params);
				Log.d(TAG_QUERY, json.getString(TAG_MESSAGE));

				// add permission request for package
				params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("package", checksum));
				params.add(new BasicNameValuePair("permission", name));
				jsonParser = new JSONParser();
				json = jsonParser.makeHttpRequest(URL + "/requests/create.php", METHOD, params);
				Log.d(TAG_QUERY, json.getString(TAG_MESSAGE));

				// add request to device profile
				params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("uuid", uuid));
				params.add(new BasicNameValuePair("package", checksum));
				params.add(new BasicNameValuePair("permission", pi.name));
				// TODO: make active based on whether user grants the permission
				params.add(new BasicNameValuePair("active", "1"));
				jsonParser = new JSONParser();
				json = jsonParser.makeHttpRequest(URL + "/device_profiles/create.php", METHOD, params);
				Log.d(TAG_QUERY, json.getString(TAG_MESSAGE));

				// TODO: add request to user profile
			}
			result = json.getString(TAG_MESSAGE);
		} catch (Exception e) {
			// some other error occurred (probably could not connect server)
			result = "Could not connect to server.";
		}

		// return response
		return result;
	}

	// TODO allow updating profiles

	// TODO report by submitting a single file
	public File buildReportFile(String uuid, ArrayList<PackageInfo> packages) {
		try {
			String filename = String.format("%s-%s.zip", uuid, new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()).toString());
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(filename));

			String make = Build.MANUFACTURER;
			String model = Build.DISPLAY;
			String carrier = Build.BRAND;
			String rom = Build.DISPLAY;
			String version = Build.VERSION.RELEASE;
			
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			//params.add(new BasicNameValuePair("device",buildDeviceParams(uuid, make, model, carrier, rom, version, null)));
			

			for (PackageInfo pkg : packages) {

			}
			out.close();
		} catch (FileNotFoundException e) {
			Log.e("buildReportFile", "file not found");
		} catch (IOException e) {
			Log.e("buildReportFile", "io exception");
		}

		return null;

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
			json.put("size", Utils.getSize(pkg.applicationInfo.sourceDir));
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

	public JSONObject getMarketData(String checksum) {
		JSONObject json;
		JSONParser jsonParser = new JSONParser();
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("package",checksum));
		json = jsonParser.makeHttpRequest(URL + "/markets/stats.php", METHOD, params);
		
		return json;
	}
	
}
