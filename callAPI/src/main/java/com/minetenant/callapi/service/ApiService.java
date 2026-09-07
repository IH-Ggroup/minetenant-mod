package com.minetenant.callapi.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class ApiService {

    private static final HttpClient HTTP_CLIENT=HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static CompletableFuture<String> fetchApiDataAsync(String url){
        return CompletableFuture.supplyAsync(()->{
            try {
                HttpRequest request=HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<String> response=HTTP_CLIENT.send(request,HttpResponse.BodyHandlers.ofString());
                if(response.statusCode()==200){
                    return response.body();
                }else{
                    throw new RuntimeException("Failed to fetch data from API. Status code: "+response.statusCode());
                }
            } catch (Exception e) {
                throw new RuntimeException("Error occurred while fetching API data.", e);
            }
        });
    }
}

