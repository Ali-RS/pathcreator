package com.jayfella.pathcreator.utility;

import com.jayfella.pathcreator.objects.MinMax3D;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// Collection of functions related to cubic bezier curves
/// (a curve with a start and end 'anchor' point, and two 'control' points to define the shape of the curve between the anchors)
public class CubicBezierUtility {

    /// Returns point at time 't' (between 0 and 1) along bezier curve defined by 4 points (anchor_1, control_1, control_2, anchor_2)
    public static Vector3f evaluateCurve (Vector3f[] points, float t) {
        return evaluateCurve (points[0], points[1], points[2], points[3], t);
    }

    /// Returns point at time 't' (between 0 and 1)  along bezier curve defined by 4 points (anchor_1, control_1, control_2, anchor_2)
    public static Vector3f evaluateCurve (Vector3f a1, Vector3f c1, Vector3f c2, Vector3f a2, float t) {
        t = FastMath.clamp(t, 0, 1);
        // return (1 - t) * (1 - t) * (1 - t) * a1 + 3 * (1 - t) * (1 - t) * t * c1 + 3 * (1 - t) * t * t * c2 + t * t * t * a2;

        // B rackets
        // O rder
        // D ivision
        // M ultiplication
        // A ddition
        // S ubtraction

        // (1 - t) * (1 - t) * (1 - t) * a1
        // + 3 * (1 - t) * (1 - t) * t * c1
        // + 3 * (1 - t) * t * t * c2
        // + t * t * t * a2;

        Vector3f a = new Vector3f(1 - t, 1 - t, 1 - t)
                .multLocal(1 - t)
                .multLocal(1 - t)
                .multLocal(a1);

        Vector3f b = new Vector3f(3, 3, 3)
                .multLocal(1 - t)
                .multLocal(1 - t)
                .multLocal(t)
                .multLocal(c1);

        Vector3f c = new Vector3f(3, 3, 3)
                .multLocal(1 - t)
                .multLocal(t)
                .multLocal(t)
                .multLocal(c2);

        Vector3f d = new Vector3f(t, t, t)
                .multLocal(t)
                .multLocal(t)
                .multLocal(a2);

        Vector3f result = a.add(b).addLocal(c).addLocal(d);
        return result;
    }

    /// Returns a vector tangent to the point at time 't'
    /// This is the vector tangent to the curve at that point
    public static Vector3f evaluateCurveDerivative (Vector3f[] points, float t) {
        return evaluateCurveDerivative (points[0], points[1], points[2], points[3], t);
    }

    /// Calculates the derivative of the curve at time 't'
    /// This is the vector tangent to the curve at that point
    public static Vector3f evaluateCurveDerivative (Vector3f a1, Vector3f c1, Vector3f c2, Vector3f a2, float t) {

        t = FastMath.clamp(t, 0, 1);

        // B rackets
        // O rder
        // D ivision
        // M ultiplication
        // A ddition
        // S ubtraction

        // return 3 * (1 - t) * (1 - t) * (c1 - a1) + 6 * (1 - t) * t * (c2 - c1) + 3 * t * t * (a2 - c2);

        // 3 * (1 - t) * (1 - t) * (c1 - a1)
        // + 6 * (1 - t) * t * (c2 - c1)
        // + 3 * t * t * (a2 - c2);

        Vector3f a = new Vector3f(3,3,3);
        Vector3f b = new Vector3f(6,6,6);
        Vector3f c = new Vector3f(3,3,3);

        a.multLocal(1 - t).multLocal(1 - t).multLocal(c1.subtract(a1));
        b.multLocal(1 - t).multLocal(t).multLocal(c2.subtract(c1));
        c.multLocal(t).multLocal(t).multLocal(a2.subtract(c2));

        return a.add(b).addLocal(c);

    }

    /// Returns the second derivative of the curve at time 't'
    public static Vector3f evaluateCurveSecondDerivative (Vector3f[] points, float t) {
        return evaluateCurveSecondDerivative (points[0], points[1], points[2], points[3], t);
    }

    ///Returns the second derivative of the curve at time 't'
    public static Vector3f evaluateCurveSecondDerivative (Vector3f a1, Vector3f c1, Vector3f c2, Vector3f a2, float t) {
        t = FastMath.clamp(t, 0, 1);
        // return 6 * (1 - t) * (c2 - 2 * c1 + a1) + 6 * t * (a2 - 2 * c2 + c1);


        // 2 * c1 + a1
        //

        // 6 * (1 - t) * (c2 - 2 * c1 + a1)
        // + 6 * t * (a2 - 2 * c2 + c1);

        // B rackets
        // O rder
        // D ivision
        // M ultiplication
        // A ddition
        // S ubtraction

        Vector3f a = new Vector3f(6, 6, 6);
        Vector3f b = new Vector3f(6, 6, 6);

        a.multLocal(1 - t).multLocal(c2.subtract(c1.mult(2).addLocal(a1)));
        b.multLocal(t).multLocal(a2.subtract(c2.mult(2).add(c1)));

        Vector3f result = a.add(b);
        return result;
    }

    /// Calculates the normal vector (vector perpendicular to the curve) at specified time
    public static Vector3f normal (Vector3f[] points, float t) {
        return normal (points[0], points[1], points[2], points[3], t);
    }

    /// Calculates the normal vector (vector perpendicular to the curve) at specified time
    public static Vector3f normal (Vector3f a1, Vector3f c1, Vector3f c2, Vector3f a2, float t) {
        Vector3f tangent = evaluateCurveDerivative (a1, c1, c2, a2, t);
        Vector3f nextTangent = evaluateCurveSecondDerivative (a1, c1, c2, a2, t);
        // Vector3f c = Vector3f.Cross (nextTangent, tangent);
        Vector3f c = nextTangent.cross(tangent);
        // return Vector3f.Cross (c, tangent).normalized;
        return c.cross(tangent).normalizeLocal();
    }

    public static BoundingBox calculateSegmentBounds (Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3) {
        MinMax3D minMax = new MinMax3D ();
        minMax.addValue(p0);
        minMax.addValue(p3);

        List<Float> extremePointTimes = extremePointTimes (p0,p1,p2,p3);

        for (float t : extremePointTimes) {
            minMax.addValue(CubicBezierUtility.evaluateCurve (p0, p1, p2, p3, t));
        }

        return new BoundingBox(
                // (minMax.Min + minMax.Max) / 2,
                minMax.getMin(),
                // minMax.Max - minMax.Min
                minMax.getMax()
        );
    }

    /// Splits curve into two curves at time t. Returns 2 arrays of 4 points.
    public static Vector3f[][] splitCurve (Vector3f[] points, float t) {
        // Vector3f a1 = Vector3f.Lerp (points[0], points[1], t);
        Vector3f a1 = new Vector3f(points[0]).interpolateLocal(points[1], t);
        // Vector3f a2 = Vector3f.Lerp (points[1], points[2], t);
        Vector3f a2 = new Vector3f(points[1]).interpolateLocal(points[2], t);
        // Vector3f a3 = Vector3f.Lerp (points[2], points[3], t);
        Vector3f a3 = new Vector3f(points[2]).interpolateLocal(points[3], t);
        // Vector3f b1 = Vector3f.Lerp (a1, a2, t);
        Vector3f b1 = new Vector3f(a1).interpolateLocal(a2, t);
        // Vector3f b2 = Vector3f.Lerp (a2, a3, t);
        Vector3f b2 = new Vector3f(a2).interpolateLocal(a3, t);
        // Vector3f pointOnCurve = Vector3f.Lerp (b1, b2, t);
        Vector3f pointOnCurve = new Vector3f(b1).interpolateLocal(b2, t);

        return new Vector3f[][] {
                new Vector3f[] { points[0], a1, b1, pointOnCurve },
                new Vector3f[] { pointOnCurve, b2, a3, points[3] }
        };
    }

    // Crude, but fast estimation of curve length.
    public static float estimateCurveLength (Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3) {
        // float controlNetLength = (p0 - p1).magnitude + (p1 - p2).magnitude + (p2 - p3).magnitude;
        float controlNetLength = p0.subtract(p1).length() + p1.subtract(p2).length() + p2.subtract(p3).length();

        // float estimatedCurveLength = (p0 - p3).magnitude + controlNetLength / 2f;
        float estimatedCurveLength = p0.subtract(p3).length() + controlNetLength / 2f;
        return estimatedCurveLength;
    }

    /// Times of stationary points on curve (points where derivative is zero on any axis)
    public static List<Float> extremePointTimes (Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3) {
        // coefficients of derivative function
        // Vector3f a = 3 * (-p0 + 3 * p1 - 3 * p2 + p3);

        Vector3f a1 = new Vector3f(3,3,3).mult(p1);
        Vector3f a2 = new Vector3f(3,3,3).mult(p2);

        Vector3f a = new Vector3f(3,3,3);
        a.multLocal(p0.negate()).addLocal(a1).subtractLocal(a2).addLocal(p3);


        /*
        Vector3f a = new Vector3f(3,3,3).mult(
                p0.negate()
                .add(3,3,3)
                .mult(p1.subtract(3,3,3))
                .mult(p2.add(p3))
        );

         */

        // Vector3f b = 6 * (p0 - 2 * p1 + p2);
        Vector3f b = new Vector3f(6,6,6)
                .multLocal(
                        p0.subtract(new Vector3f(2,2,2).mult(p1.add(p2)))
                );

        // Vector3f c = 3 * (p1 - p0);
        Vector3f c = new Vector3f(3,3,3).mult(p1.subtract(p0));

        List<Float> times = new ArrayList<>();

        times.addAll(stationaryPointTimes(a.x, b.x, c.x));
        times.addAll (stationaryPointTimes(a.y, b.y, c.y));
        times.addAll (stationaryPointTimes(a.z, b.z, c.z));
        return times;
    }

    // Finds times of stationary points on curve defined by ax^2 + bx + c.
    // Only times between 0 and 1 are considered as Bezier only uses values in that range
    static Collection<Float> stationaryPointTimes (float a, float b, float c) {
        List<Float> times = new ArrayList<Float> ();

        // from quadratic equation: y = [-b +- sqrt(b^2 - 4ac)]/2a
        if (a != 0) {
            float discriminant = b * b - 4 * a * c;
            if (discriminant >= 0) {
                float s = FastMath.sqrt(discriminant);
                float t1 = (-b + s) / (2 * a);
                if (t1 >= 0 && t1 <= 1) {
                    times.add (t1);
                }

                if (discriminant != 0) {
                    float t2 = (-b - s) / (2 * a);

                    if (t2 >= 0 && t2 <= 1) {
                        times.add (t2);
                    }
                }
            }
        }
        return times;
    }

}
