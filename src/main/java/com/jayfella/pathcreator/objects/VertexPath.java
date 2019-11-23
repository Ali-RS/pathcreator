package com.jayfella.pathcreator.objects;

/// A vertex path is a collection of points (vertices) that lie along a bezier path.
/// This allows one to do things like move at a constant speed along the path,
/// which is not possible with a bezier path directly due to how they're constructed mathematically.

import com.jayfella.pathcreator.utility.MathUtility;
import com.jayfella.pathcreator.utility.UnityMath;
import com.jayfella.pathcreator.utility.VertexPathUtility;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/// This class also provides methods for getting the position along the path at a certain distance or time
/// (where time = 0 is the start of the path, and time = 1 is the end of the path).
/// Other info about the path (tangents, normals, rotation) can also be retrieved in this manner.
public class VertexPath {

     // #region Fields

    private PathSpace space;
    private boolean closedLoop;
    private Vector3f[] localPoints;
    private Vector3f[] localTangents;
    private Vector3f[] localNormals;

    /// Percentage along the path at each vertex (0 being start of path, and 1 being the end)
    private float[] times;
    /// Total distance between the vertices of the polyline
    private float length;
    /// Total distance from the first vertex up to each vertex in the polyline
    private float[] cumulativeLengthAtEachVertex;
    /// Bounding box of the path
    private BoundingBox bounds;
    /// Equal to (0,0,-1) for 2D paths, and (0,1,0) for XZ paths
    private Vector3f up;

    // Default values and constants:
    static final int accuracy = 10; // A scalar for how many times bezier path is divided when determining vertex positions
    static final float minVertexSpacing = .01f;

    // Transform transform;
    Spatial spatial;

    // #endregion

    // #region Constructors

    /// <summary> Splits bezier path into array of vertices along the path.</summary>
    ///<param name="maxAngleError">How much can the angle of the path change before a vertex is added. This allows fewer vertices to be generated in straighter sections.</param>
    ///<param name="minVertexDst">Vertices won't be added closer together than this distance, regardless of angle error.</param>
    public VertexPath(BezierPath bezierPath, Spatial spatial, float maxAngleError /* = 0.3f */, float minVertexDst /* = 0 */) {
            this (bezierPath, VertexPathUtility.SplitBezierPathByAngleError (bezierPath, maxAngleError, minVertexDst, VertexPath.accuracy), spatial);
    }

    /// <summary> Splits bezier path into array of vertices along the path.</summary>
    ///<param name="maxAngleError">How much can the angle of the path change before a vertex is added. This allows fewer vertices to be generated in straighter sections.</param>
    ///<param name="minVertexDst">Vertices won't be added closer together than this distance, regardless of angle error.</param>
    ///<param name="accuracy">Higher value means the change in angle is checked more frequently.</param>
    public VertexPath(BezierPath bezierPath, Spatial spatial, float vertexSpacing) {
            this (bezierPath, VertexPathUtility.SplitBezierPathEvenly (bezierPath, Math.max (vertexSpacing, minVertexSpacing), VertexPath.accuracy), spatial);
    }

    /// Internal constructor
    VertexPath(BezierPath bezierPath, VertexPathUtility.PathSplitData pathSplitData, Spatial spatial) {
        this.spatial = spatial;
        // this.transform = transform;
        space = bezierPath.getSpace();
        closedLoop = bezierPath.isClosed();
        int numVerts = pathSplitData.getVertices().size();
        length = pathSplitData.getCumulativeLength().get(numVerts - 1);

        localPoints = new Vector3f[numVerts];
        localNormals = new Vector3f[numVerts];
        localTangents = new Vector3f[numVerts];
        cumulativeLengthAtEachVertex = new float[numVerts];
        times = new float[numVerts];

        // bounds = new Bounds ((pathSplitData.minMax.Min + pathSplitData.minMax.Max) / 2, pathSplitData.minMax.Max - pathSplitData.minMax.Min);
        bounds = new BoundingBox(
          pathSplitData.getMinMax().getMin().add(pathSplitData.getMinMax().getMin()).divide(2),
          pathSplitData.getMinMax().getMax().subtract(pathSplitData.getMinMax().getMin()));

        // Figure out up direction for path
        // up = (bounds.size.z > bounds.size.y) ? Vector3f.up : -Vector3f.forward;
        up = (bounds.getZExtent() > bounds.getYExtent()) ? Vector3f.UNIT_Y.clone() : Vector3f.UNIT_Z.negate();
        Vector3f lastRotationAxis = up;

        // Loop through the data and assign to arrays.
        for (int i = 0; i < localPoints.length; i++) {
            localPoints[i] = pathSplitData.getVertices().get(i);
            localTangents[i] = pathSplitData.getTangents().get(i);
            cumulativeLengthAtEachVertex[i] = pathSplitData.getCumulativeLength().get(i);
            times[i] = cumulativeLengthAtEachVertex[i] / length;

            // Calculate normals
            if (space == PathSpace.xyz) {
                if (i == 0) {
                    // localNormals[0] = Vector3f.Cross (lastRotationAxis, pathSplitData.tangents[0]).normalized;
                    localNormals[0] = lastRotationAxis.cross(pathSplitData.getTangents().get(0)).normalize();
                } else {
                    // First reflection
                    // Vector3f offset = (localPoints[i] - localPoints[i - 1]);
                    Vector3f offset = localPoints[i].subtract(localPoints[i - 1]);
                    // float sqrDst = offset.sqrMagnitude;
                    float sqrDst = offset.lengthSquared();
                    // Vector3f r = lastRotationAxis - offset * 2 / sqrDst * Vector3f.Dot (offset, lastRotationAxis);
                    // Vector3f r = lastRotationAxis.subtract(offset.mult(2)).divide(sqrDst * offset.dot(lastRotationAxis));
                    Vector3f r1 = offset.mult(2);
                    float dot = offset.dot(lastRotationAxis);
                    float r2 = sqrDst * dot;
                    Vector3f r = lastRotationAxis.subtract(r1.divide(r2));

                    // Vector3f t = localTangents[i - 1] - offset * 2 / sqrDst * Vector3f.Dot (offset, localTangents[i - 1]);
                    Vector3f t = localTangents[i - 1].subtract(offset.mult(2)).divide(sqrDst * offset.dot(localTangents[i - 1]));

                    // Second reflection
                    // Vector3f v2 = localTangents[i] - t;
                    Vector3f v2 = localTangents[i].subtract(t);
                    // float c2 = Vector3f.Dot (v2, v2);
                    float c2 = v2.dot(v2);

                    // Vector3f finalRot = r - v2 * 2 / c2 * Vector3f.Dot (v2, r);
                    Vector3f finalRot = r.subtract(v2.mult(2)).divide(c2 * v2.dot(r));

                    // Vector3f n = Vector3f.Cross (finalRot, localTangents[i]).normalized;
                    Vector3f n = finalRot.cross(localTangents[i]).normalize();

                    localNormals[i] = n;
                    lastRotationAxis = finalRot;
                }
            } else {
                // localNormals[i] = Vector3f.Cross (localTangents[i], up) * ((bezierPath.FlipNormals) ? 1 : -1);
                localNormals[i] = localTangents[i].cross(up).mult(bezierPath.getFlipNormals() ? 1 : -1);
            }
        }

        // Apply correction for 3d normals along a closed path
        if (space == PathSpace.xyz && closedLoop) {
            // Get angle between first and last normal (if zero, they're already lined up, otherwise we need to correct)
            // float normalsAngleErrorAcrossJoin = Vector3f.SignedAngle (localNormals[localNormals.Length - 1], localNormals[0], localTangents[0]);
            // @todo: Vector3f.SignedAngle
            float normalsAngleErrorAcrossJoin = localNormals[localNormals.length - 1].angleBetween(localNormals[0]);
            // Gradually rotate the normals along the path to ensure start and end normals line up correctly
            if (Math.abs (normalsAngleErrorAcrossJoin) > 0.1f) // don't bother correcting if very nearly correct
            {
                for (int i = 1; i < localNormals.length; i++) {
                    float t = (i / (localNormals.length - 1f));
                    float angle = normalsAngleErrorAcrossJoin * t;
                    // Quaternion rot = Quaternion.AngleAxis (angle, localTangents[i]);
                    Quaternion rot = new Quaternion().fromAngleAxis(angle, localTangents[i]);
                    // localNormals[i] = rot * localNormals[i] * ((bezierPath.FlipNormals) ? -1 : 1);
                    localNormals[i] = rot.mult(localNormals[i]).mult(bezierPath.getFlipNormals() ? -1 : 1);
                }
            }
        }

        // Rotate normals to match up with user-defined anchor angles
        if (space == PathSpace.xyz) {
            for (int anchorIndex = 0; anchorIndex < pathSplitData.getAnchorVertexMap().size() - 1; anchorIndex++) {
                int nextAnchorIndex = (closedLoop) ? (anchorIndex + 1) % bezierPath.getNumSegments() : anchorIndex + 1;

                float startAngle = bezierPath.getAnchorNormalAngle(anchorIndex) + bezierPath.getGlobalNormalsAngle();
                float endAngle = bezierPath.getAnchorNormalAngle(nextAnchorIndex) + bezierPath.getGlobalNormalsAngle();

                // @todo: Mathf.DeltaAngle
                // Calculates the shortest difference between two given angles given in degrees.
                // float deltaAngle = Mathf.DeltaAngle (startAngle, endAngle);
                float deltaAngle = UnityMath.deltaAngle (startAngle, endAngle);

                // int startVertIndex = pathSplitData.anchorVertexMap[anchorIndex];
                int startVertIndex = pathSplitData.getAnchorVertexMap().get(anchorIndex);
                // int endVertIndex = pathSplitData.anchorVertexMap[anchorIndex + 1];
                int endVertIndex = pathSplitData.getAnchorVertexMap().get(anchorIndex + 1);

                int num = endVertIndex - startVertIndex;
                // if (anchorIndex == pathSplitData.anchorVertexMap.Count - 2) {
                if (anchorIndex == pathSplitData.getAnchorVertexMap().size() - 2) {
                    num += 1;
                }
                for (int i = 0; i < num; i++) {
                    int vertIndex = startVertIndex + i;
                    float t = i / (num - 1f);
                    float angle = startAngle + deltaAngle * t;
                    // Quaternion rot = Quaternion.AngleAxis (angle, localTangents[vertIndex]);
                    Quaternion rot = new Quaternion().fromAngleAxis(angle, localTangents[vertIndex]);
                    // localNormals[vertIndex] = (rot * localNormals[vertIndex]) * ((bezierPath.FlipNormals) ? -1 : 1);
                    localNormals[vertIndex] = rot.mult(localNormals[vertIndex]).mult(bezierPath.getFlipNormals() ? - 1 : 1);
                }
            }
        }
    }

    // #endregion

    // #region Public methods and accessors


    public boolean isClosedLoop() {
        return closedLoop;
    }

    public PathSpace getSpace() {
        return space;
    }

    public Vector3f getUp() {
        return up;
    }

    public Vector3f getTangent(int index) {
        return MathUtility.TransformDirection (localTangents[index], spatial, space);
    }

    public Vector3f getNormal(int index) {
        return MathUtility.TransformDirection(localNormals[index], spatial, space);
    }

    public void updateTransform (Transform transform) {
        // this.transform = transform;
    }

    /*
    public int NumPoints {
        get {
            return localPoints.Length;
        }
    }
     */

    public int getNumPoints() {
        return localPoints.length;
    }


    public Vector3f GetTangent (int index) {
        return MathUtility.TransformDirection (localTangents[index], spatial, space);
    }

    public Vector3f GetNormal (int index) {
        return MathUtility.TransformDirection (localNormals[index], spatial, space);
    }

    public Vector3f getPoint(int index) {
        return MathUtility.TransformPoint (localPoints[index], spatial, space);
    }

    /// Gets point on path based on distance travelled.
    public Vector3f GetPointAtDistance (float dst, EndOfPathInstruction endOfPathInstruction /* = EndOfPathInstruction.Loop */) {
        float t = dst / length;
        return GetPointAtTime (t, endOfPathInstruction);
    }

    /// Gets forward direction on path based on distance travelled.
    public Vector3f GetDirectionAtDistance (float dst, EndOfPathInstruction endOfPathInstruction /* = EndOfPathInstruction.Loop */) {
        float t = dst / length;
        return GetDirection (t, endOfPathInstruction);
    }

    /// Gets normal vector on path based on distance travelled.
    public Vector3f GetNormalAtDistance (float dst, EndOfPathInstruction endOfPathInstruction /* = EndOfPathInstruction.Loop */) {
        float t = dst / length;
        return GetNormal (t, endOfPathInstruction);
    }

    /// Gets a rotation that will orient an object in the direction of the path at this point, with local up point along the path's normal
    public Quaternion GetRotationAtDistance (float dst, EndOfPathInstruction endOfPathInstruction /* = EndOfPathInstruction.Loop */) {
        float t = dst / length;
        return GetRotation (t, endOfPathInstruction);
    }

    /// Gets point on path based on 'time' (where 0 is start, and 1 is end of path).
    public Vector3f GetPointAtTime (float t, EndOfPathInstruction endOfPathInstruction /* = EndOfPathInstruction.Loop */) {
        TimeOnPathData data = CalculatePercentOnPathData (t, endOfPathInstruction);
        // return Vector3f.Lerp (GetPoint (data.previousIndex), GetPoint (data.nextIndex), data.percentBetweenIndices);
        return new Vector3f().interpolateLocal(getPoint(data.getPreviousIndex()), getPoint(data.getNextIndex()), data.getPercentBetweenIndices());
    }

    /// Gets forward direction on path based on 'time' (where 0 is start, and 1 is end of path).
    public Vector3f GetDirection (float t, EndOfPathInstruction endOfPathInstruction /* = EndOfPathInstruction.Loop */) {
        TimeOnPathData data = CalculatePercentOnPathData (t, endOfPathInstruction);
        // Vector3f dir = Vector3f.Lerp (localTangents[data.previousIndex], localTangents[data.nextIndex], data.percentBetweenIndices);
        Vector3f dir = new Vector3f().interpolateLocal(localTangents[data.previousIndex], localTangents[data.nextIndex], data.percentBetweenIndices);
        return MathUtility.TransformDirection (dir, spatial, space);
    }

    /// Gets normal vector on path based on 'time' (where 0 is start, and 1 is end of path).
    public Vector3f GetNormal (float t, EndOfPathInstruction endOfPathInstruction /* = EndOfPathInstruction.Loop */) {
        TimeOnPathData data = CalculatePercentOnPathData (t, endOfPathInstruction);
        // Vector3f normal = Vector3f.Lerp (localNormals[data.previousIndex], localNormals[data.nextIndex], data.percentBetweenIndices);
        Vector3f normal = new Vector3f().interpolateLocal(localNormals[data.previousIndex], localNormals[data.nextIndex], data.percentBetweenIndices);
        return MathUtility.TransformDirection (normal, spatial, space);
    }

    /// Gets a rotation that will orient an object in the direction of the path at this point, with local up point along the path's normal
    public Quaternion GetRotation (float t, EndOfPathInstruction endOfPathInstruction /* = EndOfPathInstruction.Loop */) {
        TimeOnPathData data = CalculatePercentOnPathData (t, endOfPathInstruction);

        // Vector3f direction = Vector3f.Lerp (localTangents[data.previousIndex], localTangents[data.nextIndex], data.percentBetweenIndices);
        Vector3f direction = new Vector3f().interpolateLocal(localTangents[data.previousIndex], localTangents[data.nextIndex], data.percentBetweenIndices);
        // Vector3f normal = Vector3f.Lerp (localNormals[data.previousIndex], localNormals[data.nextIndex], data.percentBetweenIndices);
        Vector3f normal = new Vector3f().interpolateLocal(localNormals[data.previousIndex], localNormals[data.nextIndex], data.percentBetweenIndices);

        // return Quaternion.LookRotation (MathUtility.TransformDirection (direction, transform, space), MathUtility.TransformDirection (normal, transform, space));
        Quaternion rotation = new Quaternion();
        rotation.lookAt(MathUtility.TransformDirection(direction, spatial, space), MathUtility.TransformDirection(normal, spatial, space));
        return rotation;
    }

    /// Finds the closest point on the path from any point in the world
    public Vector3f GetClosestPointOnPath (Vector3f worldPoint) {
        TimeOnPathData data = CalculateClosestPointOnPathData (worldPoint);
        // return Vector3f.Lerp (GetPoint (data.previousIndex), GetPoint (data.nextIndex), data.percentBetweenIndices);
        return new Vector3f().interpolateLocal(getPoint(data.previousIndex), getPoint(data.nextIndex), data.percentBetweenIndices);
    }

    /// Finds the 'time' (0=start of path, 1=end of path) along the path that is closest to the given point
    public float GetClosestTimeOnPath (Vector3f worldPoint) {
        TimeOnPathData data = CalculateClosestPointOnPathData (worldPoint);
        // return Mathf.Lerp (times[data.previousIndex], times[data.nextIndex], data.percentBetweenIndices);
        return FastMath.interpolateLinear(times[data.previousIndex], times[data.nextIndex], data.percentBetweenIndices);
    }

    /// Finds the distance along the path that is closest to the given point
    public float GetClosestDistanceAlongPath (Vector3f worldPoint) {
        TimeOnPathData data = CalculateClosestPointOnPathData (worldPoint);
        // return Mathf.Lerp (cumulativeLengthAtEachVertex[data.previousIndex], cumulativeLengthAtEachVertex[data.nextIndex], data.percentBetweenIndices);
        return FastMath.interpolateLinear(cumulativeLengthAtEachVertex[data.previousIndex], cumulativeLengthAtEachVertex[data.nextIndex], data.percentBetweenIndices);
    }

    // #endregion

    // #region Internal methods

    /// For a given value 't' between 0 and 1, calculate the indices of the two vertices before and after t.
    /// Also calculate how far t is between those two vertices as a percentage between 0 and 1.
    private TimeOnPathData CalculatePercentOnPathData (float t, EndOfPathInstruction endOfPathInstruction) {
        // Constrain t based on the end of path instruction
        switch (endOfPathInstruction) {
            case Loop:
                // If t is negative, make it the equivalent value between 0 and 1
                if (t < 0) {
                    // t += Mathf.CeilToInt (Mathf.Abs (t));
                    t += (int)Math.ceil(Math.abs(t));
                }
                t %= 1;
                break;
            case Reverse:
                // @todo PingPong
                // PingPongs the value t, so that it is never larger than length and never smaller than 0.
                // t = Mathf.PingPong (t, 1);
                t = UnityMath.pingPong (t, 1);
                break;
            case Stop:
                // t = Mathf.Clamp01 (t);
                t = FastMath.clamp(t, 0, 1);
                break;
        }

        int prevIndex = 0;
        int nextIndex = getNumPoints() - 1;
        // int i = Mathf.RoundToInt (t * (NumPoints - 1)); // starting guess
        int i = Math.round(t * (getNumPoints() - 1));

        // Starts by looking at middle vertex and determines if t lies to the left or to the right of that vertex.
        // Continues dividing in half until closest surrounding vertices have been found.
        while (true) {
            // t lies to left
            if (t <= times[i]) {
                nextIndex = i;
            }
            // t lies to right
            else {
                prevIndex = i;
            }
            i = (nextIndex + prevIndex) / 2;

            if (nextIndex - prevIndex <= 1) {
                break;
            }
        }

        /*
        Calculates the linear parameter t that produces the interpolant value within the range [a, b].

        The a and b values define the start and end of the line. Value is a location between a and b. Subtract a from
        both a and b and value to make a', b' and value'. This makes a' to be zero and b' and value' to be reduced.
        Finally divide value' by b'. This gives the InverseLerp amount.
         */
        // @todo: inverseLerp
        // float abPercent = Mathf.InverseLerp (times[prevIndex], times[nextIndex], t);
        float abPercent = UnityMath.inverseLerp (times[prevIndex], times[nextIndex], t);

        return new TimeOnPathData(prevIndex, nextIndex, abPercent);
    }

    /// Calculate time data for closest point on the path from given world point
    TimeOnPathData CalculateClosestPointOnPathData (Vector3f worldPoint) {
        // float minSqrDst = float.MaxValue;
        float minSqrDst = Float.MAX_VALUE;
        // Vector3f closestPoint = Vector3f.zero;
        Vector3f closestPoint = new Vector3f();
        int closestSegmentIndexA = 0;
        int closestSegmentIndexB = 0;

        for (int i = 0; i < localPoints.length; i++) {
            int nextI = i + 1;
            if (nextI >= localPoints.length) {
                if (closedLoop) {
                    nextI %= localPoints.length;
                } else {
                    break;
                }
            }

            Vector3f closestPointOnSegment = MathUtility.ClosestPointOnLineSegment (worldPoint, getPoint(i), getPoint(nextI));

            // float sqrDst = (worldPoint - closestPointOnSegment).sqrMagnitude;
            float sqrDst = worldPoint.subtract(closestPointOnSegment).lengthSquared();

            if (sqrDst < minSqrDst) {
                minSqrDst = sqrDst;
                closestPoint = closestPointOnSegment;
                closestSegmentIndexA = i;
                closestSegmentIndexB = nextI;
            }

        }
        // float closestSegmentLength = (GetPoint (closestSegmentIndexA) - GetPoint (closestSegmentIndexB)).magnitude;
        float closestSegmentLength = getPoint(closestSegmentIndexA).subtract(getPoint(closestSegmentIndexB)).length();
        // float t = (closestPoint - GetPoint (closestSegmentIndexA)).magnitude / closestSegmentLength;
        float t = closestPoint.subtract(getPoint(closestSegmentIndexA)).length() / closestSegmentLength;
        return new TimeOnPathData(closestSegmentIndexA, closestSegmentIndexB, t);
    }

    public float getTime(int index) {
        return times[index];
    }

    public float[] getTimes() {
        return times;
    }

    public static class TimeOnPathData {

        private final int previousIndex;
        private final int nextIndex;
        private final float percentBetweenIndices;

            TimeOnPathData(int prev, int next, float percentBetweenIndices) {
            this.previousIndex = prev;
            this.nextIndex = next;
            this.percentBetweenIndices = percentBetweenIndices;
        }

        int getPreviousIndex() {
            return previousIndex;
        }

        int getNextIndex() {
            return nextIndex;
        }

        float getPercentBetweenIndices() {
            return percentBetweenIndices;
        }
    }
    
    // #endregion
    
}
