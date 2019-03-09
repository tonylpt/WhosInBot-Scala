FROM hseeberger/scala-sbt:8u181_2.12.8_1.2.8 as builder
WORKDIR /app

# cache project dependencies
COPY build.sbt .
COPY project project
RUN sbt update

COPY . .
RUN sbt 'set test in assembly := {}' clean assembly \
    && cp -f target/scala-2.12/WhosInBot-Scala.jar .


FROM openjdk:8-stretch
MAINTAINER lpthanh@gmail.com

ENV TELEGRAM_TOKEN=
ENV JDBC_DATABASE_URL=
ENV JAVA_OPTS=
ENV SENTRY_DSN=

WORKDIR /app

RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

COPY bin/docker/ .
COPY --from=builder /app/WhosInBot-Scala.jar .

CMD ["./start.sh"]
