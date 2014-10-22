package com.like2program.ps3igri;

import java.util.HashMap;

public class ChangedStatusGlobal {

    private static ChangedStatusGlobal instance = null;
    //
    private HashMap<String, HashMap<String, String>> favs = new HashMap<String, HashMap<String, String>>();
    private HashMap<String, HashMap<String, String>> stats = new HashMap<String, HashMap<String, String>>();
    private boolean favListUpdAllowed = false;//TODO: this may not be needed anymore
    private boolean adapterNotified = false;
    private boolean cacheCleared = false;

    private ChangedStatusGlobal() {
        //prevent instantiation
    }

    public static ChangedStatusGlobal getInstance() {
        //get the same instance every time
        if (instance == null) {
            instance = new ChangedStatusGlobal();
        }
        return instance;
    }

    //if list update is needed
    public boolean favoriteChanged() {
        return favs.size() > 0;
    }

    //if list update is needed
    public boolean gameStatChanged() {
        return stats.size() > 0;
    }

    public void setFavChanged(int feedID, int newStat) {
        HashMap<String, String> fav = new HashMap<String, String>();
        String feedIDStr = Integer.toString(feedID);
        fav.put("feed_id", feedIDStr);
        fav.put("favStat", Integer.toString(newStat));
        fav.put("mainListUpdated", "no");
        fav.put("favsListUpdated", (favListUpdAllowed ? "no" : "yes"));
        fav.put("searchListUpdated", "no");
        fav.put("authorGamesListUpdated", "no");
        fav.put("gameDetailsUpdated", "no");
        favs.put(feedIDStr, fav);
    }

    public void setGameStatChanged(int feedID) {
        //Log.i("ChangedStatusGlobal", "setGameStatChanged(): feedID = " + feedID);
        HashMap<String, String> stat = new HashMap<String, String>();
        String feedIDStr = Integer.toString(feedID);
        stat.put("feed_id", feedIDStr);
        stat.put("mainListUpdated", "no");
        stat.put("favsListUpdated", (favListUpdAllowed ? "no" : "yes"));
        stat.put("searchListUpdated", "no");
        stat.put("authorGamesListUpdated", "no");
        stats.put(feedIDStr, stat);
    }

    public void setFavUpdated(int feedID, String type) {
        String feedIDStr = Integer.toString(feedID);
        if (type.equals("mainList")) {
            favs.get(feedIDStr).put("mainListUpdated", "yes");
            //clear the others here too
            favs.get(feedIDStr).put("searchListUpdated", "yes");
            favs.get(feedIDStr).put("favsListUpdated", "yes");
            favs.get(feedIDStr).put("authorGamesListUpdated", "yes");
            favs.get(feedIDStr).put("gameDetailsUpdated", "yes");
            //
        } else if (type.equals("searchList")) {
            favs.get(feedIDStr).put("searchListUpdated", "yes");
            //clear the others here too
            favs.get(feedIDStr).put("favsListUpdated", "yes");
            favs.get(feedIDStr).put("authorGamesListUpdated", "yes");
            favs.get(feedIDStr).put("gameDetailsUpdated", "yes");
            //
        } else if (type.equals("favsList")) {
            favs.get(feedIDStr).put("favsListUpdated", "yes");
            //clear the others here too
            favs.get(feedIDStr).put("searchListUpdated", "yes");
            favs.get(feedIDStr).put("authorGamesListUpdated", "yes");
            favs.get(feedIDStr).put("gameDetailsUpdated", "yes");
            //
        } else if (type.equals("authorGamesList")) {
            favs.get(feedIDStr).put("authorGamesListUpdated", "yes");
        } else if (type.equals("gameDetails")) {
            favs.get(feedIDStr).put("gameDetailsUpdated", "yes");
        }
        if (
                favs.get(feedIDStr).get("mainListUpdated").equals("yes")
                        && favs.get(feedIDStr).get("favsListUpdated").equals("yes")
                        && favs.get(feedIDStr).get("searchListUpdated").equals("yes")
                        && favs.get(feedIDStr).get("authorGamesListUpdated").equals("yes")
                        && favs.get(feedIDStr).get("gameDetailsUpdated").equals("yes")
                ) {
            favs.remove(feedIDStr);
        }
    }

    //TODO: recode this copy/paste
    public void setGameStatUpdated(int feedID, String type) {
        String feedIDStr = Integer.toString(feedID);
        if (type.equals("mainList")) {
            stats.get(feedIDStr).put("mainListUpdated", "yes");
            //clear the others here too
            stats.get(feedIDStr).put("searchListUpdated", "yes");
            stats.get(feedIDStr).put("favsListUpdated", "yes");
            stats.get(feedIDStr).put("authorGamesListUpdated", "yes");
            //
        } else if (type.equals("searchList")) {
            stats.get(feedIDStr).put("searchListUpdated", "yes");
            //clear the others here too
            stats.get(feedIDStr).put("favsListUpdated", "yes");
            stats.get(feedIDStr).put("authorGamesListUpdated", "yes");
            //
        } else if (type.equals("favsList")) {
            stats.get(feedIDStr).put("favsListUpdated", "yes");
            //clear the others here too
            stats.get(feedIDStr).put("searchListUpdated", "yes");
            stats.get(feedIDStr).put("authorGamesListUpdated", "yes");
            //
        } else if (type.equals("authorGamesList")) {
            stats.get(feedIDStr).put("authorGamesListUpdated", "yes");
        }
        if (
                stats.get(feedIDStr).get("mainListUpdated").equals("yes")
                        && stats.get(feedIDStr).get("favsListUpdated").equals("yes")
                        && stats.get(feedIDStr).get("searchListUpdated").equals("yes")
                        && stats.get(feedIDStr).get("authorGamesListUpdated").equals("yes")
                ) {
            stats.remove(feedIDStr);
        }
    }

    public HashMap<String, HashMap<String, String>> getFavs() {
        return favs;
    }

    public HashMap<String, HashMap<String, String>> getGameStats() {
        return stats;
    }

    public void setFavListUpdAllowed(boolean allow) {
        favListUpdAllowed = allow;
    }

    public boolean getAdapterNotified() {
        return adapterNotified;
    }

    public void setAdapterNotified(boolean status) {
        adapterNotified = status;
    }

    public boolean getCacheCleared() {
        return cacheCleared;
    }

    public void setCacheCleared() {
        cacheCleared = true;
    }

    public void resetAll() {
        favs.clear();
        stats.clear();
        favListUpdAllowed = false;//TODO: this may not be needed anymore
        adapterNotified = false;
        cacheCleared = false;
    }

}
