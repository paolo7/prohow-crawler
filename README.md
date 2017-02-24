# PROHOW Crawler Implementation

This is a simple implementation of a crawler that crawls a website to extract procedural knowledge according to the [PROHOW](https://w3id.org/prohow/) vocabulary and data model.

## Important Note - This is not a Generic Implementation

This code has rules which are specific to the [wikiHow](wikihow.com) website as it was in February 2017. It is likely that this code will not work for different websites, or that it will not work for wikiHow in the future. 

To adapt this crawler for a different website, the configuration file and the HTMLparserWikiHow.java class will most likely need to be modified.

## Example Tutorial

The precompiled jar file allows to crawl the [wikiHow](wikihow.com) website across multiple languages.

To run it, open a console in the `example` folder and run the jar file with the following command:
```
java -jar know-how_collector.jar
```

The crawler will now crawl the website according to the configuration file in the `example/instruction` folder. It will generate a folder containing the frontier of links explore and those yet to explore, and another file with all the results of the crawl.

WARNING: On a large website like wikiHow, this code is likely to run for a very long period of time, even multiple days.
