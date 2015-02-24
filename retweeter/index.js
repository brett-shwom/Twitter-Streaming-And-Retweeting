var Twit = require('twit');
var fs = require('fs');
var redis = require("redis");
var redisConfig = JSON.parse(fs.readFileSync('../redis.json', {encoding:'utf8'}));
var redisClient = redis.createClient(redisConfig.port,redisConfig.host,{});

if (!fs.existsSync('../secrets.json')) {
  console.log("create a secrets.json file in the root directory. it should look like this:\n");

  var s = "";
  s+="{\n";
  s+= '  "consumer_key" : "YOUR_CONSUMER_KEY_HERE",\n';
  s+= '  "consumer_secret" : "YOUR_CONSUMER_SECRET_HERE",\n';
  s+= '  "access_token" : "YOUR_ACCESS_TOKEN_HERE",\n';
  s+= '  "access_token_secret" : "YOUR_ACCESS_TOKEN_SECRET_HERE"\n';
  s+= '}'

 console.log(s);

 console.log('\n\nGet your keys at: https://apps.twitter.com');

 return;
}

var secrets = fs.readFileSync('../secrets.json', {encoding:'utf8'});
 
secrets = JSON.parse(secrets);

var twitterAPIClient = new Twit({
  consumer_key: secrets.consumer_key,
  consumer_secret: secrets.consumer_secret,
  access_token: secrets.access_token,
  access_token_secret: secrets.access_token_secret
});

redisClient.blpop(redisConfig.sharedQueueKey, 0, function (err, tweet) {
  console.log('tweet:', tweet);
  //retweet(tweet.id); //uncomment me!
});

function retweet(tweetId) {
  twitterAPIClient.post('statuses/retweet/:id', { id: tweetId }, function (err, data, response) {
    if (err) console.log(err);
    else console.log('retweet successful for id', tweetId);
  });
}



