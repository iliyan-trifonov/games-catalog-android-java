package com.like2program.ps3igri;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.AnimationSet;
import android.view.animation.BounceInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class GameImagePreview extends Activity {

    private int gameID = 0;
    private String gameUrl = "";
    private File cacheDir = null;
    private ProgressDialog progressDialog = null;
    private Bitmap imgBitmap = null;
    private ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.image_preview);
        cacheDir = new GamesUtils(getApplicationContext()).findCacheDir(GameImagePreview.this);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            gameID = bundle.getInt("gameID");
        }
        if (bundle != null) {
            gameUrl = bundle.getString("url");
        }

        image = (ImageView) findViewById(R.id.previewImage);

        progressDialog = ProgressDialog.show(this, "Зареждане", "Зареждане на картинката", true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!loadImage(false)) loadImage(true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showImage();
                        progressDialog.dismiss();
                        //
                        AnimationSet animset = new AnimationSet(false);
                        //
                        TranslateAnimation anim = new TranslateAnimation(0, 0, -500, 0);
                        anim.setInterpolator(new BounceInterpolator());
                        animset.addAnimation(anim);
                        animset.setDuration(700);
                        //
                        image.startAnimation(animset);
                    }
                });
            }
        }).start();

    }

    private boolean loadImage(boolean setNoImage) {
        File f = new File(cacheDir, gameID + ".jpg");
        //Log.i("GameImagePreview", "local file name = " + f.toString());
        if (f.exists()) {//get from phone
            //Log.i("GameImagePreview", "local file exists");
            try {
                imgBitmap = BitmapFactory.decodeStream(new FileInputStream(f));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e("GameImagePreview", "load local img error! " + e.getMessage());
            }
        } else {//download
            try {
                String url;
                if (setNoImage) {
                    url = "http://"+getString(R.string.url_site)+"/images/no_photo_t.jpg";
                } else {
                    url = "http://www."+getString(R.string.url_site)+"/photos/catalogue/" + gameID + ".jpg";
                }
                //Log.i("GameImagePreview", "downloading image = " + url);
                URL imgUrl = new URL(url);
                //
                URLConnection connection = imgUrl.openConnection();
                connection.setRequestProperty("Referer", getString(R.string.url_game_page) + gameUrl + "_" + gameID);
                connection.setConnectTimeout(1000);
                //
                //TODO: save it to a file first then load from local with resample to keep memory low
                BufferedInputStream bis = new BufferedInputStream(connection.getInputStream(), 8190);
                ByteArrayBuffer baf = new ByteArrayBuffer(50);
                int current;
                while ((current = bis.read()) != -1) {
                    baf.append((byte) current);
                }
                byte[] imageData = baf.toByteArray();
                imgBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                if (imgBitmap != null) {
                    //save the original to the phone
                    FileOutputStream out = new FileOutputStream(f);
                    imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    //Log.i("GameImagePreview", "saved thumb to " + f.toString());
                } else {
                    Toast.makeText(this, "bitmapfactory грешка!", Toast.LENGTH_LONG).show();
                    Log.e("GameImagePreview", "img download error! bitmap obj is null");
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.e("GameImagePreview", "img download error! " + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("GameImagePreview", "img download error! " + e.getMessage());
                //internet problem here
            }
        }
        return true;
    }

    private void showImage() {
        if (imgBitmap != null) {
            image.setImageBitmap(imgBitmap);
            imgBitmap = null;
        } else {
            Toast.makeText(this, "Грешка в зареждането на картинката!", Toast.LENGTH_LONG).show();
            image.setImageResource(R.drawable.noimage);
        }
    }

}
