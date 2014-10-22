package com.like2program.ps3igri;

public class GamesListItem {

    private int feed_id = -1;
    private int game_id = -1;
    private String cat = "";
    private String title = "";
    private String descr = "";
    private int price = 0;
    private String pubDate = "";
    private String rating = "";
    private int fav = 0;
    private String url = "";
    private String seen = "";
    private String status = "";
    //author infos
    private String author_name = "";
    private String author_email = "";
    private String author_phone = "";
    private String author_skype = "";
    private String author_city = "";
    private String other_contacts = "";
    private int author_id = -1;
    //

    public int getGameId() {
        return game_id;
    }

    public void setGameId(int id) {
        game_id = id;
    }

    public int getFeedId() {
        return feed_id;
    }

    public void setFeedId(int fid) {
        feed_id = fid;
    }

    public String getCat() {
        return cat;
    }

    public void setCat(String c) {
        cat = c;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String t) {
        title = t;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String d) {
        descr = d;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int p) {
        price = p;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pd) {
        pubDate = pd;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String r) {
        rating = r;
    }

    public int getFav() {
        return fav;
    }

    public void setFav(int f) {
        fav = f;
    }

    public String getUrl() {
        return url;
    }
    //


    //getters

    public void setUrl(String u) {
        url = u;
    }

    public String getSeen() {
        return seen;
    }

    public void setSeen(String s) {
        seen = s;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String s) {
        status = s;
    }

    //author infos
    public String getAuthorName() {
        return author_name;
    }

    //author infos
    public void setAuthorName(String an) {
        author_name = an;
    }

    public String getAuthorEmail() {
        return author_email;
    }

    public void setAuthorEmail(String ae) {
        author_email = ae;
    }

    public String getAuthorPhone() {
        return author_phone;
    }

    public void setAuthorPhone(String ap) {
        author_phone = ap;
    }

    public String getAuthorSkype() {
        return author_skype;
    }

    public void setAuthorSkype(String as) {
        author_skype = as;
    }

    public String getAuthorCity() {
        return author_city;
    }

    public void setAuthorCity(String ac) {
        author_city = ac;
    }

    public String getOtherContacts() {
        return other_contacts;
    }

    public void setOtherContacts(String oc) {
        other_contacts = oc;
    }

    public int getAuthorId() {
        return author_id;
    }

    public void setAuthorId(int aid) {
        author_id = aid;
    }
    //

}
