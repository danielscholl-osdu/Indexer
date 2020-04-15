FROM amazoncorretto:8

WORKDIR /
COPY provider/indexer-aws/target/indexer-aws-0.0.1-SNAPSHOT-spring-boot.jar indexer-aws-0.0.1-SNAPSHOT-spring-boot.jar
EXPOSE 8080

CMD ["java","-jar", "indexer-aws-0.0.1-SNAPSHOT-spring-boot.jar"]