package cn.xxx.xx.bug;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;


/**
 * Created by qs on 2016/10/18.
 */

public class MyBluetoothAdapter extends BaseAdapter {

    private List<String> mList;
    private Context mContext;
    private LayoutInflater minflater;

    public MyBluetoothAdapter(Context context, List<String> list) {
        mContext = context;
        mList = list;
        minflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mList == null ? 0 : mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView name = null;
        if (convertView == null) {
            convertView = minflater.inflate(R.layout.layout_devicename_item, parent, false);
            name = (TextView) convertView.findViewById(R.id.tv_device_name);
            convertView.setTag(name);
        } else {
            name = (TextView) convertView.getTag();
        }
        name.setText(mList.get(position));
        return convertView;
    }
}
