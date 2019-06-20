Book Search
===========

This project was born out of frustration with available ebook search tools.
Powered by Elasticsearch, it aims to provide easy access to your entire library,
all wrapped up in a nice interface.

Features
--------
 * **Semi-automatic EPUB processing:** Right now, you manually have to map
   CSS classes from the ebook to a standardized set, ensuring a unified look
   across a variety of books.
 * **Fast, powerful search:** Using Elasticsearch as a backend, we can achieve
   blazing fast delivery of your search results. Every result is provided with
   context, so finding what you're looking for is guaranteed.
 * **Filters and grouping:** By categorizing books into several series and subseries,
   you may filter out books that would otherwise clutter your search results.
   If you want an overview of how results are distributed across books and chapters,
   grouping your search is only one click away.
 * **Built-in authentication:** Albeit very simple, there's built-in user management
   with a limited set of permissions. If you want to give your friends access to your
   library without them archiving their own books, just take away their permission
   to manage books.

Setup
-----
For ease of deployment, a ready-to-use [`docker-compose.yml`](docker-compose.yml)
file is included. Just run `docker-compose up` and access the application using your
favourite webbrowser at `localhost:4080`. On first start, a superuser `admin` with
password `booksearcher` (configurable via an environment variable) is created.
