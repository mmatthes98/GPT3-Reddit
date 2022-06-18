package com.example.scraper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;


public class App
{
    public static void main( String[] args ) throws IOException, InterruptedException
    {
        scrape();
    }

    private static void scrape() throws IOException, InterruptedException{
        ArrayList<String> titles= new ArrayList<>();
            //client
        titles.add("");
        var client = HttpClient.newHttpClient();
            //request
        for(int i = 0; i < 40; i += 1){
            String url = String.format("https://www.reddit.com/r/AskReddit/top/.json?sort=top&t=year&after=t3_%s", titles.get(titles.size() - 1));
            if(titles.size() == 1){
                titles.clear();
            }
            getTitles(titles, url, client);
        }
        System.out.println(titles);

    }

    private static void getTitles(ArrayList<String> titles, String url, HttpClient client) throws IOException, InterruptedException{
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        var response = client.send(request, BodyHandlers.ofString());
        JsonObject posts = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray arr = (JsonArray) posts.getAsJsonObject("data").get("children");
        for(int i = 0; i < 25; i++){
            titles.add(arr.get(i).getAsJsonObject().getAsJsonObject("data").get("id").getAsString());
        }
    }
}
