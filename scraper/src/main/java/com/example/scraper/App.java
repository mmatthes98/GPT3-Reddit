package com.example.scraper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.net.URI;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;


public class App {
    public static void main(String[] args) throws IOException, InterruptedException {
        if (System.getenv("RDS_HOSTNAME") != null) {
            try {
                Class.forName("org.postgresql.Driver");
                String dbName = System.getenv("RDS_DB_NAME");
                String userName = System.getenv("RDS_USERNAME");
                String password = System.getenv("RDS_PASSWORD");
                String hostname = System.getenv("RDS_HOSTNAME");
                String port = System.getenv("RDS_PORT");
                String jdbcUrl = "jdbc:postgresql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password;
                Connection con = DriverManager.getConnection(jdbcUrl);

                scrape(con);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    //This will look at the top 225 posts on /r/AskReddit and upload the top 2 comments from each post to my postgreSQL server
    private static void scrape(Connection con) throws IOException, InterruptedException, SQLException {
        ArrayList<String> titles = new ArrayList<>();
        //client
        titles.add("");
        var client = HttpClient.newHttpClient();
        //request
        for (int i = 0; i < 9; i += 1) {
            String url = String.format("https://www.reddit.com/r/%s/top/.json?sort=top&t=year&after=t3_%s", "AskReddit", titles.get(titles.size() - 1));
            getTitles(titles, url, client, con);
        }

    }

    private static void getTitles(ArrayList<String> titles, String url, HttpClient client, Connection con) throws IOException, InterruptedException, SQLException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        var response = client.send(request, BodyHandlers.ofString());
        JsonObject posts = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray arr = (JsonArray) posts.getAsJsonObject("data").get("children");
        for (int i = 0; i < 25; i++) {
            String title = arr.get(i).getAsJsonObject().getAsJsonObject("data").get("id").getAsString();
            getBody(title, client, titles, con);
        }
    }

    private static void getBody(String title, HttpClient client, ArrayList<String> titles, Connection con) throws IOException, InterruptedException, SQLException {
        String url = String.format("https://www.reddit.com/r/%s/comments/%s/.json?sort=top", "AskReddit", title);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        var response = client.send(request, BodyHandlers.ofString());
        var posts = JsonParser.parseString(response.body()).getAsJsonArray();
        JsonObject titleObject = posts.get(0).getAsJsonObject();
        JsonObject commentObject = posts.get(1).getAsJsonObject();
        String prompt = titleObject.getAsJsonObject("data").getAsJsonArray("children").get(0).getAsJsonObject().getAsJsonObject("data").get("title").getAsString();
        titles.set(0, title);
        for(int i = 0; i < 2; i++){
            String comment = commentObject.getAsJsonObject("data").getAsJsonArray("children").get(i).getAsJsonObject().getAsJsonObject("data").get("body").getAsString();
            Statement stmt = con.createStatement();

            //Must escape ' character for SQL query
            comment = comment.replaceAll("\'", "\'\'");
            prompt = prompt.replaceAll("\'", "\'\'");
            String query = String.format("INSERT INTO toppost VALUES (\'%s\', \'%s\')", prompt, comment);
            stmt.executeUpdate(query);
        }
    }
}
