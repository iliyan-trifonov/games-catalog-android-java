package com.like2program.ps3igri;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameDetails extends Activity {

    private int feedID = 0;
    private GamesDB db = null;
    private File cacheDir = null;
    private HashMap<String, String> dbInfos = null;
    private HashMap<String, String> infos = null;
    private boolean infosReady = false;
    private boolean fromAuthorGamesList = false;
    private GamesUtils gamesUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_details);

        this.gamesUtils = new GamesUtils(this.getApplicationContext());

        showLoadingStatus();

        Bundle bundle = getIntent().getExtras();
        feedID = bundle.getInt("feedID");
        fromAuthorGamesList = bundle.getBoolean("fromAuthorGamesList", false);

        db = new GamesDB(GameDetails.this);
        cacheDir = this.gamesUtils.findCacheDir(GameDetails.this);

        //skin test
        //TODO: set it in the layout xml
        ScrollView sv = (ScrollView) findViewById(R.id.scrollView1);
        sv.setBackgroundColor(Color.WHITE);
        //

        new Thread(new Runnable() {
            @Override
            public void run() {
                loadInfos();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        populateInfos();
                    }
                });
            }
        }).start();

        ImageView image = (ImageView) findViewById(R.id.gameImage);
        if (image != null) {
            OnClickListener listener = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showImagePreview();
                }
            };
            image.setOnClickListener(listener);
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.closeDB();
        db = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ChangedStatusGlobal instance = ChangedStatusGlobal.getInstance();
        if (instance.favoriteChanged()) {
            //Toast.makeText(this, "Favorite change on gameDetails! ("+instance.getFavsCount()+")", Toast.LENGTH_SHORT).show();
            boolean favRes = this.gamesUtils.setFavChangedOnGameDetails(instance.getFavs(), feedID);
            ImageView favsIcon = (ImageView) findViewById(R.id.favsIcon);
            favsIcon.setVisibility(favRes ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.favs_add:
                changeFavStatus(1);
                return true;
            case R.id.favs_remove:
                changeFavStatus(0);
                return true;
            case R.id.image_preview:
                showImagePreview();
                return true;
            case R.id.author_games:
                showAuthorGames();
                return true;
            case R.id.author_phone:
                dialAuthor();
                return true;
            case R.id.author_email:
                emailAuthor();
                return true;
        /*case R.id.author_skype:
        	skypeAuthor();
        	return true;*/
            case R.id.game_url:
                showGameUrl();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!infosReady) {
            Toast.makeText(this, "Моля, изчакайте зареждането", Toast.LENGTH_SHORT).show();
            return true;
        }
        MenuItem favs_add = menu.findItem(R.id.favs_add);
        MenuItem favs_remove = menu.findItem(R.id.favs_remove);
        if (infos.get("fav").toString().equals("1")) {
            favs_add.setVisible(false);
            favs_remove.setVisible(true);
        } else {
            favs_add.setVisible(true);
            favs_remove.setVisible(false);
        }
        if (fromAuthorGamesList) {
            MenuItem author_games = menu.findItem(R.id.author_games);
            author_games.setVisible(false);
        }
        if (infos.get("author_phone").equals(""))
            menu.findItem(R.id.author_phone).setVisible(false);
        if (infos.get("author_email").equals(""))
            menu.findItem(R.id.author_email).setVisible(false);
        return true;
    }

    private void changeFavStatus(int favStatus) {
        boolean favRes = db.setFav(feedID, favStatus);//TODO: use feed_id here
        String messTxt = "";
        if (favRes) {
            infos.put("fav", Integer.toString(favStatus));
            //hide the fav icon
            ImageView favsIcon = (ImageView) findViewById(R.id.favsIcon);
            favsIcon.setVisibility(favStatus == 0 ? View.GONE : View.VISIBLE);
            ChangedStatusGlobal instance = ChangedStatusGlobal.getInstance();
            instance.setFavChanged(feedID, favStatus);//notify all other activities showing the favorite by setting a global flag
            messTxt = "Успешно " + (favStatus == 0 ? "премахване от" : "добавяне в") + " любими!";
            Toast.makeText(GameDetails.this, messTxt, Toast.LENGTH_SHORT).show();
        }
    }

    public void populateInfos() {
        try {
            //TODO: use custom class to receive the infos data and pass it here into strings and spanned simultaneously
            //fav icon
            if (infos.get("fav").toString().equals("1")) {
                ImageView favsIcon = (ImageView) findViewById(R.id.favsIcon);
                if (favsIcon != null)
                    favsIcon.setVisibility(View.VISIBLE);
            }
            //title
            TextView titleText = (TextView) findViewById(R.id.titleText);
            if (titleText != null) {
                titleText.setTextColor(Color.BLACK);//skin test
                titleText.setText(infos.get("title"));
            }
            //image
            ImageView image = (ImageView) findViewById(R.id.gameImage);
            if (image != null)
                this.gamesUtils.setImageViewBitmap(image, infos.get("thumb"), cacheDir);//TODO: use a bitmap returned result instead
            //date published
            TextView dateText = (TextView) findViewById(R.id.dateText);
            if (dateText != null) {
                dateText.setTextColor(Color.BLACK);//skin test
                dateText.setText(this.gamesUtils.formatDate(infos.get("date")));
            }
            //date last seen
            TextView dateSeenText = (TextView) findViewById(R.id.dateSeenText);
            if (dateSeenText != null) {
                dateSeenText.setTextColor(Color.BLACK);//skin test
                dateSeenText.setText(this.gamesUtils.formatDate(infos.get("seen")));
            }
            //price
            TextView priceText = (TextView) findViewById(R.id.priceText);
            if (priceText != null) {
                priceText.setTextColor(Color.BLACK);//skin test
                priceText.setText(infos.get("price"));
            }
            //rating
            TextView ratingText = (TextView) findViewById(R.id.ratingText);
            if (ratingText != null) {
                ratingText.setTextColor(Color.BLACK);//skin test
                if (!infos.get("rating").equals("")) ratingText.setText(infos.get("rating"));
            }
            //contacts
            TextView contactsText = (TextView) findViewById(R.id.contactsText);
            if (contactsText != null) {
                contactsText.setTextColor(Color.BLACK);//skin test
                if (!infos.get("contacts").toString().equals(""))
                    contactsText.setText(infos.get("contacts"));
                else contactsText.setText("Няма");//should never happen
            }
            //description
            TextView descrText = (TextView) findViewById(R.id.descrText);
            if (descrText != null) {
                descrText.setTextColor(Color.BLACK);//skin test
                descrText.setText(infos.get("descr"));
            }
        } catch (Exception e) {
            Toast.makeText(GameDetails.this, "Проблем с попълването на полетата! "
                    + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        infosReady = true;
    }

    private void loadInfos() {
        dbInfos = db.getGameInfos(feedID);
        //update the status to 'read' = ""
        if (!dbInfos.get("status").equals("")) {
            db.updateGameStatus(feedID, "");
            dbInfos.put("status", "");
        }
        //format the results
        infos = new HashMap<String, String>();
        String catStr = this.gamesUtils.getGameCatString(dbInfos.get("cat"));
        if (!catStr.equals("")) catStr += " : ";
        infos.put("title", (catStr) + dbInfos.get("title"));
        //copy
        infos.put("fav", dbInfos.get("fav"));
        infos.put("game_id", dbInfos.get("game_id"));
        infos.put("url", dbInfos.get("url"));
        infos.put("author_id", dbInfos.get("author_id"));
        infos.put("author_name", dbInfos.get("author_name"));
        infos.put("rating", dbInfos.get("rating"));
        //
        infos.put("thumb", this.gamesUtils.generateThumbUrl(Integer.parseInt(dbInfos.get("game_id"))));
        infos.put("date", dbInfos.get("pubdate"));
        infos.put("seen", dbInfos.get("seen"));
        infos.put("price", (!dbInfos.get("price").equals("0")
                ? dbInfos.get("price") + " лв."
                : "не е посочена"));
        infos.put("contacts",
                (!dbInfos.get("author_name").equals("") ? "Име: " + dbInfos.get("author_name") + "\n" : "")
                        + (!dbInfos.get("author_city").equals("") ? "Град: " + dbInfos.get("author_city") + "\n" : "")
                        + (!dbInfos.get("author_email").equals("") ? "Email: " + dbInfos.get("author_email") + "\n" : "")
                        + (!dbInfos.get("author_phone").equals("") ? "Телефон: " + dbInfos.get("author_phone") + "\n" : "")
                        + (!dbInfos.get("author_skype").equals("") ? "Skype: " + dbInfos.get("author_skype") + "\n" : "")
                        + (!dbInfos.get("other_contacts").equals("") ? "Други: " + dbInfos.get("other_contacts") : "")
        );
        infos.put("author_phone", dbInfos.get("author_phone"));
        infos.put("author_email", dbInfos.get("author_email"));
        infos.put("author_skype", dbInfos.get("author_skype"));
        infos.put("descr",
                (!dbInfos.get("description").equals("")
                        ? dbInfos.get("description")
                        : "няма")
        );
        dbInfos = null;
    }

    private void showLoadingStatus() {
        ImageView favsIcon = (ImageView) findViewById(R.id.favsIcon);
        favsIcon.setVisibility(View.GONE);
        TextView titleText = (TextView) findViewById(R.id.titleText);
        titleText.setText("Зареждане..");
        //ImageView image = (ImageView)findViewById(R.id.gameImage);
        //image. ...
        TextView dateText = (TextView) findViewById(R.id.dateText);
        dateText.setText("Зареждане..");
        TextView dateSeenText = (TextView) findViewById(R.id.dateSeenText);
        dateSeenText.setText("Зареждане..");
        TextView priceText = (TextView) findViewById(R.id.priceText);
        priceText.setText("Зареждане..");
        TextView ratingText = (TextView) findViewById(R.id.ratingText);
        ratingText.setText("Зареждане..");
        TextView contactsText = (TextView) findViewById(R.id.contactsText);
        contactsText.setText("Зареждане..");
        TextView descrText = (TextView) findViewById(R.id.descrText);
        descrText.setText("Зареждане..");
    }

    private void showImagePreview() {
        Intent intent = new Intent(GameDetails.this, GameImagePreview.class);
        Bundle bundle = new Bundle();
        bundle.putInt("gameID", Integer.parseInt(infos.get("game_id")));
        bundle.putString("url", infos.get("url"));
        intent.putExtras(bundle);
        GameDetails.this.startActivity(intent);
    }

    private void showAuthorGames() {
        Intent intent = new Intent(GameDetails.this, AuthorGames.class);
        Bundle bundle = new Bundle();
        bundle.putInt("author_id", Integer.parseInt(infos.get("author_id")));
        bundle.putString("author_name", infos.get("author_name"));
        intent.putExtras(bundle);
        GameDetails.this.startActivity(intent);
    }

    private void dialAuthor() {
        String phone = infos.get("author_phone");
        Pattern pattern = Pattern.compile("[^\\d\\+]*");
        Matcher matcher = pattern.matcher(phone);
        phone = matcher.replaceAll("");
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
        startActivity(intent);
    }

    private void emailAuthor() {
        String url = getString(R.string.url_game_page) + infos.get("game_id");
        Intent i = new Intent(Intent.ACTION_SEND);
        //i.setType("text/plain"); //use this line for testing in the emulator
        i.setType("message/rfc822"); // use from live device
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{infos.get("author_email")});
        i.putExtra(Intent.EXTRA_SUBJECT, "Относно Вашата обява в " + getString(R.string.url_site));
        i.putExtra(Intent.EXTRA_TEXT, "Здравейте!\n" +
                "Интересувам се от Вашата обява \"" + infos.get("title") + "\", намираща се тук:\n" +
                url + "\n");
        startActivity(Intent.createChooser(i, "Изберете email приложение"));
    }
	
	/*private void skypeAuthor(){
		Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("skype://" + infos.get("author_skype")));
	    startActivity(Intent.createChooser(intent, "Изберете skype приложение"));
	}*/

    private void showGameUrl() {
        String url = getString(R.string.url_game_page) + infos.get("game_id");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Линк към обявата");
        builder.setMessage("Можете да използвате този линк в уеб браузъра на Вашия компютър:\n\n" + url);
        AlertDialog alert = builder.create();
        alert.show();
    }

}
