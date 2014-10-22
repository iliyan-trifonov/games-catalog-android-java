package com.like2program.ps3igri;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class GamesListAdapter extends ArrayAdapter<GamesListItem> {

    private ArrayList<GamesListItem> games;
    private File cacheDir;
    private GamesUtils gamesUtils;

    public GamesListAdapter(Context context, int textViewResourceId, ArrayList<GamesListItem> gms, File cacheDir) {
        super(context, textViewResourceId, gms);
        games = gms;
        this.cacheDir = cacheDir;
        this.gamesUtils = new GamesUtils(this.getContext());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.row, null);
        }
        //v.setBackgroundColor(Color.WHITE);//skin test
        //v.setBackgroundResource(R.drawable.listview_row_gradient);
        GamesListItem item = games.get(position);
        if (item != null) {
            //fav icon
            ImageView favsIcon = null;
            if (v != null) {
                favsIcon = (ImageView) v.findViewById(R.id.favsIconInList);
            }
            if (favsIcon != null) {
                if (item.getFav() == 1) {
                    favsIcon.setVisibility(View.VISIBLE);
                } else {
                    favsIcon.setVisibility(View.GONE);
                }
            }
            TextView text = null;
            if (v != null) {
                text = (TextView) v.findViewById(R.id.textView1);
            }
            if (text != null) {
                text.setText(item.getTitle());
                //cat & price
                TextView textOther = (TextView) v.findViewById(R.id.otherText);
                textOther.setTextColor(Color.GRAY);
                //textOther.setHighlightColor(Color.BLUE);
                if (!item.getCat().equals(""))
                    textOther.setText(this.gamesUtils.getGameCatString(item.getCat()));
                else
                    textOther.setText("");
                if (item.getPrice() != 0) {
                    textOther.setText(textOther.getText()
                            + (!item.getCat().equals("") ? ", " : "") + item.getPrice() + " лв.");
                }
                //published
                TextView textPublished = (TextView) v.findViewById(R.id.publishedText);
                //textPublished.setText("От: " + item.getPubDate());
                textPublished.setVisibility(View.GONE);
                //seen
                TextView textSeen = (TextView) v.findViewById(R.id.dateSeenTextList);
                textSeen.setTextColor(Color.GRAY);
                textSeen.setText("От: " + this.gamesUtils.formatDate(item.getSeen()));
                //textSeen.setHighlightColor(Color.BLUE);
                TextView authorInfoText = (TextView) v.findViewById(R.id.authorInfoText);
                authorInfoText.setText(item.getAuthorName());
                if (!item.getAuthorCity().equals("")) {
                    authorInfoText.setText(authorInfoText.getText() + " от " + item.getAuthorCity());
                }
                authorInfoText.setTextColor(Color.GRAY);
                //colors
                if (item.getStatus().equals("new")) {
                    text.setTextColor(Color.RED);//new
                } else if (item.getStatus().equals("changed")) {
                    text.setTextColor(Color.BLUE);//changed
                } else {
                    text.setTextColor(Color.BLACK);//not changed
                }
                //text.setHighlightColor(Color.BLUE);
            }
            ImageView iv = null;
            if (v != null) {
                iv = (ImageView) v.findViewById(R.id.imageView1);
            }
            boolean bitmapRes;
            if (iv != null) {
                bitmapRes = this.gamesUtils.setImageViewBitmap(iv, this.gamesUtils.generateThumbUrl(item.getGameId()), cacheDir);
                ProgressBar progress = (ProgressBar) v.findViewById(R.id.progressBar1);
                if (progress != null) {
                    progress.setVisibility((bitmapRes) ? View.GONE : View.VISIBLE);
                    ChangedStatusGlobal instance = ChangedStatusGlobal.getInstance();
                    //Log.i("GamesListAdapter", "getView(): instance.getAdapterNotified() = " + instance.getAdapterNotified());
                    if (!bitmapRes && instance.getAdapterNotified()) {
                        //Log.i("GamesListAdapter", "getView(): loading noimage drawable");
                        progress.setVisibility(View.GONE);
                        iv.setImageResource(R.drawable.noimage);
                        bitmapRes = true;
                    }
                }
                iv.setVisibility((bitmapRes) ? View.VISIBLE : View.GONE);
                //skin test
                iv.setAdjustViewBounds(false);
                iv.setScaleType(ScaleType.FIT_XY);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(100, 100);
                iv.setLayoutParams(layoutParams);
                //
            }
        }
        return v;
    }

}
