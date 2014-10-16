# DOCKER-VERSION 1.0.1
FROM dockerfile/java:openjdk-7-jre
# Install Java
RUN \
  apt-get update && \
  apt-get install -y openjdk-7-jre && \
  rm -rf /var/lib/apt/lists/*
# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-7-openjdk-amd64
# Set working directory
WORKDIR /data
# Bundle bot binary and move from host to container
ADD src /data/src
CMD ["/data/src/start.sh"]