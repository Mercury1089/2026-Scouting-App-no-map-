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
        // TODO Auto-generated constructor stub
        this.context = (SettingsActivity) context;
        this.data = data;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return data.length;
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return data[position];
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View vi = inflater.inflate(R.layout.screen_settings_qr_list_item, null);

        Button item = vi.findViewById(R.id.itemButton);

        String[] qrData = data[position].split(",");
        Log.d("Stuff", data[position]);
        String scouterName = qrData[0], teamNumber = qrData[1], matchNumber = qrData[2], qrString = data[position];

        item.setText(context.getString(R.string.QRCacheItem, padLeftZeros(teamNumber, 4), padLeftZeros(matchNumber, 2)));
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loading_alert = new Dialog(context);
                loading_alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
                loading_alert.setContentView(R.layout.screen_qr_loading);
                loading_alert.setCancelable(false);
                loading_alert.show();

                QRRunnable runnable = new QRRunnable(data[position], context, loading_alert);
                new Thread(runnable).start();
            }
        });
        item.setTag(scouterName + "~" + teamNumber + "~" + matchNumber + "~" + qrString);
        item.setId(Integer.parseInt(teamNumber+matchNumber));
        return vi;
    }

//    class QRRunnable implements Runnable {
//
//        private String scouter, teamNum, matchNum, qrString;
//        private SettingsActivity context;
//
//        public QRRunnable(String[] qrData, Context c, View v){
//            scouter = qrData[0];
//            teamNum = qrData[1];
//            matchNum = qrData[2];
//            qrString = qrData[3];
//            context = (SettingsActivity) c;
//        }
//
//        @Override
//        public void run() {
//            try {
//                Bitmap bitmap = TextToImageEncode(qrString);
//                context.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Dialog dialog = new Dialog(context);
//                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//                        dialog.setContentView(R.layout.popup_qr_cached);
//
//                        ImageView imageView = dialog.findViewById(R.id.imageView);
//                        TextView scouterName = dialog.findViewById(R.id.ScouterNameQR);
//                        TextView teamNumber = dialog.findViewById(R.id.TeamNumberQR);
//                        TextView matchNumber = dialog.findViewById(R.id.MatchNumberQR);
//                        Button closeButton = dialog.findViewById(R.id.CloseButton);
//                        imageView.setImageBitmap(bitmap);
//
//                        dialog.setCancelable(false);
//
//                        scouterName.setText(scouter);
//                        teamNumber.setText(GenUtils.padLeftZeros(teamNum, 2));
//                        matchNumber.setText(GenUtils.padLeftZeros(matchNum, 2));
//
//                        loading_alert.dismiss();
//
//                        dialog.show();
//
//                        closeButton.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                dialog.dismiss();
//                            }
//                        });
//                    }
//                });
//            } catch (WriterException e){}
//        }
//    }
}
