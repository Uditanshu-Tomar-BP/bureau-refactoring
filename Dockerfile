FROM maven:3-openjdk-8 AS java-build
LABEL name="ravi ranjan"

WORKDIR /app 
COPY pom.xml .
RUN mvn clean dependency:go-offline
COPY . .
RUN mvn clean install deploy
ENTRYPOINT ["tail", "-f", "/dev/null"]
