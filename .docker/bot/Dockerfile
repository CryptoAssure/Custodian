# DOCKER-VERSION 1.0.1
FROM dockerfile/java:openjdk-7-jre
MAINTAINER CryptoAsssure "https://github.com/CryptoAssure"

# check for OS updates and install if needed.
RUN \
  sudo apt-get update && \
  sudo apt-get -y upgrade && \
  rm -rf /var/lib/apt/lists/*

# set environment variables for container.
RUN sudo locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US.UTF-8
ENV LC_ALL en_US.UTF-8

# set working directory.
WORKDIR /data

# install java.  
RUN \
  sudo apt-get install -y openjdk-7-jre && \
  rm -rf /var/lib/apt/lists/*

# define commonly used JAVA_HOME variable.
ENV JAVA_HOME /usr/lib/jvm/java-7-openjdk-amd64

# bundle bot binary and move from host to container.
ADD src /data/src

# add configuration file.
ADD options.json /data/src/options.json

# create a local 'services' directory and 
# symlink the bot scripts.
RUN \
  mkdir -p /data/services && \
  ln -s /data/src/services/* /data/services/ && \
  cd /data/services/ && \
  sudo chmod +x start.sh stop.sh restart.sh

# DEV
RUN mkdir /data/services/logs/
RUN cd /data/services/logs/ && sudo chmod 777 /data/services/logs/ && echo "This is inside of the /data/services/logs directory" > index.html

#ENTRYPOINT ["/data/services/start.sh"]

#CMD ["/data/services/logs/", "/www"]