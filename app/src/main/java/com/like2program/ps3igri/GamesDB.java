package com.like2program.ps3igri;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class GamesDB {

    private static final String DB_NAME = "GamesCatalog";
    private static final int DB_VER = 1;
    private static final String FEEDS_TABLE = "feeds";
    private static final String AUTHORS_TABLE = "authors";
    private static final String FEEDS_TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + FEEDS_TABLE
                    + " (feed_id integer primary key autoincrement, "
                    + " game_id integer UNIQUE, "
                    + " author_id integer, "
                    + "cat text not null default '', "
                    + "title text not null, "
                    + "description text not null default '', "
                    + "price integer default 0, "
                    + "pubdate datetime, "
                    + "fav integer default 0, "
                    + "favdate datetime, "
                    + "url text not null default '', "
                    + "seen datetime, "
                    + "status text not null default ''"
                    + "); "
                    + "CREATE INDEX IF NOT EXISTS seenIDX ON " + FEEDS_TABLE + " (seen);"
                    + "CREATE INDEX IF NOT EXISTS statusIDX ON " + FEEDS_TABLE + " (status);"
                    + "CREATE INDEX IF NOT EXISTS favIDX ON " + FEEDS_TABLE + " (fav);"
                    + "CREATE INDEX IF NOT EXISTS favdateIDX ON " + FEEDS_TABLE + " (favdate);";
    private static final String AUTHORS_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + AUTHORS_TABLE
            + "(id integer primary key autoincrement, "
            + "author_id integer UNIQUE, "
            + "author_name text, "
            + "author_email text, "
            + "author_phone text, "
            + "author_skype text, "
            + "author_city text, "
            + "other_contacts text, "
            + "rating text"
            + ");"
            + "CREATE INDEX IF NOT EXISTS authorName ON " + AUTHORS_TABLE + " (author_name)";
    private static final int maxGamesOnList = 50;
    private SQLiteDatabase db;

    public GamesDB(Context context) {//TODO: create a destructor for this where the db will be closed
        try {
            db = context.openOrCreateDatabase(DB_NAME, android.content.Context.MODE_PRIVATE, null);
            if (db.getVersion() != DB_VER) {
                Log.i("GamesDB", "database version upgraded! old: " + db.getVersion()
                        + ", new: " + DB_VER);
                db.setVersion(DB_VER);
                db.execSQL("DROP TABLE IF EXISTS " + FEEDS_TABLE);
                db.execSQL("DROP TABLE IF EXISTS " + AUTHORS_TABLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "DB open err: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        //TODO: create/recreate the DB on difference (db migration)
        //db.execSQL("DROP TABLE IF EXISTS " + FEEDS_TABLE);
        //db.execSQL("DROP TABLE IF EXISTS " + AUTHORS_TABLE);
        db.execSQL(FEEDS_TABLE_CREATE);
        db.execSQL(AUTHORS_TABLE_CREATE);
    }

    public void closeDB() {
        db.close();
    }

    //TODO: set minimum fields for the list and max for reverse
    public ArrayList<GamesListItem> getLastGames(int max, String cat_filter) {
        ArrayList<GamesListItem> games = new ArrayList<GamesListItem>();
        String fields = "cat, title, description, price, "
                + "pubdate, rating, url, seen, status, fav, "
                + "feed_id, game_id, author_name, author_city, "
                + "author_email, author_phone, author_skype, "
                + "other_contacts, " + AUTHORS_TABLE + ".author_id";
        String whereNewChangedString = "(status = 'new' OR status = 'changed')";
        String whereOtherString = "status = ''";
        if (!cat_filter.equals("")) {
            if (cat_filter.equals("sell") || cat_filter.equals("exchange")) {
                whereNewChangedString += " AND (cat = '" + cat_filter + "' OR cat = 'sell_exchange')";
                whereOtherString += " AND (cat = '" + cat_filter + "' OR cat = 'sell_exchange')";
            } else {
                whereNewChangedString += " AND cat = '" + cat_filter + "'";
                whereOtherString += " AND cat = '" + cat_filter + "'";
            }
        }

        //new and changed games
        String queryNewChanged = "SELECT " + fields
                + " FROM " + FEEDS_TABLE
                + " INNER JOIN " + AUTHORS_TABLE
                + " ON " + AUTHORS_TABLE + ".author_id = "
                + FEEDS_TABLE + ".author_id"
                + " WHERE " + whereNewChangedString
                + " ORDER BY seen DESC, feed_id DESC";
        Cursor c = db.rawQuery(queryNewChanged, null);
        int numRows = c.getCount();
        c.moveToFirst();
        for (int i = 0; i < numRows; i++) {
            GamesListItem item = new GamesListItem();
            item.setCat(c.getString(0));
            item.setTitle(c.getString(1));
            item.setDescr(c.getString(2));
            item.setPrice(c.getInt(3));
            item.setPubDate(c.getString(4));
            item.setRating(c.getString(5));
            item.setUrl(c.getString(6));
            item.setSeen(c.getString(7));
            item.setStatus(c.getString(8));
            item.setFav(c.getInt(9));
            item.setFeedId(c.getInt(10));
            item.setGameId(c.getInt(11));
            item.setAuthorName(c.getString(12));
            item.setAuthorCity(c.getString(13));
            item.setAuthorEmail(c.getString(14));
            item.setAuthorPhone(c.getString(15));
            item.setAuthorSkype(c.getString(16));
            item.setOtherContacts(c.getString(17));
            item.setAuthorId(c.getInt(18));
            games.add(item);
            c.moveToNext();
        }
        c.close();
        //other games if needed
        int otherGamesNeeded = (max > 0 ? max : maxGamesOnList) - numRows;
        if (max == 0 || otherGamesNeeded > 0) {
            String queryOther = "SELECT " + fields
                    + " FROM " + FEEDS_TABLE
                    + " INNER JOIN " + AUTHORS_TABLE
                    + " ON " + AUTHORS_TABLE + ".author_id = "
                    + FEEDS_TABLE + ".author_id"
                    + " WHERE " + whereOtherString
                    + " ORDER BY seen DESC, feed_id DESC"
                    + (max == 0 ? "" : " LIMIT " + Integer.toString(otherGamesNeeded));
            c = db.rawQuery(queryOther, null);
            numRows = c.getCount();
            c.moveToFirst();
            for (int i = 0; i < numRows; i++) {
                GamesListItem item = new GamesListItem();
                item.setCat(c.getString(0));
                item.setTitle(c.getString(1));
                item.setDescr(c.getString(2));
                item.setPrice(c.getInt(3));
                item.setPubDate(c.getString(4));
                item.setRating(c.getString(5));
                item.setUrl(c.getString(6));
                item.setSeen(c.getString(7));
                item.setStatus(c.getString(8));
                item.setFav(c.getInt(9));
                item.setFeedId(c.getInt(10));
                item.setGameId(c.getInt(11));
                item.setAuthorName(c.getString(12));
                item.setAuthorCity(c.getString(13));
                item.setAuthorEmail(c.getString(14));
                item.setAuthorPhone(c.getString(15));
                item.setAuthorSkype(c.getString(16));
                item.setOtherContacts(c.getString(17));
                item.setAuthorId(c.getInt(18));
                games.add(item);
                c.moveToNext();
            }
            c.close();
        }
        return games;
    }

    public ArrayList<GamesListItem> searchGames(String search) {
        ArrayList<GamesListItem> games;
        Cursor c = db.rawQuery("SELECT feed_id, cat, title, price, seen, status, fav, game_id, author_name, author_city " +
                "FROM " + FEEDS_TABLE +
                " INNER JOIN " + AUTHORS_TABLE + " ON " + AUTHORS_TABLE + ".author_id = " + FEEDS_TABLE + ".author_id " +
                (!search.equals("") ? "WHERE title LIKE ? OR description LIKE ? " : "") +
                "ORDER BY seen DESC, feed_id DESC", (!search.equals("") ? new String[]{"%" + search + "%", "%" + search + "%"} : null));
        games = cursor2GamesArr(c);
        c.close();
        return games;
    }

    public ArrayList<GamesListItem> favGames(int max) {
        ArrayList<GamesListItem> games;
        Cursor c = db.rawQuery("SELECT feed_id, cat, title, price, seen, status, fav, game_id, author_name, author_city " +
                "FROM " + FEEDS_TABLE +
                " INNER JOIN " + AUTHORS_TABLE + " ON " + AUTHORS_TABLE + ".author_id = " + FEEDS_TABLE + ".author_id " +
                "WHERE fav = 1 " +
                "ORDER BY favdate DESC, seen DESC, feed_id DESC " +
                (max > 0 ? "LIMIT " + Integer.toString(max) : ""), null);
        games = cursor2GamesArr(c);
        c.close();
        return games;
    }

    public boolean setFav(int feedID, int favStatus) {
        ContentValues values = new ContentValues();
        values.put("fav", favStatus);
        values.put("favdate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));//current datetime, TODO: make it inside sql
        return (db.update(FEEDS_TABLE, values, "feed_id = ?", new String[]{Integer.toString(feedID)}) > 0);
    }

    private ArrayList<GamesListItem> cursor2GamesArr(Cursor c) {
        ArrayList<GamesListItem> games = new ArrayList<GamesListItem>();
        int numRows = c.getCount();
        if (numRows > 0) {
            c.moveToFirst();
            for (int i = 0; i < numRows; i++) {
                GamesListItem item = new GamesListItem();
                item.setFeedId(c.getInt(0));
                item.setCat(c.getString(1));
                item.setTitle(c.getString(2));
                item.setPrice(c.getInt(3));
                item.setSeen(c.getString(4));
                item.setStatus(c.getString(5));
                item.setFav(c.getInt(6));
                item.setGameId(c.getInt(7));
                item.setAuthorName(c.getString(8));
                item.setAuthorCity(c.getString(9));
                games.add(item);
                c.moveToNext();
            }
        }
        return games;
    }

    public boolean gameExists(int gameID) {
        Cursor c = db.query(FEEDS_TABLE, new String[]{"feed_id"}, "game_id = ?",
                new String[]{Integer.toString(gameID)}, null, null, null);
        int count = c.getCount();
        c.close();
        return (count > 0);
    }

    public boolean insertGame(GamesListItem item) {
        ContentValues values = new ContentValues();
        values.put("game_id", item.getGameId());
        values.put("cat", item.getCat());
        values.put("title", item.getTitle());
        values.put("description", item.getDescr());
        values.put("price", item.getPrice());
        values.put("pubdate", item.getPubDate());
        values.put("url", item.getUrl());
        values.put("seen", item.getSeen());
        values.put("status", item.getStatus());
        values.put("author_id", item.getAuthorId());
        boolean insFeedsRes = db.insert(FEEDS_TABLE, null, values) > 0;
        boolean insAuthorsRes = addAuthor(item);
        return (insFeedsRes && insAuthorsRes);
    }

    public boolean updateGame(GamesListItem item) {
        ContentValues values = new ContentValues();
        values.put("cat", item.getCat());
        values.put("title", item.getTitle());
        values.put("description", item.getDescr());
        values.put("price", item.getPrice());
        values.put("pubdate", item.getPubDate());
        values.put("url", item.getUrl());
        values.put("seen", item.getSeen());
        values.put("status", item.getStatus());
        values.put("author_id", item.getAuthorId());
        boolean updFeedsRes = db.update(FEEDS_TABLE, values, "game_id = ?",
                new String[]{Integer.toString(item.getGameId())}) > 0;

        values.clear();
        values.put("author_name", item.getAuthorName());
        values.put("author_email", item.getAuthorEmail());
        values.put("author_phone", item.getAuthorPhone());
        values.put("author_skype", item.getAuthorSkype());
        values.put("author_city", item.getAuthorCity());
        values.put("other_contacts", item.getOtherContacts());
        values.put("rating", item.getRating());
        boolean updAuthorsRes = db.update(AUTHORS_TABLE, values, "author_id = ?",
                new String[]{Integer.toString(item.getAuthorId())}) > 0;
        return (updFeedsRes || updAuthorsRes);
    }

    public String gameStatus(GamesListItem newGame) {
        Cursor c = db.rawQuery("SELECT cat, title, description, price, pubdate, rating, seen, status, "
                + "author_name, author_city, author_phone, author_skype, author_email, other_contacts"
                + " FROM " + FEEDS_TABLE
                + " INNER JOIN " + AUTHORS_TABLE
                + " ON " + AUTHORS_TABLE + ".author_id = "
                + FEEDS_TABLE + ".author_id"
                + " WHERE game_id = " + newGame.getGameId()
                + " LIMIT 1"
                , null);
        String status = "";
        if (c.getCount() == 0) status = "new";
        else {
            c.moveToFirst();
            if (c.getString(7).equals("new")) {
                status = "new";
            } else if (
                    !newGame.getCat().equals(c.getString(0))
                            || !newGame.getTitle().equals(c.getString(1))
                            || !newGame.getDescr().equals(c.getString(2))
                            || newGame.getPrice() != c.getInt(3)
                            || !newGame.getPubDate().equals(c.getString(4))
                            || !newGame.getRating().equals(c.getString(5))
                            || !newGame.getSeen().equals(c.getString(6))
                            //status = res field 7
                            || !newGame.getAuthorName().equals(c.getString(8))
                            || !newGame.getAuthorCity().equals(c.getString(9))
                            || !newGame.getAuthorPhone().equals(c.getString(10))
                            || !newGame.getAuthorSkype().equals(c.getString(11))
                            || !newGame.getAuthorEmail().equals(c.getString(12))
                            || !newGame.getOtherContacts().equals(c.getString(13))
                    ) {
                status = "changed";
            }
        }
        c.close();
        return status;//exists and not changed
    }

    public boolean clearDB() {
        return (
                db.delete(FEEDS_TABLE, null, null) > 0
                        || db.delete(AUTHORS_TABLE, null, null) > 0
        );
    }

    public HashMap<String, String> getGameInfos(int feedID) {
        HashMap<String, String> infos = new HashMap<String, String>();
        Cursor c = db.rawQuery("SELECT "
                + "title, description, pubdate, price, "
                + "rating, cat, seen, fav, status, url, "
                + "game_id, author_name, author_city, author_email, "
                + "author_phone, author_skype, other_contacts, " + AUTHORS_TABLE + ".author_id "
                + " FROM " + FEEDS_TABLE
                + " INNER JOIN " + AUTHORS_TABLE
                + " ON " + AUTHORS_TABLE + ".author_id = "
                + FEEDS_TABLE + ".author_id"
                + " WHERE feed_id = " + Integer.toString(feedID)
                + " LIMIT 1"
                , null);
        if (c.getCount() == 0) return infos;
        c.moveToFirst();
        infos.put("title", c.getString(0));
        infos.put("description", c.getString(1));
        infos.put("pubdate", c.getString(2));
        infos.put("price", c.getString(3));
        infos.put("rating", c.getString(4));
        infos.put("cat", c.getString(5));
        infos.put("seen", c.getString(6));
        infos.put("fav", c.getString(7));
        infos.put("status", c.getString(8));
        infos.put("url", c.getString(9));
        infos.put("game_id", c.getString(10));
        infos.put("author_name", c.getString(11));
        infos.put("author_city", c.getString(12));
        infos.put("author_email", c.getString(13));
        infos.put("author_phone", c.getString(14));
        infos.put("author_skype", c.getString(15));
        infos.put("other_contacts", c.getString(16));
        infos.put("author_id", c.getString(17));
        c.close();
        return infos;
    }

    public void reverseResults() {//use reverse array walk in the inserts instead
        //Log.i("GamesDB", "reverseResults() called");
        ArrayList<GamesListItem> original;
        original = getLastGames(0, "");
        clearDB();
        for (GamesListItem anOriginal : original) {
            insertGame(anOriginal);
        }
    }

    public boolean updateGameStatus(int feedID, String status) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        return (db.update(FEEDS_TABLE, values, "feed_id = ?", new String[]{Integer.toString(feedID)}) > 0);
    }

    public void setAllRead() {
        ContentValues values = new ContentValues();
        values.put("status", "");
        db.update(FEEDS_TABLE, values, "status <> ?", new String[]{""});
    }

    public int getFeedId(int gameID) {
        Cursor c = db.query(FEEDS_TABLE, new String[]{"feed_id"}, "game_id = ?",
                new String[]{Integer.toString(gameID)},
                null, null, null, "1");
        int result;
        if (c.getCount() == 0) result = -1;
        else {
            c.moveToFirst();
            result = c.getInt(0);
        }
        c.close();
        return result;
    }

    public int getGamesCount() {
        Cursor c = db.query(FEEDS_TABLE, new String[]{"COUNT(*)"}, null,
                null, null, null, null);
        int result = 0;
        if (c.getCount() > 0) {
            c.moveToFirst();
            result = c.getInt(0);
        }
        c.close();
        return result;
    }

    public void startTransaction() {
        db.beginTransaction();
    }

    public void finishTransaction() {
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public boolean deleteOldRecords(long records2Leave) {
        //TODO: make the rawquery into normal db.select()
        Cursor c = db.rawQuery("SELECT feed_id FROM "
                + FEEDS_TABLE + " ORDER BY feed_id DESC LIMIT "
                + Long.toString(records2Leave), null);
        c.moveToLast();
        String max_id = c.getString(0);
        c.close();
        return db.delete(FEEDS_TABLE, "feed_id < ?", new String[]{max_id}) > 0;
    }

    private boolean addAuthor(GamesListItem item) {
        Cursor c = db.query(AUTHORS_TABLE, new String[]{"author_id"},
                "author_id = ?", new String[]{Integer.toString(item.getAuthorId())}, null, null, null, "1");
        int rowsCount = c.getCount();
        c.close();
        if (rowsCount == 0) {
            ContentValues values = new ContentValues();
            values.put("author_id", item.getAuthorId());
            values.put("author_name", item.getAuthorName());
            values.put("author_email", item.getAuthorEmail());
            values.put("author_phone", item.getAuthorPhone());
            values.put("author_skype", item.getAuthorSkype());
            values.put("author_city", item.getAuthorCity());
            values.put("other_contacts", item.getOtherContacts());
            values.put("rating", item.getRating());
            return db.insert(AUTHORS_TABLE, null, values) > 0;
        }
        return true;
    }

    public ArrayList<GamesListItem> authorGames(int author_id) {
        ArrayList<GamesListItem> games;
        Cursor c = db.rawQuery("SELECT feed_id, cat, title, price, seen, status, fav, game_id, " +
                "author_name, author_city " +
                "FROM " + FEEDS_TABLE +
                " INNER JOIN " + AUTHORS_TABLE + " ON " + AUTHORS_TABLE + ".author_id = " +
                FEEDS_TABLE + ".author_id " +
                "WHERE " + FEEDS_TABLE + ".author_id = ? " +
                "ORDER BY seen DESC, feed_id DESC", new String[]{Integer.toString(author_id)});
        games = cursor2GamesArr(c);
        c.close();
        return games;
    }


}
