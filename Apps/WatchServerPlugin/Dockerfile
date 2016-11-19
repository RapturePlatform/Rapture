
FROM alpine:latest
MAINTAINER Jonathan Major <jonathan.major@incapturetechnologies.com>
RUN apk --update add bash tar openjdk8-jre
RUN apk add --install bash && rm -rf /var/cache/apk/*

ENV HOST=curtis
ENV PASSWORD=rapture

COPY build/install/WatchServerPlugin /opt/rapture/WatchServerPlugin
WORKDIR /opt/rapture/WatchServerPlugin/bin
COPY docker/feat.sh /tmp/feat.sh
ENTRYPOINT [ "/tmp/feat.sh" ]
