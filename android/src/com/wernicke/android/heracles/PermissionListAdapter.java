package com.wernicke.android.heracles;

import java.util.ArrayList;
import java.util.Collections;

import com.wernicke.android.utils.PermissionComparator;
import com.wernicke.heracles.R;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PermissionInfo;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * ListView adapter to display permission names, labels, and icons. Performance optimized adapter implementation which reuses existing views and implements the
 * holder pattern as discussed in http://www.vogella.com/articles/AndroidListView/article.html
 * 
 * @author james
 * 
 */
public class PermissionListAdapter extends ArrayAdapter<PermissionInfo> {
	private final Activity context;
	private final ArrayList<PermissionInfo> permissions;
	public PackageInfo pkg;

	static class ViewHolder {
		public TextView labelView;
		public TextView nameView;
		public CheckBox checkView;
	}

	public PermissionListAdapter(Activity context, ArrayList<PermissionInfo> permissions) {
		super(context, R.layout.permission_list_row_layout, permissions);
		this.context = context;
		PermissionComparator pc = new PermissionComparator();
		Collections.sort(permissions, pc);
		this.permissions = permissions;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// get existing view if possible
		View rowView = convertView;
		if (rowView == null) {
			// inflate permission_list_row_layout.xml
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(R.layout.permission_list_row_layout, null);

			// create new view holder
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.labelView = (TextView) rowView.findViewById(R.id.permission_label);
			viewHolder.nameView = (TextView) rowView.findViewById(R.id.permission_name);

			// set row view tag to holder
			rowView.setTag(viewHolder);
		}

		// get existing view for holder
		ViewHolder holder = (ViewHolder) rowView.getTag();

		// set permission label
		holder.labelView.setText(permissions.get(position).loadLabel(context.getPackageManager()).toString());

		// set permission name
		holder.nameView.setText(permissions.get(position).name);

		// color red if dangerous
		if (permissions.get(position).protectionLevel == PermissionInfo.PROTECTION_DANGEROUS) {
			holder.labelView.setTextColor(Color.RED);
			holder.nameView.setTextColor(Color.RED);
		} else {
			holder.labelView.setTextColor(Color.WHITE);
			holder.nameView.setTextColor(Color.WHITE);
		}

		return rowView;
	}

	class PermissionOnClickListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {

		}

	}

}