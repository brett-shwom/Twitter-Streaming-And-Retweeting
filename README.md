Stream and retweet
------------------

## Structure

There are 2 projects, one for reading streaming data from twitter, the other for retweeting.

## Installation

```
install.sh
```

Also make sure you have a redis instance running somehwere.

## Architecture

The streamer writes to a redis list. The retweeter reads from the list and retweets.

## Requirements
- Stick your keys in a file called `secrets.json` in the root directory. The file should look like this:
```
{
 "consumer_key" : "YOUR_CONSUMER_KEY",
 "consumer_secret" : "YOUR_CONSUMER_SECRET",
 "access_token" : "YOUR_ACCESS_TOKEN",
 "access_token_secret" : "YOUR_ACCESS_TOKEN_SECRET"
}
```

You can get keys from [http://apps.twitter.com](http://apps.twitter.com)

- Specify the loation of your redis instance in `redis.json`
- Run redis

## Why did I mix node and Java

Because I am rediculous. 

I tried using a node package, but was having trouble getting the stream to work. The Java library is maintained by twitter so I figured it had to be decent.

