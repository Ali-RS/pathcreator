package com.jayfella.pathcreator.utility;

import com.jayfella.pathcreator.objects.BezierPath;
import com.jayfella.pathcreator.objects.MinMax3D;
import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class VertexPathUtility {

    public static PathSplitData SplitBezierPathByAngleError(BezierPath bezierPath, float maxAngleError, float minVertexDst, float accuracy)
    {
        PathSplitData splitData = new PathSplitData();

        splitData.vertices.add(bezierPath.getPoint(0));
        splitData.tangents.add(CubicBezierUtility.evaluateCurveDerivative(bezierPath.getPointsInSegment(0), 0).normalize());
        splitData.cumulativeLength.add(0f);
        splitData.anchorVertexMap.add(0);
        splitData.minMax.addValue(bezierPath.getPoint(0));

        Vector3f prevPointOnPath = bezierPath.getPoint(0);
        Vector3f lastAddedPoint = bezierPath.getPoint(0);

        float currentPathLength = 0;
        float dstSinceLastVertex = 0;

        // Go through all segments and split up into vertices
        for (int segmentIndex = 0; segmentIndex < bezierPath.getNumSegments(); segmentIndex++)
        {
            Vector3f[] segmentPoints = bezierPath.getPointsInSegment(segmentIndex);
            float estimatedSegmentLength = CubicBezierUtility.estimateCurveLength(segmentPoints[0], segmentPoints[1], segmentPoints[2], segmentPoints[3]);
            // int divisions = Math.ceilToInt(estimatedSegmentLength * accuracy);
            int divisions = (int) Math.ceil(estimatedSegmentLength * accuracy);
            float increment = 1f / divisions;

            for (float t = increment; t <= 1; t += increment)
            {
                boolean isLastPointOnPath = (t + increment > 1 && segmentIndex == bezierPath.getNumSegments() - 1);
                if (isLastPointOnPath) {
                    t = 1;
                }

                Vector3f pointOnPath = CubicBezierUtility.evaluateCurve(segmentPoints, t);
                Vector3f nextPointOnPath = CubicBezierUtility.evaluateCurve(segmentPoints, t + increment);

                // angle at current point on path
                float localAngle = 180 - MathUtility.MinAngle(prevPointOnPath, pointOnPath, nextPointOnPath);
                // angle between the last added vertex, the current point on the path, and the next point on the path
                float angleFromPrevVertex = 180 - MathUtility.MinAngle(lastAddedPoint, pointOnPath, nextPointOnPath);
                float angleError = Math.max(localAngle, angleFromPrevVertex);


                if ((angleError > maxAngleError && dstSinceLastVertex >= minVertexDst) || isLastPointOnPath) {

                    // currentPathLength += (lastAddedPoint - pointOnPath).magnitude;
                    currentPathLength += lastAddedPoint.subtract(pointOnPath).length();
                    splitData.cumulativeLength.add(currentPathLength);
                    splitData.vertices.add(pointOnPath);
                    splitData.tangents.add(CubicBezierUtility.evaluateCurveDerivative(segmentPoints, t).normalize());
                    splitData.minMax.addValue(pointOnPath);
                    dstSinceLastVertex = 0;
                    lastAddedPoint = pointOnPath;
                }
                else
                {
                    // dstSinceLastVertex += (pointOnPath - prevPointOnPath).magnitude;
                    dstSinceLastVertex += pointOnPath.subtract(prevPointOnPath).length();
                }
                prevPointOnPath = pointOnPath;
            }
            splitData.anchorVertexMap.add(splitData.vertices.size() - 1);
        }
        return splitData;
    }

    public static PathSplitData SplitBezierPathEvenly(BezierPath bezierPath, float spacing, float accuracy)
    {
        PathSplitData splitData = new PathSplitData();

        splitData.vertices.add(bezierPath.getPoint(0));
        splitData.tangents.add(CubicBezierUtility.evaluateCurveDerivative(bezierPath.getPointsInSegment(0), 0).normalize());
        splitData.cumulativeLength.add(0f);
        splitData.anchorVertexMap.add(0);
        splitData.minMax.addValue(bezierPath.getPoint(0));

        Vector3f prevPointOnPath = bezierPath.getPoint(0);
        Vector3f lastAddedPoint = bezierPath.getPoint(0);

        float currentPathLength = 0;
        float dstSinceLastVertex = 0;

        // Go through all segments and split up into vertices
        for (int segmentIndex = 0; segmentIndex < bezierPath.getNumSegments(); segmentIndex++)
        {
            Vector3f[] segmentPoints = bezierPath.getPointsInSegment(segmentIndex);
            float estimatedSegmentLength = CubicBezierUtility.estimateCurveLength(segmentPoints[0], segmentPoints[1], segmentPoints[2], segmentPoints[3]);
            // int divisions = Mathf.CeilToInt(estimatedSegmentLength * accuracy);
            int divisions = (int) Math.ceil(estimatedSegmentLength * accuracy);
            float increment = 1f / divisions;

            for (float t = increment; t <= 1; t += increment)
            {
                boolean isLastPointOnPath = (t + increment > 1 && segmentIndex == bezierPath.getNumSegments() - 1);
                if (isLastPointOnPath)
                {
                    t = 1;
                }
                Vector3f pointOnPath = CubicBezierUtility.evaluateCurve(segmentPoints, t);
                // dstSinceLastVertex += (pointOnPath - prevPointOnPath).magnitude;
                dstSinceLastVertex += pointOnPath.subtract(prevPointOnPath).length();

                // If vertices are now too far apart, go back by amount we overshot by
                if (dstSinceLastVertex > spacing) {
                    float overshootDst = dstSinceLastVertex - spacing;
                    // pointOnPath += (prevPointOnPath-pointOnPath).normalized * overshootDst;
                    pointOnPath.addLocal(prevPointOnPath.subtract(pointOnPath.normalize()).multLocal(overshootDst));
                    t-=increment;
                }

                if (dstSinceLastVertex >= spacing || isLastPointOnPath)
                {
                    // currentPathLength += (lastAddedPoint - pointOnPath).magnitude;
                    currentPathLength += lastAddedPoint.subtract(pointOnPath).length();
                    splitData.cumulativeLength.add(currentPathLength);
                    splitData.vertices.add(pointOnPath);
                    splitData.tangents.add(CubicBezierUtility.evaluateCurveDerivative(segmentPoints, t).normalize());
                    splitData.minMax.addValue(pointOnPath);
                    dstSinceLastVertex = 0;
                    lastAddedPoint = pointOnPath;
                }
                prevPointOnPath = pointOnPath;
            }
            splitData.anchorVertexMap.add(splitData.vertices.size() - 1);
        }
        return splitData;
    }


    public static class PathSplitData {

        private List<Vector3f> vertices = new ArrayList<>();
        private List<Vector3f> tangents = new ArrayList<>();
        private List<Float> cumulativeLength = new ArrayList<>();
        private List<Integer> anchorVertexMap = new ArrayList<>();
        private MinMax3D minMax = new MinMax3D();

        public List<Vector3f> getVertices() {
            return vertices;
        }

        public void setVertices(List<Vector3f> vertices) {
            this.vertices = vertices;
        }

        public List<Vector3f> getTangents() {
            return tangents;
        }

        public void setTangents(List<Vector3f> tangents) {
            this.tangents = tangents;
        }

        public List<Float> getCumulativeLength() {
            return cumulativeLength;
        }

        public void setCumulativeLength(List<Float> cumulativeLength) {
            this.cumulativeLength = cumulativeLength;
        }

        public List<Integer> getAnchorVertexMap() {
            return anchorVertexMap;
        }

        public void setAnchorVertexMap(List<Integer> anchorVertexMap) {
            this.anchorVertexMap = anchorVertexMap;
        }

        public MinMax3D getMinMax() {
            return minMax;
        }

        public void setMinMax(MinMax3D minMax) {
            this.minMax = minMax;
        }
    }

}
