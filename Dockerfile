FROM adoptopenjdk:11-jre-hotspot
COPY ./target/*.jar app.jar
COPY ./certs/test.jks /certs/test.jks
COPY ./context/context.json /context/context.json
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
