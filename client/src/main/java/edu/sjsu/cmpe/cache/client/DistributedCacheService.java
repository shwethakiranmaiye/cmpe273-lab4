package edu.sjsu.cmpe.cache.client;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
//import com.mashape.unirest.http.async;
import com.mashape.unirest.http.async.Callback;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.HashMap;
import java.util.Map;

import java.io.InputStream;
import java.util.ArrayList;
//  import java.util.concurrent;
//import com.mashape.unirest.http.*;
/**
 * Distributed cache service
 * 
 */
public class DistributedCacheService implements CacheServiceInterface {
    //private final String cacheServerUrl;
    private ArrayList<String> cacheServerUrls;

    public DistributedCacheService(ArrayList<String> ll) {
        this.cacheServerUrls = ll;
    }

    /**
     * @see edu.sjsu.cmpe.cache.client.CacheServiceInterface#get(long)
     */
    @Override
    public String get(long key) {
        final CountDownLatch responseWaiter = new CountDownLatch(3);
        final HashMap<String, String> responses = new HashMap<String, String>(); 
        for (final String cacheServerUrl: this.cacheServerUrls) {
            Future<HttpResponse<JsonNode>> response = null;
            response = Unirest
                    .get(cacheServerUrl + "/cache/{key}")
                    .header("accept", "application/json")
                    .routeParam("key", Long.toString(key))
                    .asJsonAsync(new Callback<JsonNode>() {

                        public void failed(UnirestException e) {
                            System.out.println("The Get request has failed");
                            responseWaiter.countDown();
                        }

                        public void completed(HttpResponse<JsonNode> response) {
                            int code = response.getStatus();
                            String value = response.getBody().getObject().getString("value");
                            responses.put(cacheServerUrl, value);
                            responseWaiter.countDown();
                            System.out.println( " get value"+ value );
                        }

                        public void cancelled() {
                            System.out.println("The request has been cancelled");
                            responseWaiter.countDown();
                        }
            });
        }// end of for
        HashMap <String, Integer> responseValueCount = new HashMap<String, Integer>();
        try {
            responseWaiter.await();
            // Now, check the responses of the two received responses
            
            for (Map.Entry<String, String> entry: responses.entrySet()) {
                if (responseValueCount.containsKey(entry.getValue())) {
                    responseValueCount.put(entry.getValue(), responseValueCount.get(entry.getValue())+1);
                } else {
                    responseValueCount.put(entry.getValue(),1);
                }
            }
        } catch(InterruptedException  e) {

        }
        // Now, check response values and make consistent if some inconsitency is observed.
        String value = "";
        Boolean needtoupdate = false;
        // Check if all the responses are different; In that case, delete everything.
        if (responseValueCount.size() == 3) {

        }
        // Get the response winner.
        for (Map.Entry<String, Integer> entry: responseValueCount.entrySet()) {
            if(entry.getValue()  ==3) {
                value = entry.getKey();
            } else if (entry.getValue() == 2) {
                value = entry.getKey();
                needtoupdate = true;
            } 
        }

        // update the inconsistent state
        for (Map.Entry<String, String> entry: responses.entrySet()) {
            if (entry.getValue() != value) {
                try {
                    Unirest
                        .put(entry.getKey() + "/cache/{key}/{value}")
                        .header("accept", "application/json")
                        .routeParam("key", Long.toString(key))
                        .routeParam("value", value).asJson();
                } catch (UnirestException e) {
                    System.err.println(e);
                }
            }
        }
        return value;
    }

    /**
     * @see edu.sjsu.cmpe.cache.client.CacheServiceInterface#put(long,
     *      java.lang.String)
     */
    /*@Override
    public void put(long key, String value) {
        HttpResponse<JsonNode> response = null;
        try {
            response = Unirest
                    .put(this.cacheServerUrl + "/cache/{key}/{value}")
                    .header("accept", "application/json")
                    .routeParam("key", Long.toString(key))
                    .routeParam("value", value).asJson();
        } catch (UnirestException e) {
            System.err.println(e);
        }

        if (response.getCode() != 200) {
            System.out.println("Failed to add to the cache.");
        }
    }*/

    @Override
    public Boolean put(long key, final String value) {
        final CountDownLatch responseWaiter = new CountDownLatch(3);
        final HashMap<String, Integer> responses = new HashMap<String, Integer>(); 
        for (final String cacheServerUrl: this.cacheServerUrls) {
            Future<HttpResponse<JsonNode>> response = null;
            response = Unirest
                    .put(cacheServerUrl + "/cache/{key}/{value}")
                    .header("accept", "application/json")
                    .routeParam("key", Long.toString(key))
                    .routeParam("value", value)
                    .asJsonAsync(new Callback<JsonNode>() {

                        public void failed(UnirestException e) {
                            System.out.println("The request has failed");
                            responseWaiter.countDown();
                        }

                        public void completed(HttpResponse<JsonNode> response) {
                            int code = response.getStatus();
                            responses.put(cacheServerUrl, response.getStatus());
                            responseWaiter.countDown();
                            System.out.println( " put getStatus "+code + " value" + value );
                          //  System.out.println("get code" +response.getCode() );
                            //Map<String, String> headers = response.getHeaders();
                            JsonNode body = response.getBody();
                            InputStream rawBody = response.getRawBody();
                        }

                        public void cancelled() {
                            System.out.println("The request has been cancelled");
                            responseWaiter.countDown();
                        }
            });
        }// end of for
        try {
            responseWaiter.await();
            // Now, check the responses of the two received responses
            int count = 0;
            for (Map.Entry<String, Integer> entry: responses.entrySet()) {
                Integer status = entry.getValue();
                if (status == 200) {
                    count += 1;
                }
                if (count == 2) {
                    return true;
                }
            }
        } catch(InterruptedException  e) {

        }
        // This means it insert is unsuccessful; revert and return false;
        for (Map.Entry<String, Integer> entry: responses.entrySet()) {
            Integer status = entry.getValue();
            if (status == 200) {
                try {
                Unirest.delete(entry.getKey() + "/cache/{key}")
                    .header("accept", "application/json")
                    .routeParam("key", Long.toString(key))
                    .asJson();
                }catch (UnirestException e) {
                    System.err.println(e);
                }
            }
        }
        return false;
    }
//DELETE METHID _SHWETHa
@Override
    public void delete(long key) {
        HttpResponse<JsonNode> response = null;
        try {
            response = Unirest.delete(/*this.cacheServerUrl + */"/cache/{key}")
                    .header("accept", "application/json")
                    .routeParam("key", Long.toString(key))
                    .asJson();
        } catch (UnirestException e) {
            System.err.println(e);
       }
       int code = response.getStatus();
       System.out.println( "delete getStatus"+code );
       System.out.println("---delete function--");

    }


}
