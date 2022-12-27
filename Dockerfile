FROM openjdk:17-ea-11-jdk-slim
VOLUME /tmp
COPY boot/build/libs/boot-1.0.jar /app/demoapp.jar
RUN groupadd -g 999 appuser
RUN useradd -r -u 999 -g appuser appuser
USER appuser
ENTRYPOINT java -Xms256m -Xmx1024m -Dspring.profiles.active=prod -jar /app/demoapp.jar