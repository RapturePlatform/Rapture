
FROM alpine:latest
MAINTAINER Jonathan Major <jonathan.major@incapturetechnologies.com>
RUN apk --update add bash tar openjdk8-jre
COPY build/install/WatchServerPlugin /opt/rapture/WatchServerPlugin
WORKDIR /opt/rapture/WatchServerPlugin/bin
COPY docker/feat.sh /tmp/feat.sh
ENTRYPOINT [ "/tmp/feat.sh" ]
