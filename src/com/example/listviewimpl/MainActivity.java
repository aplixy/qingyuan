package com.example.listviewimpl;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.listviewimpl.view.MyListView;

public class MainActivity extends Activity {

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        setContentView(R.layout.activity_main1);
        
        MyListView listView = (MyListView) findViewById(R.id.my_listview);
//        CopyOfMyListView listView = (CopyOfMyListView) findViewById(R.id.my_listview);
        final CustomAdapter customAdapter = new CustomAdapter(this, createTestData());
        listView.setAdapter(customAdapter);
        
        // item Click
        listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String itemName = (String) customAdapter.getItem(position);
				Toast.makeText(getBaseContext(), "点击   " + itemName, 
						Toast.LENGTH_SHORT).show();
			}
		});
        
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				String itemName = (String) customAdapter.getItem(position);
				Toast.makeText(getBaseContext(), "长按  " + itemName, 
						Toast.LENGTH_SHORT).show();
				return true;
			}
		});
    }
	
	// ===========================================================
	// Private Methods
	// ===========================================================

	/**
	 * ListView数据创建
	 */
	private List<String> createTestData() {
		List<String> data = new ArrayList<String>();
		for (int i = 0; i < 50; i++) {
			data.add("Love World " + i);
		}
		return data;
	}
    
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================


	private class CustomAdapter extends BaseAdapter {

		private List<String> mData;
		private LayoutInflater mInflater;
		
		public CustomAdapter(Context context, List<String> data) {
			mData = data;
			mInflater = LayoutInflater.from(context);
		}
		
		@Override
		public int getCount() {
			if (mData == null || mData.size() <= 0) {
				return 0;
			}
			return mData.size();
		}

		@Override
		public Object getItem(int position) {
			if (mData == null || mData.size() <= 0
					|| position < 0 || position >= mData.size()) {
				return null;
			}
			return mData.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.list_item, null);
			}
			
			TextView name = (TextView) convertView.findViewById(R.id.tv_name);
			name.setText( (CharSequence) getItem(position) );
			
			return convertView;
		}
		
	}
	

}