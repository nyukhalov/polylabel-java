package com.github.nyukhalov.polylabel;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.geojson.Polygon;
import com.mapbox.services.commons.geojson.custom.PositionDeserializer;
import com.mapbox.services.commons.models.Position;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PolylabelTest {
    private final Polygon water1;
    private final Polygon water2;

    public PolylabelTest() {
        water1 = fromResource("fixtures/water1.json");
        water2 = fromResource("fixtures/water2.json");
    }

    // finds pole of inaccessibility for water1 and precision 1
    @Test
    public void test1() throws Exception {
        Point expectedPoint = Point.fromCoordinates(new double[] {3865.85009765625, 2124.87841796875});

        Point p = Polylabel.polylabel(water1, 1);

        Assert.assertEquals(expectedPoint.getCoordinates(), p.getCoordinates());
    }

    // finds pole of inaccessibility for water1 and precision 50
    @Test
    public void test2() throws Exception {
        Point expectedPoint = Point.fromCoordinates(new double[] {3854.296875, 2123.828125});

        Point p = Polylabel.polylabel(water1, 50);

        Assert.assertEquals(expectedPoint.getCoordinates(), p.getCoordinates());
    }

    // finds pole of inaccessibility for water2 and default precision 1
    @Test
    public void test3() throws Exception {
        Point expectedPoint = Point.fromCoordinates(new double[] {3263.5, 3263.5});

        Point p = Polylabel.polylabel(water2, 1);

        Assert.assertEquals(expectedPoint.getCoordinates(), p.getCoordinates());
    }

    // works on degenerate polygons
    @Test
    public void test4() throws Exception {
        Point expectedPoint = Point.fromCoordinates(new double[] {0, 0});
        Polygon polygon1 = fromJson("[[[0, 0], [1, 0], [2, 0], [0, 0]]]");
        Polygon polygon2 = fromJson("[[[0, 0], [1, 0], [1, 1], [1, 0], [0, 0]]]");

        Point p1 = Polylabel.polylabel(polygon1, 1);
        Point p2 = Polylabel.polylabel(polygon2, 1);

        Assert.assertEquals(expectedPoint.getCoordinates(), p1.getCoordinates());
        Assert.assertEquals(expectedPoint.getCoordinates(), p2.getCoordinates());
    }

    private Polygon fromResource(String resourceName) {
        String json = getResource(resourceName);
        return fromJson(json);
    }

    private String getResource(String resourceName) {
        return new Scanner(
                ClassLoader.getSystemResourceAsStream(resourceName),
                "UTF-8")
                .useDelimiter("\\A")
                .next();
    }

    private Polygon fromJson(String json) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Position.class, new PositionDeserializer());
        Type listType = new TypeToken<ArrayList<ArrayList<Position>>>(){}.getType();

        List<List<Position>> coordinates = gsonBuilder.create().fromJson(json, listType);

        return Polygon.fromCoordinates(coordinates);
    }
}
