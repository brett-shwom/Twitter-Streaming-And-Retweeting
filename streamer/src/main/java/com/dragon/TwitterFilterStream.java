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
import java.lang.InterruptedException;


public class TwitterFilterStream
{


    public static void main( String[] args ) throws InterruptedException {
        final Jedis redisClient = getRedisClient();
        stream(args[0],args[1],args[2],args[3], message -> redisClient.lpush(message));

    }

    public static Jedis getRedisClient() {
        return new Jedis("localhost");
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
        List<String> terms = Lists.newArrayList("man");
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
