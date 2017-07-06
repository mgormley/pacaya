# Pacaya [![Build Status](https://travis-ci.org/mgormley/pacaya.svg?branch=master)](https://travis-ci.org/mgormley/pacaya)

## Summary

Pacaya is a library for joint modeling with graphical models,
structured factors, neural networks, and hypergraphs. Structured
factors allow us to encode structural constraints such as for
dependency parsing, constituency parsing, etc. Neural factors are 
just factors where the scores are computed by a neural network. 

This library has been used extensively for NLP. Check out 
[Pacaya NLP](https://github.com/mgormley/pacaya-nlp) for applications of 
this library to dependency parsing, relation extraction, semantic role labeling, 
and more.

A software tutorial is in the works. Check back soon for an update!

Contributions to this library come from Matt Gormley, Meg Mitchell, and Travis Wolfe.

## Using the Library

The latest version is deployed on 
[Maven Central](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22edu.jhu.pacaya%22%20AND%20a%3A%22pacaya%22):

```xml
<dependency>
    <groupId>edu.jhu.pacaya</groupId>
    <artifactId>pacaya</artifactId>
    <version>3.1.5</version>
</dependency>
```

## Setup

### Dependencies

This project has several dependencies all of which are available on Maven Central.
Among others we make extensive use:

* [Prim](https://github.com/mgormley/prim): a Java primitives library
* [Optimize](https://github.com/mgormley/optimize): a numerical optimization library

### Build:

* Compile the code from the command line:

        mvn compile

* To build a single jar with all the dependencies included:

        mvn compile assembly:single

### Eclipse setup:

* Create local versions of the .project and .classpath files for Eclipse:

        mvn eclipse:eclipse

* Add M2\_REPO environment variable to
  Eclipse. http://maven.apache.org/guides/mini/guide-ide-eclipse.html
  Open the Preferences and navigate to 'Java --> Build Path -->
  Classpath Variables'. Add a new classpath variable M2\_REPO with the
  path to your local repository (e.g. ~/.m2/repository).

* To make the project Git aware, right click on the project and select Team -> Git...

## Citing

If you use this library, please cite the thesis below:

```bibtek
@thesis{gormley_graphical_2015,
        location = {Baltimore, {MD}},
        title = {Graphical Models with Structured Factors, Neural Factors, and Approximation-Aware Training},
        institution = {Johns Hopkins University},
        type = {phdthesis},
        author = {Gormley, Matthew R.},
        date = {2015}
}
```
