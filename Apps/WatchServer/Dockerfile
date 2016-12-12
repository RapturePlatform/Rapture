FROM gliderlabs/alpine:3.4
#see https://github.com/sillelien/base-alpine for DNS hacks below
MAINTAINER jonathan.major@incapturetechnologies.com

# About nsswitch.conf - see https://registry.hub.docker.com/u/frolvlad/alpine-oraclejdk8/dockerfile/
COPY docker/rootfs /

# S6 is a process manager (helps with DNS)
ADD https://github.com/just-containers/s6-overlay/releases/download/v1.13.0.0/s6-overlay-amd64.tar.gz /tmp/s6-overlay.tar.gz
RUN tar xvfz /tmp/s6-overlay.tar.gz -C / && \
  apk -U add dnsmasq jq curl && \
  chmod 755 /bin/*.sh /etc/services.d/dns/run /etc/services.d/dns-hack/run /etc/services.d/syslog/run && \
  mkdir /app && \
  echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' >> /etc/nsswitch.conf && \
  addgroup -g 998 app && \
  adduser -D  -G app -s /bin/false -u 998 app

# Install certificates. See https://github.com/denvazh/java/blob/master/openjdk7-jdk/Dockerfile for refrence
RUN apk --update upgrade
RUN apk-install bash openjdk8-jre ca-certificates && \
  find /usr/share/ca-certificates/mozilla/ -name *.crt -exec keytool -import -trustcacerts \
  -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts -storepass changeit -noprompt \
  -file {} -alias {} \; && \
  keytool -list -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts --storepass changeit


# Setup up required resources
COPY build/install/WatchServer /opt/rapture/WatchServer
# this contains config files for docker environment using link names
COPY docker/config /opt/rapture/WatchServer/etc/rapture/config

# Setup environment
ENV RAPTURE_CONFIG_HOME /opt/rapture/WatchServer/etc/rapture/config
VOLUME ["/opt/rapture/WatchServer/etc"]
WORKDIR /opt/rapture/WatchServer/bin

RUN mkdir /tmp/subfolder

ENTRYPOINT [ "/init" ]
CMD ["/opt/rapture/WatchServer/bin/WatchServer"]
