# DOCKER-VERSION 1.0.1
FROM dockerfile/java:oracle-java7
MAINTAINER CryptoAsssure "https://github.com/CryptoAssure"

# check for OS updates and install if needed.
RUN \
  sudo apt-get update && \
  sudo apt-get -y upgrade && \
  sudo apt-get install git -y

# set environment variables for container.
RUN sudo locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US.UTF-8
ENV LC_ALL en_US.UTF-8

# set working directory.
WORKDIR /data

# define commonly used JAVA_HOME variable.
ENV JAVA_HOME /usr/lib/jvm/java-7-oracle

# bundle bot binary and move from host to container.
ADD src /data/src

# add configuration file.
ADD options.json /data/src/options.json

# create a local 'services' directory and 
# symlink the bot scripts.
#RUN \
#  mkdir -p /data/services && \
#  ln -s /data/src/services/* /data/services/ && \
#  cd /data/services/ && \
#  sudo chmod +x start.sh stop.sh restart.sh

#ENTRYPOINT ["/data/services/start.sh"]