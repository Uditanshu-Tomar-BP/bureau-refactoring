FROM maven:3-openjdk-8 AS java-build
LABEL name="ravi ranjan"

WORKDIR /app
COPY pom.xml .
COPY settings.xml .
RUN mvn clean dependency:go-offline
COPY . .
RUN mvn clean -s settings.xml deploy
ENTRYPOINT ["tail", "-f", "/dev/null"]
