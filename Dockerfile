FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY src ./src

RUN mkdir -p out && \
    javac --add-modules jdk.httpserver -d out src/main/java/*.java

CMD ["java", "--add-modules", "jdk.httpserver", "-cp", "out", "ARPWebServer"]