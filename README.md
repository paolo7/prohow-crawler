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

# Seed Extraction

The python script `seed-generator/extract_seeds.py` allows to extract a list of visited URLs from the results of a previous run.

The output of this script is a `seeds.txt` file. When the crawler is run, if it detects a `seeds.txt` file in the root folder where it is run it will add all the URLs find inside into the list of seed URLs. The file should only contain URLs, one per line, with no empty lines.

# Instruction Extraction

The amount of data extracted by the crawler can be inconveniently large for many processing purposes. If the amount of data extracted is too large, the instruction extractor script allows the extraction of specific sets of instructions from the whole set of Turtle files. The `extractor` folder contains the `extract_specific_instruction_sets.py` python script that extracts the RDF representation of specific sets of instructions. The instructions that will be extracted are the ones specified in the `extract_specific_sets_instructions.txt` file. This file should contain one URL per line, with no blank lines. To make this process more efficient, the `list_of_allowed_languages` list in the python script can be modified to specify which language tags to consider when analysing the source files.
