package com.tapi.ippingdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class TwoTextViewAdapter extends ArrayAdapter<Map<String, String>> {
    private List<Map<String, String>> items;
    Context mContext;

    public TwoTextViewAdapter(Context context, int i, List<Map<String, String>> list) {
        super(context, i, list);
        this.mContext = context;
        this.items = list;
    }

    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(C0565R.layout.list_item_title_value, (ViewGroup) null);
        }
        Map map = this.items.get(i);
        Map.Entry entry = (Map.Entry) map.entrySet().iterator().next();
        if (map != null) {
            ((TextView) view.findViewById(C0565R.C0567id.txtTitle)).setText((CharSequence) entry.getKey());
            ((TextView) view.findViewById(C0565R.C0567id.txtValue)).setText((CharSequence) entry.getValue());
        }
        return view;
    }
}