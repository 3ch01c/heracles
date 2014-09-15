package com.wernicke.android.utils;

import java.util.Comparator;

import com.wernicke.android.heracles.PackageListActivity;

import android.content.pm.PackageInfo;

public class PackageComparator implements Comparator<PackageInfo> {
	public int compare(PackageInfo p1, PackageInfo p2) {
		return p1.applicationInfo.loadLabel(PackageListActivity.context.getPackageManager()).toString()
				.compareTo(p2.applicationInfo.loadLabel(PackageListActivity.context.getPackageManager()).toString());
	}
}
