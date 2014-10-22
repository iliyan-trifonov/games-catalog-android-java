package com.like2program.ps3igri;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

//TODO: make a favs clear/leave last 100/ menu

public class GamesFavoritesActivity extends ListActivity {

    private ArrayList<GamesListItem> games = null;
    private GamesListAdapter gameListAdapter;
    private GamesDB db = null;
    private File cacheDir;
    private ProgressDialog progressDialog = null;
    private GamesUtils gamesUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        this.gamesUtils = new GamesUtils(getApplicationContext());

        cacheDir = this.gamesUtils.findCacheDir(GamesFavoritesActivity.this);
        games = new ArrayList<GamesListItem>();

        gameListAdapter = new GamesListAdapter(GamesFavoritesActivity.this, R.layout.row, games, cacheDir);
        setListAdapter(gameListAdapter);
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(GamesFavoritesActivity.this, R.anim.list_layout_controller);
        getListView().setLayoutAnimation(controller);

        db = new GamesDB(GamesFavoritesActivity.this);

        loadFavs();

        ChangedStatusGlobal instance = ChangedStatusGlobal.getInstance();
        instance.setFavListUpdAllowed(true);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //make smooth gradients
        this.gamesUtils.smoothGradients(GamesFavoritesActivity.this, getWindow());
        //check if update is needed
        ChangedStatusGlobal instance = ChangedStatusGlobal.getInstance();
        if (instance.favoriteChanged()) {
            //Toast.makeText(this, "Favorite change on FavsList! ("+instance.getFavsCount()+")", Toast.LENGTH_SHORT).show();
            this.gamesUtils.changeFavoriteOnList("favsList", games, gameListAdapter, instance.getFavs(), true);
        }
        if (instance.gameStatChanged()) {
            //Toast.makeText(this, "Game stats change on FavsList! ("+instance.getGameStatsCount()+")", Toast.LENGTH_SHORT).show();
            this.gamesUtils.changeGameOnList("favsList", games, gameListAdapter, instance.getGameStats());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.closeDB();
        db = null;
    }

    private void showDownloadProgress(String message) {
        progressDialog = ProgressDialog.show(GamesFavoritesActivity.this, "Зареждане", message, true);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (!games.get(position).getStatus().equals("")) {
            ChangedStatusGlobal instance = ChangedStatusGlobal.getInstance();
            instance.setGameStatChanged(games.get(position).getFeedId());
            //
            games.get(position).setStatus("");
            TextView text = (TextView) v.findViewById(R.id.textView1);
            //text.setTextColor(Color.WHITE);
            text.setTextColor(Color.BLACK);//skin test
        }

        Intent intent = new Intent(GamesFavoritesActivity.this, GameDetails.class);
        Bundle bundle = new Bundle();
        bundle.putInt("feedID", games.get(position).getFeedId());
        intent.putExtras(bundle);
        GamesFavoritesActivity.this.startActivityForResult(intent, 101);
    }

    private void populateGamesList() {
        String[] gamesParams = new String[games.size()];
        gameListAdapter.clear();
        if (games != null && games.size() > 0) {
            RearrangedGamesInfos rearranged = this.gamesUtils.rearrangeGames(games);
            games = rearranged.getGames();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (prefs.getBoolean("gamesCountPref", true)) {
                Toast.makeText(GamesFavoritesActivity.this, "Показани са " + rearranged.getInfos().get(0) + " нови, "
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
        progressDialog.dismiss();
    }

    private void loadFavs() {
        showDownloadProgress("Зареждане на любимите");
        games.clear();
        gameListAdapter.clear();

        new Thread(new Runnable() {
            @Override
            public void run() {
                games = db.favGames(0);//show last 100 if too many results and create a Toast to notify the user
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (games.size() == 0) {
                            progressDialog.dismiss();
                            Toast.makeText(GamesFavoritesActivity.this, "Не са намерени игри!", Toast.LENGTH_SHORT).show();
                        } else populateGamesList();
                    }
                });
            }
        }).start();
    }

}
