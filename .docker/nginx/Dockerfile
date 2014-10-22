FROM ubuntu:14.04
MAINTAINER CryptoAsssure "https://github.com/CryptoAssure"
RUN apt-get update -qq && apt-get -y install net-tools nginx

# Remove the default Nginx configuration file
RUN rm -v /etc/nginx/nginx.conf

# Copy a configuration file from the current directory
ADD nginx.conf /etc/nginx/

# Append "daemon off;" to the beginning of the configuration
RUN echo "daemon off;" >> /etc/nginx/nginx.conf

RUN mkdir /etc/nginx/ssl
ADD default /etc/nginx/sites-available/default
ADD default-ssl /etc/nginx/sites-available/default-ssl

EXPOSE 80

VOLUME /www

CMD ["nginx"]