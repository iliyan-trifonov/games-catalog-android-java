package com.like2program.ps3igri;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LazyImageListDownloader extends AsyncTask<String, Void, Void> {

    private File cacheDir;
    private GamesListAdapter adapter;
    private boolean imageDownloaded = false;
    private ChangedStatusGlobal instance = ChangedStatusGlobal.getInstance();

    public LazyImageListDownloader(File cacheDir, GamesListAdapter adapter) {
        this.cacheDir = cacheDir;
        this.adapter = adapter;
        instance.setAdapterNotified(false);
    }

    @Override
    protected Void doInBackground(String... urls) {
        //Log.i("LazyImageListDownloader", "urls.length = " + urls.length);
        for (String url : urls) {
            if (!downloadImage(url, false)) {
                //Log.i("LazyImageListDownloader", "downloading noimage for this game");
                downloadImage(url, true);
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        if (imageDownloaded) {
            adapter.notifyDataSetChanged();
            //Log.i("LazyImageListDownloader", "adapter notified of the changes");
        }
        instance.setAdapterNotified(true);//TODO: rename it to lazydownlfinished(true)
        //TODO: count number of tries to download not only successfull and if counter>0 -> notify adapter
        //adapter.notifyDataSetChanged();//test: always notify - breaks the layout animation on the main list, etc.
    }

    public boolean downloadImage(String url, boolean setNoImage) {
        String fileName = "noimage.jpg";
        //http://site_url/photos/catalogue/13452_thumb.jpg
        Pattern pattern = Pattern.compile("catalogue/(.*)");
        Matcher matcher = pattern.matcher(url);
        while (matcher.find()) {
            fileName = matcher.group(1);
        }
        File f = new File(cacheDir, fileName);
        if (setNoImage) {
            File noImageFile = new File(cacheDir, "no_photo_t.jpg");
            if (noImageFile.exists()) {
                //Log.i("LazyImageListDownloader", "noimage file exists, copying..");
                //copy the noimage jpg file
                try {
                    FileInputStream in = new FileInputStream(noImageFile);
                    FileOutputStream out = new FileOutputStream(f);
                    byte[] bytes = null;
                    while (in.read(bytes) != -1) {
                        out.write(bytes);
                    }
                    in.close();
                    out.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            }
        }
        if (f.exists()) {//get from phone
            //nothing here..
            //Log.i("LazyImageListDownloader", "downloadImage(): local file exists");
        } else {//download
            try {
                imageDownloaded = true;
                //Log.i("LazyImageListDownloader", "downloadImage(): downloading image = " + url);
                Bitmap imgBitmap;
                String noImageUrl = this.adapter.getContext().getString(R.string.url_no_image_thumb);
                URL imgUrl = new URL(setNoImage ? noImageUrl : url);
                //Log.i("LazyImageListDownloader", "downloading from url: " + imgUrl.toString());
                //
                URLConnection connection = imgUrl.openConnection();
                connection.setRequestProperty("Referer", this.adapter.getContext().getString(R.string.url_referer));
                connection.setConnectTimeout(1500);
                //
                //TODO: use bufferedinputstream(like in the previewimage load) to skip bitmapfactory decodestream errors
                imgBitmap = BitmapFactory.decodeStream(connection.getInputStream());
                if (imgBitmap != null) {
                    //save the original to the phone
                    FileOutputStream out = new FileOutputStream(f);
                    imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    //Log.i("LazyImageListDownloader", "saved thumb to " + f.toString());
                } else {
                    Log.e("LazyImageListDownloader", "img download error! bitmap obj is null");
                }
            } catch (FileNotFoundException e) {
                return false;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                //internet problem here
            }
        }
        return true;
    }


}
