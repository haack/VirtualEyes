package org.haack.virtualeyes;

import org.opencv.core.Core;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;


public class Region {

	int id;
	boolean active;
	Point center; //store multiple?
	Point expected;
	MatOfPoint contour;
	double circumference;
	//area?
	
	public Region(int id, MatOfPoint contour, Point center) {
		this.id = id;
		this.center = center;
		this.contour = contour;
		
		//calculate circumference
    	for (int j = 0; j < contour.rows(); j++) {
    		Point vertex = new Point(contour.get(j, 0)[0], contour.get(j, 0)[1]);
    		Point nextVertex;
    		if (j == contour.rows() -1) {
        		nextVertex = new Point(contour.get(0, 0)[0], contour.get(0, 0)[1]);
    		} else {
        		nextVertex = new Point(contour.get(j+1, 0)[0], contour.get(j+1, 0)[1]);
    		}
    		double xDelta = Math.abs(vertex.x - nextVertex.x);
    		double yDelta = Math.abs(vertex.y - nextVertex.y);
    		circumference = Math.pow(xDelta + yDelta, 2);
        }

		active = true;
		System.out.println("Region added ID:" + id);
		System.out.println("Circumference:" + circumference);
	}
	
	public boolean isMatch(MatOfPoint test, Point testCenter) {
		if (Math.abs(center.x - testCenter.x) < 50) {
//			if (Math.abs(center.y - testCenter.y) < 50) {
				center = testCenter;
				contour = new MatOfPoint();
				test.copyTo(contour);
				rotatePoints();
				active = true;
	     		return true;	     		
//			}
     	}
		return false;
	}
	
	public double getCircumference() {
		return circumference;
	}
	
	public Rect getBoundingRect() {
		Point topleft = new Point(9999,9999); //doesnt work for super large files.
		Point bottomright = new Point(-1,-1);
		for (int j = 0; j < contour.rows(); j++) {
    		Point vertex = new Point(contour.get(j, 0)[0], contour.get(j, 0)[1]);
    		if (vertex.x < topleft.x) {
    			topleft.x = vertex.x;
    		} 
    		if (vertex.x > bottomright.x) {
    			bottomright.x = vertex.x;
    		} 
    		if (vertex.y < topleft.y) {
    			topleft.y = vertex.y;
    		} 
    		if (vertex.y > bottomright.y) {
    			bottomright.y = vertex.y;
    		} 
        }
		return new Rect(topleft, bottomright);
	}
	
	//rotates points in contour until topright is [0]
	public void rotatePoints() {
		System.out.println("Rotating");
		Point vertex = new Point(contour.get(0, 0)[0], contour.get(0, 0)[1]);
//		Point point = getBoundingRect().tl();
		Point point = new Point(0, 0);
		System.out.println("TL x: " + point.x);
		double distance = getDistanceFrom(point, vertex);
		System.out.println("Distance: " + distance);
		int smallestIndex = 0;
		for (int i = 3; i >= 1; i--) {
			System.out.println("i: " + i);
			if (distance > getDistanceFrom(point, new Point(contour.get(i, 0)[0], contour.get(i, 0)[1]))) {
				
				System.out.println("\n\nSwitcharoo\n");
				smallestIndex = i;
				distance = getDistanceFrom(new Point(contour.get(i, 0)[0], contour.get(i, 0)[1]), point);
				break;
			}
		}
		
		System.out.println("smallest index: " + smallestIndex);
		
		//rotate based on smallest index
		for (int i = 0; i < smallestIndex; i++) {
			double temp[] = contour.get(0, 0);
			contour.put(0, 0, contour.get(1, 0));
			contour.put(1, 0, contour.get(2, 0));
			contour.put(2, 0, contour.get(3, 0));
			contour.put(3,0, temp);
		}
	}
	
	public double getDistanceFrom(Point p, Point a) {
		//return Math.pow(Math.abs(p.x - a.x) + Math.abs(p.y - a.y), 2);
		return Math.sqrt(Math.pow(a.x - p.x, 2) + Math.pow(a.y - p.y, 2));
	}
}
