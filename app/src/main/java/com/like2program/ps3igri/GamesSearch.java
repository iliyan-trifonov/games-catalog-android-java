package com.like2program.ps3igri;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class GamesSearch extends ListActivity {

    private ArrayList<GamesListItem> games = null;
    private GamesListAdapter gameListAdapter;
    private ProgressDialog progressDialog = null;
    private File cacheDir;
    private Button searchButton;
    private EditText searchBox;
    private GamesDB db = null;
    private GamesUtils gamesUtils;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);

        this.gamesUtils = new GamesUtils(getApplicationContext());

        cacheDir = this.gamesUtils.findCacheDir(GamesSearch.this);
        db = new GamesDB(GamesSearch.this);
        games = new ArrayList<GamesListItem>();

        gameListAdapter = new GamesListAdapter(this, R.layout.row, games, cacheDir);
        setListAdapter(gameListAdapter);
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(this, R.anim.list_layout_controller);
        getListView().setLayoutAnimation(controller);

        searchButton = (Button) findViewById(R.id.button1);
        searchBox = (EditText) findViewById(R.id.editText1);
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    searchGames();
                    return true;
                }
                return false;
            }
        });
        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                searchGames();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //make smooth gradients
        this.gamesUtils.smoothGradients(GamesSearch.this, getWindow());
        //check if update is needed
        ChangedStatusGlobal instance = ChangedStatusGlobal.getInstance();
        if (instance.favoriteChanged()) {
            //Toast.makeText(this, "Favorite change on SearchList! ("+instance.getFavsCount()+")", Toast.LENGTH_SHORT).show();
            this.gamesUtils.changeFavoriteOnList("searchList", games, gameListAdapter, instance.getFavs(), false);
        }
        if (instance.gameStatChanged()) {
            //Toast.makeText(this, "Game stats change on SearchList! ("+instance.getGameStatsCount()+")", Toast.LENGTH_SHORT).show();
            this.gamesUtils.changeGameOnList("searchList", games, gameListAdapter, instance.getGameStats());
        }
        instance.setFavListUpdAllowed(false);
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

        Intent intent = new Intent(GamesSearch.this, GameDetails.class);
        Bundle bundle = new Bundle();
        bundle.putInt("feedID", games.get(position).getFeedId());
        intent.putExtras(bundle);
        GamesSearch.this.startActivity(intent);
    }

    private void showDownloadProgress(String message) {
        progressDialog = ProgressDialog.show(this, "Зареждане", message, true);
    }

    private void populateGamesList() {
        String[] gamesParams = new String[games.size()];
        gameListAdapter.clear();
        if (games != null && games.size() > 0) {
            RearrangedGamesInfos rearranged = this.gamesUtils.rearrangeGames(games);
            games = rearranged.getGames();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (prefs.getBoolean("gamesCountPref", true)) {
                Toast.makeText(GamesSearch.this, "Показани са " + rearranged.getInfos().get(0) + " нови, "
                        + rearranged.getInfos().get(1) + " променени и "
                        + rearranged.getInfos().get(2) + " други игри",
                        Toast.LENGTH_SHORT).show();
            }
            for (int i = 0; i < games.size(); i++) {
                gameListAdapter.add(games.get(i));
                gamesParams[i] = this.gamesUtils.generateThumbUrl(games.get(i).getGameId());
            }
        }
        //download not yet downloaded images
        new LazyImageListDownloader(cacheDir, gameListAdapter).execute(gamesParams);
        //
        progressDialog.dismiss();
    }

    public void searchGames() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
        showDownloadProgress("Търсене на игри..");
        new Thread(new Runnable() {
            @Override
            public void run() {
                games = db.searchGames(new GamesUtils(getApplicationContext()).generateSearchString(searchBox.getText().toString()));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        populateGamesList();
                    }
                });
            }
        }).start();
    }

}
