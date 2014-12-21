package edu.sjsu.cmpe.cache.client;
import java.util.ArrayList;
import java.lang.*;

public class Client {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Cache Client...");
        ArrayList <String> elements = new ArrayList<String>();
        elements.add("http://localhost:3000");
        elements.add("http://localhost:3001");
        elements.add("http://localhost:3002");
        CacheServiceInterface cache = new DistributedCacheService(elements);
                
        
        cache.put(1, "a");
        System.out.println("Step 1; put(1 => a)");

        Thread.sleep(30000);
        cache.put(1, "b");
        System.out.println("Step 2; put(1 => b)");

        Thread.sleep(30000);
        String value = cache.get(1);
        System.out.println("Step 3; get(1) => " + value);

        
        System.out.println("Existing Cache Client...");
    }

}
