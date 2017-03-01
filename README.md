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

## File Prefixes

The generated Turtle files will use namespace prefixes not defined in the file itself. To use them, it might be required to specify the following URI namespaces:

```
@prefix w: <http://w3id.org/prohowlinks#> .
@prefix oa: <http://www.w3.org/ns/oa#> .
@prefix prohow: <http://w3id.org/prohow#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix dbo: <http://dbpedia.org/ontology/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
```

These namespaces are defined in Turtle format, and they could be added at the beginning of any generated `.ttl` file to make it into a correct self-contained Turtle file.

# Instruction Extraction

### A Python Script to Exctract Subsets of the Data

The amount of data extracted by the crawler can be inconveniently large for many processing purposes. If the amount of data extracted is too large, the instruction extractor script allows the extraction of specific sets of instructions from the whole set of Turtle files
The `extractor` folder contains the `extract_specific_instruction_sets.py` python script that extracts the RDF representation of specific sets of instructions. This can be done in one of three ways, or a combination of those. Instructions that do not match at least one of the category-based filter or the specific instructions filter will not be selected.

## Language-Based Filter

It is possible to limit the extraction of instruction data to instructions of specific languages. This can be done by modifying the `list_of_allowed_languages` variable in the `extract_specific_instruction_sets.py` script. If this variable is an empty list, then all languages will be considered. Otherwise, the only languages that will be considered are those whose language tag is included in this list. For example, the extractor could be made to filter only English and Spanish instructions if it contains the language tags `"en"` and `"es"` (as strings).

NOTE: This filter takes precedence over all other filters. So instructions of languages other than the ones specified with this filter will not be extracted, even if they would match other filters.

## Category-Based Filter

It is possible to limit the extractions to instructions that fall under specific categories. This can be done by modifying the `list_of_allowed_languages` variable in the `extract_specific_instruction_sets.py` script. More specifically, the algorighm will select all the instructions which directly belong to one of the types defined in list_of_allowed_categories or any of their sub-classes. To consider sub-classes, they need to be defined as defined in RDFS in the Turtle file `class_hierarchy.ttl`. The types/categories need to be added as URL strings in the `list_of_allowed_languages` list. For example, to extract only breakfast food instructions in Spanish and English the following URL strings can be added to the list: `"http://www.wikihow.com/Category:Breakfast"` `"http://es.wikihow.com/Categor%C3%ADa:Desayunos"`.

## Specific Instructions Filter

It is possible to point the extractor to instructions associated with specific URLs. The instructions that will be extracted are the ones specified in the `extract_specific_sets_instructions.txt` file. This file should contain one URL per line, with no blank lines.

# Seed Extraction

The python script `seed-generator/extract_seeds.py` allows to extract a list of visited URLs from the results of a previous run.

The output of this script is a `seeds.txt` file. When the crawler is run, if it detects a `seeds.txt` file in the root folder where it is run it will add all the URLs find inside into the list of seed URLs. The file should only contain URLs, one per line, with no empty lines.
