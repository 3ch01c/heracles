package com.wernicke.android.heracles;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wernicke.android.utils.PackageComparator;
import com.wernicke.heracles.R;

/**
 * ListView adapter to display package names, labels, and icons. Performance optimized adapter implementation which reuses existing views and implements the
 * holder pattern as discussed in http://www.vogella.com/articles/AndroidListView/article.html
 * 
 * @author james
 * 
 */
public class PackageListAdapter extends ArrayAdapter<PackageInfo> {
	private final Activity context;
	private final ArrayList<PackageInfo> packages;

	static class ViewHolder {
		public TextView labelView;
		public TextView nameView;
		public ImageView iconView;
	}

	public PackageListAdapter(Activity context, ArrayList<PackageInfo> packages) {
		super(context, R.layout.package_list_row_layout, packages);
		this.context = context;
		PackageComparator pc = new PackageComparator();
		Collections.sort(packages, pc);
		this.packages = packages;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// get existing view if possible
		View rowView = convertView;
		if (rowView == null) {
			// inflate permission_list_row_layout.xml
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(R.layout.package_list_row_layout, null);

			// create new view holder
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.labelView = (TextView) rowView.findViewById(R.id.package_label);
			viewHolder.nameView = (TextView) rowView.findViewById(R.id.package_name);
			viewHolder.iconView = (ImageView) rowView.findViewById(R.id.package_icon);

			// set row view tag to holder
			rowView.setTag(viewHolder);
		}

		// get existing view for holder
		ViewHolder holder = (ViewHolder) rowView.getTag();

		// set package label
		try {
			holder.labelView.setText(PackageListActivity.pm.getApplicationLabel(packages.get(position).applicationInfo));
		} catch (Exception e) {
			holder.labelView.setText(packages.get(position).packageName);
		}

		// set package name
		holder.nameView.setText(packages.get(position).packageName);

		// set package icon
		try {
			holder.iconView.setImageDrawable(packages.get(position).applicationInfo.loadIcon(PackageListActivity.pm));
		} catch (Exception e) {
			Log.d(this.toString(), "cannot get icon");
		}
		return rowView;
	}

	class PackageOnClickListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {

		}

	}

}