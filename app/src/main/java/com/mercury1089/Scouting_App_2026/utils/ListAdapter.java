package com.mercury1089.Scouting_App_2026.utils;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;

import com.mercury1089.Scouting_App_2026.R;
import com.mercury1089.Scouting_App_2026.SettingsActivity;
import com.mercury1089.Scouting_App_2026.qr.QRRunnable;

import static com.mercury1089.Scouting_App_2026.utils.GenUtils.padLeftZeros;

public class ListAdapter extends BaseAdapter {
    SettingsActivity context;
    String[] data;
    private static LayoutInflater inflater = null;
    private Dialog loading_alert;
    public final static int QRCodeSize = 500;

    public ListAdapter(Context context, String[] data) {
        this.context = (SettingsActivity) context;
        this.data = data;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.length;
    }

    @Override
    public Object getItem(int position) {
        return data[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (vi == null) {
            vi = inflater.inflate(R.layout.screen_settings_qr_list_item, parent, false);
        }

        Button item = vi.findViewById(R.id.itemButton);
        final String qrString = data[position];

        try {
            // Parse based on the first line of the record (which is the Setup line)
            String firstLine = qrString.split("\n")[0];
            String[] qrData = firstLine.split(",");
            
            // Expected length is 8 after our previous fix: Scouter,Team,Match,P1,P2,Color,Preload,NoShow
            if (qrData.length >= 3) {
                String teamNumber = qrData[1];
                String matchNumber = qrData[2];

                item.setText(context.getString(R.string.QRCacheItem, padLeftZeros(teamNumber, 4), padLeftZeros(matchNumber, 2)));
                
                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loading_alert = new Dialog(context);
                        loading_alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        loading_alert.setContentView(R.layout.screen_qr_loading);
                        loading_alert.setCancelable(false);
                        loading_alert.show();

                        QRRunnable runnable = new QRRunnable(qrString, context, loading_alert);
                        new Thread(runnable).start();
                    }
                });
            } else {
                item.setText("Invalid QR Data");
                item.setEnabled(false);
            }
        } catch (Exception e) {
            Log.e("ListAdapter", "Error parsing QR data at position " + position, e);
            item.setText("Parse Error");
            item.setEnabled(false);
        }
        
        return vi;
    }
}
