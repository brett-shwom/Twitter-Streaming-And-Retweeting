cd streamer
mvn install && mvn exec:java -DtwitterSecretsPath="secrets.json" -DredisConfigurationPath="redis.json"