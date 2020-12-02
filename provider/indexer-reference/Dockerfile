FROM openjdk:8-slim
WORKDIR /app
COPY target/indexer-reference-0.0.1-SNAPSHOT-spring-boot.jar indexer-reference.jar
# Run the web service on container startup.
CMD java -Djava.security.egd=file:/dev/./urandom -Dserver.port=8080 -jar /app/indexer-reference.jar
