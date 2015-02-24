package com.dragon;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.Hosts;
import redis.clients.jedis.Jedis;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonObject;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


import java.lang.InterruptedException;
import java.lang.NumberFormatException;



public class TwitterFilterStream
{


    public static void main( String[] args ) throws InterruptedException, NumberFormatException, IOException {
        String twitterSecretsPath = args[0];
        String redisConfigurationPath = args[1];

        JsonObject twitterSecrets = readJSONFile(twitterSecretsPath);
        JsonObject redisConfiguration = readJSONFile(redisConfigurationPath);


        String consumerKey = twitterSecrets.getString("consumer_key");
        String consumerSecret= twitterSecrets.getString("consumer_secret");
        String token = twitterSecrets.getString("access_token");
        String tokenSecret = twitterSecrets.getString("access_token_secret");

        String redisHost = redisConfiguration.getString("host");
        int redisPort = redisConfiguration.getInt("port");

        final String redisSharedQueueKey = redisConfiguration.getString("sharedQueueKey");

        final Jedis redisClient = getRedisClient(redisHost, redisPort);

        stream(consumerKey,consumerSecret,token,tokenSecret, message -> redisClient.lpush(redisSharedQueueKey, message));

    }

    public static JsonObject readJSONFile(String path) throws IOException {


        InputStream fileInputStream = new FileInputStream(path);
        JsonReader jsonReader = Json.createReader(fileInputStream);
        JsonObject jsonObject = jsonReader.readObject();

        jsonReader.close();
        fileInputStream.close();

        return jsonObject;
    }

    public static Jedis getRedisClient(String host, int port) {
        return new Jedis(host,port);
    }

    public static void stream(String consumerKey, String consumerSecret, String token, String tokenSecret, Consumer<String> messageReceivedHandler) throws InterruptedException{

        /** Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);
        //BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>(1000);

        /** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
        // Optional: set up some followings and track terms
        //List<Long> followings = Lists.newArrayList(1234L, 566788L);
        List<String> terms = Lists.newArrayList("#java", "#javascript", "#fun", "#love");
        //hosebirdEndpoint.followings(followings);
        hosebirdEndpoint.trackTerms(terms);

        // These secrets should be read from a config file
        Authentication hosebirdAuth = new OAuth1(consumerKey, consumerSecret, token, tokenSecret);
    

        ClientBuilder builder = new ClientBuilder()
            .name("Hosebird-Client-01")                              // optional: mainly for the logs
            .hosts(hosebirdHosts)
            .authentication(hosebirdAuth)
            .endpoint(hosebirdEndpoint)
            .processor(new StringDelimitedProcessor(msgQueue))
            //.eventMessageQueue(eventQueue);                          // optional: use this if you want to process client events
            ;

        Client hosebirdClient = builder.build();
        // Attempts to establish a connection.
        hosebirdClient.connect();

        new StringDelimitedProcessor(msgQueue);

        System.out.println("Waiting for message");

        while (!hosebirdClient.isDone()) {
          String msg = msgQueue.take();
          System.out.println(msg);
          messageReceivedHandler.accept(msg);
        }

    }

}
