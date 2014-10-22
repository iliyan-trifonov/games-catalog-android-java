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

public class AuthorGames extends ListActivity {

    private ArrayList<GamesListItem> games = null;
    private GamesListAdapter gameListAdapter;
    private GamesDB db = null;
    private File cacheDir;
    private ProgressDialog progressDialog = null;
    private int author_id = -1;
    private GamesUtils gamesUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            author_id = bundle.getInt("author_id");
        }

        if (bundle != null) {
            addAuthorNameInTitle(bundle.getString("author_name"));
        }

        setContentView(R.layout.main);

        this.gamesUtils = new GamesUtils(this.getApplicationContext());

        cacheDir = this.gamesUtils.findCacheDir(AuthorGames.this);
        games = new ArrayList<GamesListItem>();

        gameListAdapter = new GamesListAdapter(AuthorGames.this, R.layout.row, games, cacheDir);
        setListAdapter(gameListAdapter);
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(AuthorGames.this, R.anim.list_layout_controller);
        getListView().setLayoutAnimation(controller);

        db = new GamesDB(AuthorGames.this);

        loadGames();

    }

    @Override
    protected void onResume() {
        super.onResume();
        //make smooth gradients
        this.gamesUtils.smoothGradients(AuthorGames.this, getWindow());
        //check if update is needed
        ChangedStatusGlobal instance = ChangedStatusGlobal.getInstance();
        if (instance.favoriteChanged()) {
            this.gamesUtils.changeFavoriteOnList("authorGamesList", games, gameListAdapter, instance.getFavs(), false);
        }
        if (instance.gameStatChanged()) {
            this.gamesUtils.changeGameOnList("authorGamesList", games, gameListAdapter, instance.getGameStats());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.closeDB();
        db = null;
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

        Intent intent = new Intent(AuthorGames.this, GameDetails.class);
        Bundle bundle = new Bundle();
        bundle.putInt("feedID", games.get(position).getFeedId());
        bundle.putBoolean("fromAuthorGamesList", true);
        intent.putExtras(bundle);
        AuthorGames.this.startActivityForResult(intent, 101);
    }

    private void loadGames() {
        showDownloadProgress("Зареждане на всички обяви на автора");
        games.clear();
        gameListAdapter.clear();
        new Thread(new Runnable() {
            @Override
            public void run() {
                games = db.authorGames(author_id);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (games.size() == 0) {
                            progressDialog.dismiss();
                            Toast.makeText(AuthorGames.this, "Не са намерени игри!", Toast.LENGTH_SHORT).show();
                        } else populateGamesList();
                    }
                });
            }
        }).start();
    }

    private void showDownloadProgress(String message) {
        progressDialog = ProgressDialog.show(AuthorGames.this, "Зареждане", message, true);
    }

    private void populateGamesList() {
        String[] gamesParams = new String[games.size()];
        gameListAdapter.clear();
        if (games != null && games.size() > 0) {
            RearrangedGamesInfos rearranged = this.gamesUtils.rearrangeGames(games);
            games = rearranged.getGames();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (prefs.getBoolean("gamesCountPref", true)) {
                Toast.makeText(AuthorGames.this, "Показани са " + rearranged.getInfos().get(0) + " нови, "
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

    private void addAuthorNameInTitle(String name) {
        setTitle(getTitle().toString().replace("автор", name));
    }

}
