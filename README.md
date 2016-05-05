# Summary

Pacaya is a library for joint modeling with graphical models,
structured factors, neural networks, and hypergraphs. Structured
factors allow us to encode structural constraints such as for
dependency parsing, constituency parsing, etc.

This release includes code for (Gormley, Mitchell, Van Durme, and Dredze, ACL 2014). 
Please cite our paper if you use this library.

```
@InProceedings{gormley-etal:2014:SRL,
  author    = {Gormley, Matthew R. and Mitchell, Margaret and {Van Durme}, Benjamin and Dredze, Mark},
  title     = {Low-Resource Semantic Role Labeling},
  booktitle = {Proceedings of {ACL}},
  month     = {June},
  year      = {2014},
}
```

A software tutorial is in the works and we'll make it available online with the official release of this library. 
Check back soon for an update!

Contributions to this library come from Matt Gormley, Meg Mitchell, and Travis Wolfe.

# Using the Library

The latest version is deployed on Maven Central:

    <dependency>
      <groupId>edu.jhu.pacaya</groupId>
      <artifactId>pacaya</artifactId>
      <version>2.0.1</version>
    </dependency>

# Setup

## Dependencies

This project has several dependencies all of which are available on Maven Central.
Among others we make extensive use:
* Prim: a Java primitives library
  https://github.com/mgormley/prim
* Optimize: a Java optimization library
  https://github.com/minyans/optimize

## Build:

* Compile the code from the command line:

    mvn compile

* To build a single jar with all the dependencies included:

    mvn compile assembly:single

## Eclipse setup:

* Create local versions of the .project and .classpath files for Eclipse:

    mvn eclipse:eclipse

* Add M2_REPO environment variable to
  Eclipse. http://maven.apache.org/guides/mini/guide-ide-eclipse.html
  Open the Preferences and navigate to 'Java --> Build Path -->
  Classpath Variables'. Add a new classpath variable M2_REPO with the
  path to your local repository (e.g. ~/.m2/repository).

* To make the project Git aware, right click on the project and select Team -> Git... 
