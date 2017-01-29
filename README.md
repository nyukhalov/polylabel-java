[![Build Status](https://travis-ci.org/nyukhalov/polylabel-java.svg?branch=master)](https://travis-ci.org/nyukhalov/polylabel-java) [![Coverage Status](https://coveralls.io/repos/github/nyukhalov/polylabel-java/badge.svg?branch=master)](https://coveralls.io/github/nyukhalov/polylabel-java?branch=master)

Polylabel-java 
==============

A Java port of the [mapbox polylabel](https://github.com/mapbox/polylabel): a fast algorithm for finding the pole of inaccessibility of a polygon

Usage
=====

```Java
public class Example {
    public static void main() {
        Polygon polygon = readPolygon(); // Get polygon data from somewhere.
        Point p = Polylabel.polylabel(polygon, 1);
    }
}
```