/**
 * 
 */
package com.kaart.highwaynamemodification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Logging;

/**
 * @author Taylor Smock
 *
 */
public class GeometryCustom {
	public static OsmPrimitive getClosestPrimitive(OsmPrimitive osm, Collection<OsmPrimitive> primitives) {
		double lowestDistance = Double.MAX_VALUE;
		OsmPrimitive closest = null;
		for (OsmPrimitive primitive : primitives) {
			if (primitive.getUniqueId() == 17001999 || osm.getUniqueId() == 489706962) {
				Logging.info("We have the id 17001999 ({0}) and 489706962 ({1})", primitive.getUniqueId() == 17001999, osm.getUniqueId() == 489706962);
			}
			double distance = getDistance(osm, primitive);
			if (distance < lowestDistance) {
				lowestDistance = distance;
				closest = primitive;
			}
		}
		return closest;
	}
	
	public static double getDistance(OsmPrimitive one, OsmPrimitive two) {
		double rValue = Double.MAX_VALUE;
		if (one instanceof Node && two instanceof Node) {
			rValue = ((Node) one).getCoor().greatCircleDistance(((Node) two).getCoor());
		} else if (one instanceof Node && two instanceof Way) {
			rValue = getDistanceWayPoint((Way) two, (Node) one);
		} else if (one instanceof Way && two instanceof Node) {
			rValue = getDistanceWayPoint((Way) one, (Node) two);
		} else if (one instanceof Way && two instanceof Way) {
			rValue = getDistanceWayWay((Way) one, (Way) two);
		} else if (one instanceof Relation && !(two instanceof Relation)) {
			for (OsmPrimitive osmPrimitive: ((Relation) one).getMemberPrimitives()) {
				double currentDistance = getDistance(osmPrimitive, two);
				if (currentDistance < rValue) rValue = currentDistance;
			}
		} else if (!(one instanceof Relation) && two instanceof Relation) {
			for (OsmPrimitive osmPrimitive : ((Relation) two).getMemberPrimitives()) {
				double currentDistance = getDistance(osmPrimitive, one);
				if (currentDistance < rValue) rValue = currentDistance;
			}
		}
		return rValue;
	}
	
	public static double getDistanceWayPoint(Way way, Node node) {
		double rValue = Double.MAX_VALUE;
		if (way.getNodesCount() < 2) return rValue;
		List<WaySegment> segments = getWaySegments(way);
		for (WaySegment segment : segments) {
			EastNorth point = Geometry.closestPointToSegment(segment.getFirstNode().getEastNorth(), segment.getSecondNode().getEastNorth(), node.getEastNorth());
			double distance = point.distance(node.getEastNorth());
			if (distance < rValue) rValue = distance;
		}
		return rValue;
	}
	
	public static WaySegment getClosestWaySegment(Way way, OsmPrimitive primitive) {
		List<WaySegment> segments = getWaySegments(way);
		double lowestDistance = Double.MAX_VALUE;
		WaySegment closest = null;
		for (WaySegment segment : segments) {
			double distance = getDistance(segment.toWay(), primitive);
			if (distance < lowestDistance) {
				lowestDistance = distance;
				closest = segment;
			}
		}
		return closest;
	}
	
	public static double getDistanceWayWay(Way one, Way two) {
		double rValue = Double.MAX_VALUE;
		List<WaySegment> oneSegments = getWaySegments(one);
		List<WaySegment> twoSegments = getWaySegments(two);
		for (WaySegment oneSegment : oneSegments) {
			for (WaySegment twoSegment : twoSegments) {
				EastNorth intersection = Geometry.getSegmentSegmentIntersection(
						oneSegment.getFirstNode().getEastNorth(),
						oneSegment.getSecondNode().getEastNorth(),
						twoSegment.getFirstNode().getEastNorth(),
						twoSegment.getSecondNode().getEastNorth());
				if (intersection != null) return 0.0;
				double distance = getDistanceSegmentSegment(oneSegment, twoSegment);
				if (distance < rValue) rValue = distance;
			}
		}
		return rValue;
	}
	
	public static double getDistanceSegmentSegment(WaySegment one, WaySegment two) {
		EastNorth vectorOne = one.getSecondNode().getEastNorth().subtract(one.getFirstNode().getEastNorth());
		EastNorth vectorTwo = two.getSecondNode().getEastNorth().subtract(two.getFirstNode().getEastNorth());
		EastNorth vectorThree = one.getFirstNode().getEastNorth().subtract(two.getFirstNode().getEastNorth());
		double SMALL_NUMBER = 0.00000000001;
		double a = dot(vectorOne, vectorOne);
		double b = dot(vectorOne, vectorTwo);
		double c = dot(vectorTwo, vectorTwo);
		double d = dot(vectorOne, vectorThree);
		double e = dot(vectorTwo, vectorThree);
		
		double D = a * c - b * b;
		double sc, sN, sD = d;
		double tc, tN, tD = D;
		if (D < SMALL_NUMBER) {
			sN = 0.0;
			sD = 1.0;
			tN = e;
			tD = c;
		} else {
			sN = (b * e - c * d);
			tN = (a * e - b * d);
			if (sN < 0.0) {
				sN = 0.0;
				tN = e;
				tD = c;
			} else if (sN > sD) {
				sN = sD;
				tN = e + b;
				tD = c;
			}
		}
		
		if (tN < 0.0) {
			tN = 0.0;
			if (-d < 0.0) sN = 0.0;
			else if (-d > a) sN = sD;
			else {
				sN = -d;
				sD = a;
			}
		} else if (tN > tD) {
			tN = tD;
			if ((-d + b) < 0.0) sN = 0;
			else if ((-d + b) > a) sN = sD;
			else {
				sN = (-d + b);
				sD = a;
			}
		}
		sc = Math.abs(sN) < SMALL_NUMBER ? 0.0 : sN / sD;
		tc = Math.abs(tN) < SMALL_NUMBER ? 0.0 : tN / tD;
		EastNorth p1 = one.getFirstNode().getEastNorth().interpolate(one.getSecondNode().getEastNorth(), sc);
		EastNorth p2 = two.getFirstNode().getEastNorth().interpolate(two.getSecondNode().getEastNorth(), tc);
		return p1.distance(p2);
	}
	
	public static double dot(EastNorth one, EastNorth two) {
		return two.getX() * one.getX() + one.getY() * two.getY();
	}
	
	public static List<WaySegment> getWaySegments(Way way) {
		List<WaySegment> segments = new ArrayList<>();
		int i = 0;
		do {
			segments.add(new WaySegment(way, i));
			i++;
		} while (i < way.getNodesCount() - 2);
		return segments;
	}
}
