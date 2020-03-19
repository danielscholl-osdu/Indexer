FROM amazoncorretto:8

ARG JAR_VERSION
ENV JAR_FILE=indexer-aws-${JAR_VERSION}-spring-boot.jar

WORKDIR /
COPY provider/indexer-aws/target/$JAR_FILE $JAR_FILE
EXPOSE 8080

CMD java -jar $JAR_FILE
