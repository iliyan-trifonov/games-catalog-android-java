package com.like2program.ps3igri;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends ListActivity {

    private ArrayList<GamesListItem> games = new ArrayList<GamesListItem>();
    private GamesListAdapter gameListAdapter;
    private Runnable loadGames;
    private ProgressDialog progressDialog = null;
    private GamesDB db = null;
    private boolean dbWasEmpty = false;
    private File cacheDir;
    private boolean internetProblem = false;
    private boolean loadAllDbGames = false;
    private long filesSize = 0;
    private long deletedSize = 0;
    private int gamesCount = 0;
    private boolean gamesCleared = false;
    private long maxCacheSize = -1;
    private long maxDbRecords = -1;
    private GamesUtils gamesUtils;
    private Runnable returnGamesList = new Runnable() {
        public void run() {
            if (internetProblem) {
                Toast.makeText(MainActivity.this, "Проблем с интернет връзката! Моля, опитайте отново.", Toast.LENGTH_SHORT).show();
                closeDownloadProgress();
                internetProblem = false;
            } else {
                populateGamesList();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        this.gamesUtils = new GamesUtils(getApplicationContext());

        setTitle("Списък :: " + getString(R.string.app_name_suffix));//TODO: make it from the manifest/layout xml without loosing the app name

        cacheDir = this.gamesUtils.findCacheDir(MainActivity.this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        //TODO: check why prefs.getInt() didn't work the first time and how to save int prefs
        String cacheSizePref = prefs.getString("cacheSizePref", "10");
        if (cacheSizePref.equals("")) {
            cacheSizePref = "10";
            prefs.edit().putString("cacheSizePref", "10").commit();
        }
        String dbSizePref = prefs.getString("dbSizePref", "1000");
        if (dbSizePref.equals("")) {
            dbSizePref = "1000";
            prefs.edit().putString("dbSizePref", "1000").commit();
        }
        maxCacheSize = Long.parseLong(cacheSizePref) * 1024 * 1024;
        maxDbRecords = Long.parseLong(dbSizePref);

        //set the 2 boolean prefs if not set yet
        //boolean dbClearDoneMessPref = prefs.getBoolean("dbClearDoneMessPref", false);
        //boolean cacheClearDoneMessPref = prefs.getBoolean("cacheClearDoneMessPref", false);

        boolean firstRun = prefs.getBoolean("firstRun", true);
        if (firstRun) {
            prefs.edit().putBoolean("firstRun", false).commit();
            programHelp();
        }

        db = new GamesDB(MainActivity.this);

        ChangedStatusGlobal instance = ChangedStatusGlobal.getInstance();
        if (!instance.getCacheCleared()) {
            checkCacheLimit();
            instance.setCacheCleared();
        }
        checkGamesLimit();

        setListAnimation();

        getGames();

    }

    @Override
    protected void onResume() {
        super.onResume();
        //make smooth gradients
        this.gamesUtils.smoothGradients(MainActivity.this, getWindow());
        //check if update is needed
        ChangedStatusGlobal instance = ChangedStatusGlobal.getInstance();
        if (instance.favoriteChanged()) {
            //Toast.makeText(this, "Favorite change on MainList! ("+instance.getFavsCount()+")", Toast.LENGTH_SHORT).show();
            this.gamesUtils.changeFavoriteOnList("mainList", games, gameListAdapter, instance.getFavs(), false);
        }
        if (instance.gameStatChanged()) {
            //Toast.makeText(this, "Game stats change on MainList! ("+instance.getGameStatsCount()+")", Toast.LENGTH_SHORT).show();
            this.gamesUtils.changeGameOnList("mainList", games, gameListAdapter, instance.getGameStats());
        }
        instance.setFavListUpdAllowed(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        ChangedStatusGlobal instance = ChangedStatusGlobal.getInstance();
        instance.resetAll();
        //TODO: test System.exit(0); here with some messages and values in the Singleton class
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (!games.get(position).getStatus().equals("")) {
            TextView text = (TextView) v.findViewById(R.id.textView1);
            if (text != null) {
                //text.setTextColor(Color.WHITE);
                text.setTextColor(Color.BLACK);//skin test
            }
            //
            games.get(position).setStatus("");
        }

        Intent intent = new Intent(MainActivity.this, GameDetails.class);
        //TODO: remove the bundle and use putint()/putextra() or similar only
        Bundle bundle = new Bundle();
        bundle.putInt("feedID", games.get(position).getFeedId());
        intent.putExtras(bundle);
        MainActivity.this.startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.load_json:
                loadGamesList();//from json
                return true;
            case R.id.load_db:
                getListFromDB(false, "");
                return true;
            case R.id.filter_sell:
                getListFromDB(false, "sell");
                return true;
            case R.id.filter_exchange:
                getListFromDB(false, "exchange");
                return true;
            case R.id.filter_buy:
                getListFromDB(false, "buy");
                return true;
            case R.id.filter_all://same as show last games main menu
                getListFromDB(false, "");
                return true;
            case R.id.load_db_favs:
                showFavs();
                return true;
            case R.id.search:
                searchStart();
                return true;
            case R.id.set_all_read:
                setAllRead();
                return true;
            case R.id.show_all_results:
                getListFromDB(true, "");
                return true;
            case R.id.menu_settings:
                showPreferences();
                return true;
            case R.id.menu_about:
                aboutProgram();
                return true;
            case R.id.menu_help:
                programHelp();
                return true;
            case R.id.exit:
                System.exit(0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.closeDB();
        db = null;
    }

    private void getGamesJSON() {
        downloadAndParseJSON(true);
        if (games.size() == 0) downloadAndParseJSON(false);//load last 50 if no games for today
        checkAndSaveInDB();
        if (dbWasEmpty) {
            db.reverseResults();
            dbWasEmpty = false;
        }
        games = db.getLastGames(50, "");
        //Log.i("MainActivity", "getGamesJSON(): got from db = " + games.size() + " games");
        runOnUiThread(returnGamesList);
    }

    private void loadGamesList() {
        gameListAdapter.clear();
        showDownloadProgress("Зареждане на списъка от интернет");
        loadGames = new Runnable() {
            public void run() {
                getGamesJSON();
            }
        };
        Thread thread = new Thread(null, loadGames, "loadGamesThread");
        thread.start();
    }

    private void downloadAndParseJSON(boolean today) {
        //Log.i("MainActivity", "downloadAndParseJSON() called");
        String jsonEncoded = "";
        try {
            URL url = new URL(getString(R.string.data_url_json));
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(1500);
            connection.setDoOutput(true);
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            if (today) out.write("date=" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String result;
            while ((result = in.readLine()) != null) {
                jsonEncoded += result;
            }
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Log.w("MainActivity", "downloadAndParseJSON(): internet problem!");
            internetProblem = true;
        }
        try {
            JSONArray jsonArr = new JSONArray(jsonEncoded);
            JSONObject jsonObj;
            GamesListItem item;
            games.clear();
            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i);
                item = new GamesListItem();
                item.setGameId(jsonObj.getInt("id"));
                item.setTitle(jsonObj.getString("title"));
                item.setDescr(jsonObj.getString("description").replace("\r", ""));
                item.setSeen(jsonObj.getString("published"));
                item.setPubDate(jsonObj.getString("published_first"));
                item.setCat(this.gamesUtils.jsonStr2CatType(jsonObj.getString("offer_type")));
                item.setUrl(jsonObj.getString("url"));
                item.setPrice(jsonObj.getInt("price"));
                item.setRating(jsonObj.getString("rating"));
                //contacts
                item.setAuthorName(!jsonObj.getString("name").equals("null") ? jsonObj.getString("name") : "");
                item.setAuthorEmail(!jsonObj.getString("mail").equals("null") ? jsonObj.getString("mail") : "");
                item.setAuthorPhone(!jsonObj.getString("phone").equals("null") ? jsonObj.getString("phone") : "");
                item.setAuthorSkype(!jsonObj.getString("skype").equals("null") ? jsonObj.getString("skype") : "");
                item.setAuthorCity(!jsonObj.getString("city").equals("null") ? jsonObj.getString("city") : "");
                item.setAuthorId(jsonObj.getInt("author_id"));
                item.setOtherContacts(jsonObj.getString("other_contacts"));
                //
                games.add(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("MainActivity", "downloadAndParseJSON(): reading json errors! " + e.getMessage());
        }
    }

    private void checkAndSaveInDB() {
        GamesListItem item;
        db.startTransaction();
        for (int i = 0; i < games.size(); i++) {
            item = games.get(i);
            item.setStatus(db.gameStatus(item));
            //TODO: not optimized for DB here
            if (!db.gameExists(item.getGameId())) {
                boolean insRes = db.insertGame(item);
                if (!insRes) Log.w("MainActivity", "insRes false! - item = " + item);
            } else if (item.getStatus().equals("changed")) {
                boolean updRes = db.updateGame(item);
                if (!updRes) Log.w("MainActivity", "updRes false! - item = " + item);
            }
            //set game id
            item.setFeedId(db.getFeedId(item.getGameId()));//TODO: take the id from the exists check above
            //
            games.set(i, item);
        }
        db.finishTransaction();
    }

    private void populateGamesList() {
        String[] gamesParams = new String[games.size()];
        gameListAdapter.clear();
        if (games != null && games.size() > 0) {
            RearrangedGamesInfos rearranged = this.gamesUtils.rearrangeGames(games);
            games = rearranged.getGames();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (prefs.getBoolean("gamesCountPref", true)) {
                Toast.makeText(MainActivity.this, "Показани са " + rearranged.getInfos().get(0) + " нови, "
                        + rearranged.getInfos().get(1) + " променени и "
                        + rearranged.getInfos().get(2) + " други игри",
                        Toast.LENGTH_SHORT).show();
            }
            for (int i = 0; i < games.size(); i++) {
                gameListAdapter.add(games.get(i));
                gamesParams[i] = this.gamesUtils.generateThumbUrl(games.get(i).getGameId());
            }
            //TODO: store the game urls needed by the details activity somewhere else/or use the adapter array/ and delete the big games array
            //games.clear();
            //games = null;
        }
        //download not yet downloaded images
        new LazyImageListDownloader(cacheDir, gameListAdapter).execute(gamesParams);
        //
        closeDownloadProgress();
    }

    private void showDownloadProgress(String message) {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
        progressDialog = ProgressDialog.show(MainActivity.this, "Зареждане", message, true);
    }

    private void closeDownloadProgress() {
        progressDialog.dismiss();
    }

    private void getListFromDB(boolean all, final String cat_filter) {

        titleFromFilter("Списък", cat_filter);

        showDownloadProgress("Зареждане " + (all ? "на всички " : "") + "от локалните записи");
        games.clear();
        gameListAdapter.clear();

        loadAllDbGames = all;
        new Thread(new Runnable() {
            @Override
            public void run() {
                games = db.getLastGames(loadAllDbGames ? 0 : 50, cat_filter);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (games.size() == 0) {
                            closeDownloadProgress();
                            Toast.makeText(MainActivity.this, "Не са намерени игри!", Toast.LENGTH_SHORT).show();
                            if (cat_filter.equals("")) {
                                //Log.i("MainActivity", "db is empty, loading json..");
                                dbWasEmpty = true;
                                loadGamesList();//from json
                            }
                        } else populateGamesList();
                    }
                });
            }
        }).start();

    }

    private void searchStart() {
        Intent intent = new Intent(MainActivity.this, GamesSearch.class);
        MainActivity.this.startActivity(intent);
    }

    private void showFavs() {
        Intent intent = new Intent(MainActivity.this, GamesFavoritesActivity.class);
        MainActivity.this.startActivity(intent);
    }

    private void getGames() {
        getListFromDB(false, "");
    }

    private void titleFromFilter(String replaceable, String cat_filter) {
        String title = getTitle().toString();
        if (!cat_filter.equals("")) {
            Pattern pattern = Pattern.compile(replaceable + "[^ ]*");
            Matcher matcher = pattern.matcher(title);
            title = matcher.replaceFirst(replaceable + "(" + this.gamesUtils.getGameCatString(cat_filter) + ")");
        } else {
            Pattern pattern = Pattern.compile(replaceable + "[^ ]*");
            Matcher matcher = pattern.matcher(title);
            title = matcher.replaceAll(replaceable);
        }
        setTitle(title);
    }

    private void checkGamesLimit() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                gamesCount = db.getGamesCount();
                gamesCleared = gamesCount > maxDbRecords && db.deleteOldRecords(maxDbRecords);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                        if (gamesCleared && prefs.getBoolean("dbClearDoneMessPref", false)) {
                            Toast.makeText(MainActivity.this, "Игри в БД преди: " + gamesCount
                                    + (gamesCleared ? ",\nБД е почистена от старите записи" : "")
                                    , Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void checkCacheLimit() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                filesSize = new GamesUtils(getApplicationContext()).checkCacheSize(MainActivity.this);
                if (filesSize > maxCacheSize) {
                    deletedSize = new GamesUtils(getApplicationContext()).clearCache(MainActivity.this, filesSize, maxCacheSize);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                        if (deletedSize > 0 && prefs.getBoolean("cacheClearDoneMessPref", false)) {
                            //Log.i("MainActivity", "deletedSize = " + deletedSize);
                            double cacheDoubleSize = filesSize / 1024.0 / 1024.0;
                            double deletedDoubleSize = deletedSize / 1024.0 / 1024.0;
                            DecimalFormat df = new DecimalFormat("#.##");
                            //Log.i("MainActivity", "deletedDoubleSize = " + deletedDoubleSize);
                            Toast.makeText(MainActivity.this, "Размер на кеша преди: " + df.format(cacheDoubleSize) + "MB"
                                    + (deletedSize > 0 ? ",\nПочистени са " + df.format(deletedDoubleSize) + "MB" : "")
                                    , Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void setAllRead() {
        this.gamesUtils.confirm(this, R.string.confirm_dialog_title, R.string.confirm_dialog_message, R.string.confirm_dialog_yes, R.string.confirm_dialog_no,
                new Runnable() {
                    @Override
                    public void run() {
                        showDownloadProgress("Маркиране на всички като прочетени..");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                db.setAllRead();//TODO: put in thread + progress dialog
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        closeDownloadProgress();
                                        Toast.makeText(MainActivity.this, "Всички са маркирани като прочетени",
                                                Toast.LENGTH_SHORT).show();
                                        getListFromDB(false, "");
                                    }
                                });
                            }
                        }).start();
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        //nothing
                    }
                }
        );
    }

    private void showPreferences() {
        Intent intent = new Intent(getBaseContext(), Preferences.class);
        startActivity(intent);
    }

    private void setListAnimation() {
        gameListAdapter = new GamesListAdapter(MainActivity.this, R.layout.row, games, cacheDir);
        setListAdapter(gameListAdapter);
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(MainActivity.this, R.anim.list_layout_controller);
        getListView().setLayoutAnimation(controller);
    }

    private void aboutProgram() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("За програмата");
        builder.setMessage(
                "Версия: " + getVersion() + "\n" +
                        "Автор: Илиян Трифонов\n" +
                        "Сайт: iliyan-trifonov.com\n\n" +
                        "Приложението е създадено с подкрепата на "+getString(R.string.url_site)+"!\n\n" +
                        "Посетете "+getString(R.string.url_site)+" и си регистрирайте акаунт, за да се възползвате от допълнителните услуги на сайта.");
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void programHelp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ръководство");
        builder.setMessage("Message");
        builder.setPositiveButton("Затвори", null);
        AlertDialog alert = builder.show();
        TextView myMsg = (TextView) alert.findViewById(android.R.id.message);
        myMsg.setGravity(Gravity.FILL_HORIZONTAL);
        myMsg.setText("Добре дошли в помощника за обяви, публикувани в "+getString(R.string.url_site)+"!\n\n" +
                "В началия екран ще видите списък с последните обяви от днес.\n\n" +
                "Обикновено се показват последните 50 обяви (по-малко или най-много 50).\n\n" +
                "Ако с времето сте изтеглили нови обяви и те не са прочетени, ще се показват повече от 50.\n\n" +
                "Можете да маркирате всички обяви като прочетени от менюто, а можете и да видите всички, които сте изтеглили досега, отново от менюто.\n\n" +
                "Използвайте менюто 'Изтегли' често, за да попълвате базата данни на приложението при Вас.\n\n" +
                "Маркирайте всички като прочетени, за да не ви се претрупва началния екран.\n\n" +
                "Използвайте менюто 'Търсене', за да намерите обява по част от името й.\n\n" +
                "---\n" +
                "\nКогато разглеждате обява, имате опции в менюто за:\n\n" +
                " - запазване на обявата в 'Любими'\n\n" +
                " - показване на други обяви на автора\n\n" +
                " - свързване с автора по телефон или имейл (ако са въведени)\n\n" +
                " - разглеждане на по-голямата картинка на обявата.\n\n" +
                "Можете също така да щракнете малката картинка на обявата, за да заредите по-голямата.\n\n" +
                "---\n" +
                "\nЦветове в списъка:\n\n" +
                "Червен: обявата е нова за Вашия локален списък.\n\n" +
                "Син: обявата е във Вашия списък, но е подновена на сайта.\n\n" +
                "Черен: обявата е във Вашия списък и е прочетена.\n\n" +
                "---\n" +
                "\nКогато дадена обява ви е любима, до заглавието й има звездичка.\n\n" +
                "\nСледете текста в най-горната лента на програмата, за да се упътите.");
    }

    private String getVersion() {
        try {
            PackageInfo pi;
            pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pi.versionName;
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "?";
    }

}