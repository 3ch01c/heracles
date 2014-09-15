package com.wernicke.android.utils;

import android.content.pm.PermissionInfo;

public class ProtectionLevel {
	public static String toString(int val) {
		switch (val) {
		case PermissionInfo.PROTECTION_NORMAL:
			return String.format("NORMAL: 0x%02x", val);
		case PermissionInfo.PROTECTION_DANGEROUS:
			return String.format("DANGEROUS: 0x%02x", val);
		case PermissionInfo.PROTECTION_FLAG_SYSTEM:
			return String.format("SYSTEM: 0x%02x", val);
		case PermissionInfo.PROTECTION_SIGNATURE:
			return String.format("SIGNATURE: 0x%02x", val);
		case PermissionInfo.PROTECTION_FLAG_DEVELOPMENT:
			return String.format("DEVELOPMENT: 0x%02x", val);
		case PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM:
			return String.format("SIGNATURE OR SYSTEM: 0x%02x", val);
		case 0x12:
			return String.format("SIGNATURE AND SYSTEM: 0x%02x", val);
		case 0x32:
			return String.format("DEVELOPMENT, SIGNATURE AND SYSTEM: 0x%02x", val);
		default:
			return String.format("Unknown value: %d", val);
		}
	}
}
