package com.github.nyukhalov.polylabel;

import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.geojson.Polygon;
import com.mapbox.services.commons.models.Position;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class Polylabel {

    public static Point polylabel(Polygon polygon, double precision) {

        // find the bounding box of the outer ring
        double minX = 0, minY = 0, maxX = 0, maxY = 0;
        List<Position> outerRingCoordinates = polygon.getCoordinates().get(0);
        for (int i = 0; i < outerRingCoordinates.size(); i++) {
            Position p = outerRingCoordinates.get(i);
            double lon = p.getLongitude();
            double lat = p.getLatitude();

            if (i == 0 || lon < minX) minX = lon;
            if (i == 0 || lat < minY) minY = lat;
            if (i == 0 || lon > maxX) maxX = lon;
            if (i == 0 || lat > maxY) maxY = lat;
        }

        double width = maxX - minX;
        double height = maxY - minY;
        double cellSize = Math.min(width, height);
        double h = cellSize / 2;

        // a priority queue of cells in order of their "potential" (max distance to polygon)
        PriorityQueue<Cell> cellQueue = new PriorityQueue<>(new CellComparator());

        if (cellSize == 0) return Point.fromCoordinates(new double[] {minX, minY});

        // cover polygon with initial cells
        for (double x = minX; x < maxX; x += cellSize) {
            for (double y = minY; y < maxY; y += cellSize) {
                cellQueue.add(new Cell(x + h, y + h, h, polygon));
            }
        }

        // take centroid as the first best guess
        Cell bestCell = getCentroidCell(polygon);

        // special case for rectangular polygons
        Cell bboxCell = new Cell(minX + width / 2, minY + height / 2, 0, polygon);
        if (bboxCell.d > bestCell.d) bestCell = bboxCell;

        while (!cellQueue.isEmpty()) {
            // pick the most promising cell from the queue
            Cell cell = cellQueue.poll();

            // update the best cell if we found a better one
            if (cell.d > bestCell.d) {
                bestCell = cell;
            }

            // do not drill down further if there's no chance of a better solution
            if (cell.max - bestCell.d <= precision) continue;

            // split the cell into four cells
            h = cell.h / 2;
            cellQueue.add(new Cell(cell.x - h, cell.y - h, h, polygon));
            cellQueue.add(new Cell(cell.x + h, cell.y - h, h, polygon));
            cellQueue.add(new Cell(cell.x - h, cell.y + h, h, polygon));
            cellQueue.add(new Cell(cell.x + h, cell.y + h, h, polygon));
        }

        return Point.fromCoordinates(new double[]{bestCell.x, bestCell.y});
    }

    // get polygon centroid
    private static Cell getCentroidCell(Polygon polygon) {
        double area = 0;
        double x = 0;
        double y = 0;
        List<Position> points = polygon.getCoordinates().get(0);

        for (int i = 0, len = points.size(), j = len - 1; i < len; j = i++) {
            Position a = points.get(i);
            Position b = points.get(j);
            double aLon = a.getLongitude();
            double aLat = a.getLatitude();
            double bLon = b.getLongitude();
            double bLat = b.getLatitude();

            double f = aLon * bLat - bLon * aLat;
            x += (aLon + bLon) * f;
            y += (aLat + bLat) * f;
            area += f * 3;
        }
        if (area == 0) return new Cell(points.get(0).getLongitude(), points.get(0).getLatitude(), 0, polygon);
        return new Cell(x / area, y / area, 0, polygon);
    }

    private static class CellComparator implements Comparator<Cell> {
        @Override
        public int compare(Cell o1, Cell o2) {
            return Double.compare(o2.max, o1.max);
        }
    }

    private static class Cell {
        // cell center x
        private final double x;

        // cell center y
        private final double y;

        // half the cell size
        private final double h;

        // distance from cell center to polygon
        private final double d;

        // max distance to polygon within a cell
        private final double max;

        private Cell(double x, double y, double h, Polygon polygon) {
            this.x = x;
            this.y = y;
            this.h = h;
            this.d = pointToPolygonDist(x, y, polygon);
            this.max = this.d + this.h * Math.sqrt(2);
        }

        // signed distance from point to polygon outline (negative if point is outside)
        private double pointToPolygonDist(double x, double y, Polygon polygon) {
            boolean inside = false;
            double minDistSq = Double.MAX_VALUE;

            List<List<Position>> coordinates = polygon.getCoordinates();
            for (int k = 0; k < coordinates.size(); k++) {
                List<Position> ring = coordinates.get(k);

                for (int i = 0, len = ring.size(), j = len - 1; i < len; j = i++) {
                    Position a = ring.get(i);
                    Position b = ring.get(j);
                    double aLon = a.getLongitude();
                    double aLat = a.getLatitude();
                    double bLon = b.getLongitude();
                    double bLat = b.getLatitude();

                    if ((aLat > y != bLat > y) &&
                            (x < (bLon - aLon) * (y - aLat) / (bLat - aLat) + aLon)) inside = !inside;

                    minDistSq = Math.min(minDistSq, getSegDistSq(x, y, a, b));
                }
            }

            return (inside ? 1 : -1) * Math.sqrt(minDistSq);
        }

        // get squared distance from a point to a segment
        private double getSegDistSq(double px, double py, Position a, Position b) {

            double x = a.getLongitude();
            double y = a.getLatitude();
            double dx = b.getLongitude() - x;
            double dy = b.getLatitude() - y;

            if (dx != 0 || dy != 0) {

                double t = ((px - x) * dx + (py - y) * dy) / (dx * dx + dy * dy);

                if (t > 1) {
                    x = b.getLongitude();
                    y = b.getLatitude();

                } else if (t > 0) {
                    x += dx * t;
                    y += dy * t;
                }
            }

            dx = px - x;
            dy = py - y;

            return dx * dx + dy * dy;
        }
    }
}
