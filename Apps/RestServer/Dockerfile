FROM alpine:3.3
MAINTAINER Duke Nguyen <duke.nguyen@incapturetechnologies.com>
RUN apk --update add bash tar openjdk8-jre
COPY build/install/RestServer /opt/rapture/RestServer
COPY docker/config /opt/rapture/RestServer/etc/rapture/config
COPY docker/keystore.jks /opt/rapture/RestServer/bin
ENV RAPTURE_CONFIG_HOME /opt/rapture/RestServer/etc/rapture/config
WORKDIR /opt/rapture/RestServer/bin
ENTRYPOINT [ "/bin/bash", "/opt/rapture/RestServer/bin/RestServer" ]

