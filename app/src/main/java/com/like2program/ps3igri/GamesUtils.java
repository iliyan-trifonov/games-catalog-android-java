package com.like2program.ps3igri;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GamesUtils {

    private Context context;

    public GamesUtils(Context applicationContext) {
        this.context = applicationContext;
    }

    public RearrangedGamesInfos rearrangeGames(ArrayList<GamesListItem> games) {
        ArrayList<GamesListItem> result = new ArrayList<GamesListItem>();
        ArrayList<GamesListItem> newGames = new ArrayList<GamesListItem>();
        ArrayList<GamesListItem> changedGames = new ArrayList<GamesListItem>();
        ArrayList<GamesListItem> otherGames = new ArrayList<GamesListItem>();
        ArrayList<Integer> infos = new ArrayList<Integer>();
        //
        for (GamesListItem game : games) {
            if (game.getStatus().equals("new")) {
                newGames.add(game);
            } else if (game.getStatus().equals("changed")) {
                changedGames.add(game);
            } else {
                otherGames.add(game);
            }
        }
        //
        result.addAll(newGames);
        result.addAll(changedGames);
        result.addAll(otherGames);
        //
        infos.add(newGames.size());
        infos.add(changedGames.size());
        infos.add(otherGames.size());
        //
        RearrangedGamesInfos rearranged = new RearrangedGamesInfos();
        rearranged.setGames(result);
        rearranged.setInfos(infos);
        return rearranged;
    }

    public String getGameCatString(String cat) {
        if (cat.equals("sell")) return "Продава";
        if (cat.equals("buy")) return "Купува";
        if (cat.equals("sell_exchange")) return "Продава/Разменя";
        if (cat.equals("exchange")) return "Разменя";
        return cat;
    }

    public File findCacheDir(Context context) {
        File cacheDir;
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            cacheDir = new File(android.os.Environment.getExternalStorageDirectory(), "/data/data/com.like2program.ps3igri/cache/");
        } else {
            cacheDir = context.getCacheDir();
        }
        if (cacheDir != null) {
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            } else if (!cacheDir.isDirectory()) {
                try {
                    if (cacheDir.delete()) {//TODO: on error show a message to the user to delete the dir/file manually
                        cacheDir.mkdirs();
                    }
                } catch (SecurityException se) {
                    se.printStackTrace();
                    Log.e("GamesUtils", "findCacheDir(): delete nondir cachedir error: " + se.getMessage());
                }
            }
        }
        File nomediaFile = new File(cacheDir, ".nomedia");
        if (!nomediaFile.exists()) {
            //Log.i("GamesUtils", "creating nomedia file: " + nomediaFile.toString());
            try {
                nomediaFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return cacheDir;
    }

    public void changeFavoriteOnList(String type, ArrayList<GamesListItem> games, GamesListAdapter adapter,
                                     HashMap<String, HashMap<String, String>> favs, boolean isFromFavsList) {
        ChangedStatusGlobal instance = ChangedStatusGlobal.getInstance();
        //TODO: use tags for every view instead and search by tag later - use the internal for speed
        int favStat;
        for (int i = 0; i < games.size(); i++) {
            if (favs.containsKey(Integer.toString(games.get(i).getFeedId()))) {
                //TODO: PLEASE?! int to string to int to string to int... - univeral string or int values everywhere
                favStat = Integer.parseInt(favs.get(Integer.toString(games.get(i).getFeedId())).get("favStat"));
                instance.setFavUpdated(games.get(i).getFeedId(), type);
                if (favStat == 0 && isFromFavsList) {
                    adapter.remove(games.get(i));
                    games.remove(i);
                } else {
                    games.get(i).setFav(favStat);
                }
            }
            if (!instance.favoriteChanged()) break;//if no changed favs left
        }
        //set to updated the other favs that were not in the games array: TODO: use this as a main loop if possible - smaller loop
        for (String fav_feed_id : favs.keySet()) {
            instance.setFavUpdated(Integer.parseInt(fav_feed_id), type);
        }
        adapter.notifyDataSetChanged();
    }

    //TODO: recode this copy/paste into one single function for favs and game stats
    public void changeGameOnList(String type, ArrayList<GamesListItem> games, GamesListAdapter adapter,
                                 HashMap<String, HashMap<String, String>> stats) {
        ChangedStatusGlobal instance = ChangedStatusGlobal.getInstance();
        for (GamesListItem game : games) {
            if (stats.containsKey(Integer.toString(game.getFeedId()))) {
                game.setStatus("");
                instance.setGameStatUpdated(game.getFeedId(), type);
            }
            if (!instance.gameStatChanged()) break;
        }
        for (String stat_feed_id : stats.keySet()) {
            instance.setGameStatUpdated(Integer.parseInt(stat_feed_id), type);
        }
        adapter.notifyDataSetChanged();
    }

    public boolean setImageViewBitmap(ImageView iv, String url, File cacheDir) {
        String fileName = "noimage.jpg";
        Pattern pattern = Pattern.compile("catalogue/(.*)");
        Matcher matcher = pattern.matcher(url);
        while (matcher.find()) {
            fileName = matcher.group(1);
        }
        File f = new File(cacheDir, fileName);
        if (f.exists()) {//get from phone
            try {
                Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
                iv.setImageBitmap(b);
                return true;
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }

    public String jsonStr2CatType(String jsonCat) {
        if (jsonCat.equals("Продавам")) return "sell";
        else if (jsonCat.equals("Купувам")) return "buy";
        else if (jsonCat.equals("Разменям")) return "exchange";
        else if (jsonCat.equals("Продавам/Разменям")) return "sell_exchange";
        else return "";
    }

    public String generateThumbUrl(int game_id) {
        return this.context.getString(R.string.url_game_thumb) + game_id + "_thumb.jpg";
    }

    public String generateSearchString(String source) {
        Pattern pattern = Pattern.compile(" ");
        Matcher matcher = pattern.matcher(source);
        return matcher.replaceAll("%");
    }

    public long checkCacheSize(Context context) {
        File f = findCacheDir(context);
        File[] files = f.listFiles();
        long filesSize = 0;
        if (files != null) {
            for (File file : files) {
                filesSize += file.length();
            }
        }
        return filesSize;
    }

    public long clearCache(Context context, long currentSize, long desiredSize) {
        File f = findCacheDir(context);
        File[] files = f.listFiles();
        long deletedSize = 0;
        if (files != null) {
            //TODO: check if the sort sorts oldest first
            Arrays.sort(files, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                }
            });
            long stopSize = currentSize - desiredSize;
            for (int i = 0; i < files.length && deletedSize < stopSize; i++) {
                deletedSize += files[i].length();
                files[i].delete();
            }
        }
        return deletedSize;
    }

    public boolean setFavChangedOnGameDetails(HashMap<String, HashMap<String, String>> favs, int feed_id) {
        if (favs.containsKey(Integer.toString(feed_id))) {
            ChangedStatusGlobal instance = ChangedStatusGlobal.getInstance();
            boolean result = favs.get(Integer.toString(feed_id)).get("favStat").equals("1");
            instance.setFavUpdated(feed_id, "gameDetails");
            return result;
        }
        return false;
    }

    public void smoothGradients(Context context, Window window) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("smoothGradientPref", false)) {
            //Log.i("GamesUtils", "smoothGradientPref is true!");
            try {
                // Eliminates color banding
                window.setFormat(PixelFormat.RGBA_8888);
            } catch (Exception e) {
                e.printStackTrace();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("smoothGradientPref", false);
                editor.commit();
                Log.e("GamesUtils", "smoothGradientPref set to false from failure!");
            }
        }
    }

    public String formatDate(String datetime) {
        //2011-05-03 16:01:01
        HashMap<String, String> monthsBG = new HashMap<String, String>();
        monthsBG.put("01", "Януари");
        monthsBG.put("02", "Февруари");
        monthsBG.put("03", "Март");
        monthsBG.put("04", "Април");
        monthsBG.put("05", "Май");
        monthsBG.put("06", "Юни");
        monthsBG.put("07", "Юли");
        monthsBG.put("08", "Август");
        monthsBG.put("09", "Септември");
        monthsBG.put("10", "Октомври");
        monthsBG.put("11", "Ноември");
        monthsBG.put("12", "Декември");
        Pattern pattern = Pattern.compile("([\\d]+)-([\\d]+)-([\\d]+)\\ (.*)");
        Matcher matcher = pattern.matcher(datetime);
        if (matcher.find()) {
            return matcher.group(3)
                    + " "
                    + monthsBG.get(matcher.group(2))
                    + " "
                    + matcher.group(1)
                    + " "
                    + matcher.group(4);
        }
        return datetime;
    }

    /**
     * Display a confirm dialog.
     *
     * @param activity
     * @param title
     * @param message
     * @param positiveLabel
     * @param negativeLabel
     * @param onPositiveClick runnable to call (in UI thread) if positive button pressed. Can be null
     * @param onNegativeClick runnable to call (in UI thread) if negative button pressed. Can be null
     */
    public final void confirm(
            final Activity activity,
            final int title,
            final int message,
            final int positiveLabel,
            final int negativeLabel,
            final Runnable onPositiveClick,
            final Runnable onNegativeClick) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setPositiveButton(positiveLabel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int buttonId) {
                        if (onPositiveClick != null) onPositiveClick.run();
                    }
                });
        dialog.setNegativeButton(negativeLabel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int buttonId) {
                        if (onNegativeClick != null) onNegativeClick.run();
                    }
                });
        dialog.setIcon(android.R.drawable.ic_dialog_alert);
        dialog.show();

    }

}
