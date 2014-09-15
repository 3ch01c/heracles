package com.wernicke.android.utils;

import java.util.Comparator;

import android.content.pm.PermissionInfo;

public class PermissionComparator implements Comparator<PermissionInfo> {
	public int compare(PermissionInfo p1, PermissionInfo p2) {
		return p1.name.compareTo(p2.name);
	}
}
