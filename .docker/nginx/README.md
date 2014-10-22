SSL
---

To use SSL, put your certs in `/etc/nginx/ssl` and enable the `default-ssl` site:

    ADD server.crt /etc/nginx/ssl/
    ADD server.key /etc/nginx/ssl/
    RUN ln -s /etc/nginx/sites-available/default-ssl /etc/nginx/sites-enabled/default-ssl


