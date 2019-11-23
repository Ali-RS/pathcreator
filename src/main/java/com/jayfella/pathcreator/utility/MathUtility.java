package com.jayfella.pathcreator.utility;

import com.jayfella.pathcreator.objects.PathSpace;
import com.jme3.math.*;
import com.jme3.scene.Spatial;

public class MathUtility {

    // I get the feeling here that if we use the spatial instead of "transform" we get a lot of the functionality
    // that these classes want such as "world scale" and "world location".

    static PosRotScale LockTransformToSpace (Spatial spatial, PathSpace space) {
        // var original = new PosRotScale (t);
        PosRotScale original = new PosRotScale(spatial.getLocalTransform());

        if (space == PathSpace.xy) {
            // t.eulerAngles = new Vector3f (0, 0, t.eulerAngles.z);
            spatial.setLocalRotation(new Quaternion().fromAngles(0, 0, spatial.getLocalRotation().toAngles(null)[2]));
            // t.position = new Vector3f (t.position.x, t.position.y, 0);
            spatial.setLocalTranslation(new Vector3f(spatial.getLocalTranslation().x, spatial.getLocalTranslation().y, 0));
        } else if (space == PathSpace.xz) {
            // t.eulerAngles = new Vector3f (0, t.eulerAngles.y, 0);
            spatial.setLocalRotation(new Quaternion().fromAngles(0, spatial.getLocalRotation().toAngles(null)[1], 0));
            // t.position = new Vector3f(t.position.x, 0, t.position.z);
            spatial.setLocalTranslation(new Vector3f(spatial.getLocalTranslation().x, 0, spatial.getLocalTranslation().z));
        }

        //float maxScale = Mathf.Max (t.localScale.x * t.parent.localScale.x, t.localScale.y * t.parent.localScale.y, t.localScale.z * t.parent.localScale.z);
        // float maxScale = Mathf.Max (t.lossyScale.x, t.lossyScale.y, t.lossyScale.z);

        /*
        Transform.lossyScale
        ===
        The global scale of the object (Read Only).
        Please note that if you have a parent transform with scale and a child that is arbitrarily rotated, the scale
        will be skewed. Thus scale can not be represented correctly in a 3 component vector but only a 3x3 matrix.
        Such a representation is quite inconvenient to work with however. lossyScale is a convenience property that
        attempts to match the actual world scale as much as it can. If your objects are not skewed the value will be
        completely correct and most likely the value will not be very different if it contains skew too.
         */
        float maxScale = Math.max(spatial.getWorldScale().x, Math.max(spatial.getWorldScale().y, spatial.getWorldScale().z));

        //t.localScale = Vector3f.one * maxScale;
        spatial.setLocalScale(new Vector3f(1,1,1).mult(maxScale));

        return original;
    }

    public static Vector3f TransformPoint (Vector3f p, Spatial spatial, PathSpace space) {
        //PosRotScale original = LockTransformToSpace (spatial, space);
        /*
        Transform.TransformPoint
        Transforms position from local space to world space.
        Note that the returned position is affected by scale. Use Transform.TransformDirection if you are dealing with direction vectors.
         */
        // @todo: TransformPoint
        // Vector3f transformedPoint = t.TransformPoint (p);
        Vector3f transformedPoint = spatial.getLocalTranslation().add(spatial.getLocalScale().mult(p));
        // original.SetTransform(t);
        //original.setPosition(transformedPoint);
        return transformedPoint;
    }

    public static Vector3f InverseTransformPoint (Vector3f p, Spatial spatial, PathSpace space) {
        // PosRotScale original = LockTransformToSpace (spatial, space);
        // InverseTransformPoint = worldToLocal
        // @todo: InverseTransformPoint
        // Vector3f transformedPoint = t.InverseTransformPoint (p);
        Vector3f transformedPoint = spatial.worldToLocal(p, null);
        // original.SetTransform (t);
        // original.setPosition(transformedPoint);
        return transformedPoint;
    }

    public static Vector3f TransformVector (Vector3f p, Spatial spatial, PathSpace space) {
        //PosRotScale original = LockTransformToSpace (spatial, space);

        // i think this is for scale.

        /*
        Transform.TransformVector
        Transforms vector from local space to world space.

        This operation is not affected by position of the transform, but it is affected by scale. The returned vector
        may have a different length than vector.
         */
        // @todo: TransformVector
        // Vector3f transformedPoint = t.TransformVector (p);
        Vector3f transformedPoint = spatial.getWorldScale();
        // original.SetTransform (t);
        //original.setScale(transformedPoint);
        return transformedPoint;
    }

    public static Vector3f InverseTransformVector (Vector3f p, Spatial spatial, PathSpace space) {
        //PosRotScale original = LockTransformToSpace (spatial, space);
        /*
        Transform.InverseTransformVector
        Transforms a vector from world space to local space. The opposite of Transform.TransformVector.

        This operation is affected by scale.
         */
        // @todo: InverseTransformVector
        // Vector3f transformedPoint = t.InverseTransformVector (p);
        Vector3f transformedPoint = spatial.worldToLocal(p, null);
        // original.SetTransform (t);
        //original.setScale(transformedPoint);
        return transformedPoint;
    }

    public static Vector3f TransformDirection (Vector3f p, Spatial spatial, PathSpace space) {
        //PosRotScale original = LockTransformToSpace (spatial, space);
        /*
        Transform.TransformDirection
        Transforms direction from local space to world space.

        This operation is not affected by scale or position of the transform. The returned vector has the same length
        as direction.
         */
        // @todo TransformDirection
        // Vector3f transformedPoint = t.TransformDirection (p);
        // original.SetTransform (t);
        // return transformedPoint;

        // Vector3f transformedPoint = spatial.localToWorld(p, null);
        //original.setRotation(spatial.getWorldRotation());
        return spatial.getWorldRotation().mult(Vector3f.UNIT_Z);
    }

    public static Vector3f InverseTransformDirection (Vector3f p, Spatial spatial, PathSpace space) {
        //PosRotScale original = LockTransformToSpace (spatial, space);
        /*
        Transform.InverseTransformDirection
        Transforms a direction from world space to local space. The opposite of Transform.TransformDirection.

        This operation is unaffected by scale.
         */
        // @todo: InverseTransformDirection
        // Vector3f transformedPoint = t.InverseTransformDirection (p);
        // original.SetTransform (t);
        // return transformedPoint;
        //original.setRotation(spatial.getLocalRotation());
        return spatial.getLocalRotation().mult(Vector3f.UNIT_Z);
    }

    public static boolean LineSegmentsIntersect (Vector2f a1, Vector2f a2, Vector2f b1, Vector2f b2) {
        float d = (b2.x - b1.x) * (a1.y - a2.y) - (a1.x - a2.x) * (b2.y - b1.y);
        if (d == 0)
            return false;
        float t = ((b1.y - b2.y) * (a1.x - b1.x) + (b2.x - b1.x) * (a1.y - b1.y)) / d;
        float u = ((a1.y - a2.y) * (a1.x - b1.x) + (a2.x - a1.x) * (a1.y - b1.y)) / d;

        return t >= 0 && t <= 1 && u >= 0 && u <= 1;
    }

    public static boolean LinesIntersect (Vector2f a1, Vector2f a2, Vector2f a3, Vector2f a4) {
        return (a1.x - a2.x) * (a3.y - a4.y) - (a1.y - a2.y) * (a3.x - a4.x) != 0;
    }

    public static Vector2f PointOfLineLineIntersection (Vector2f a1, Vector2f a2, Vector2f a3, Vector2f a4) {
        float d = (a1.x - a2.x) * (a3.y - a4.y) - (a1.y - a2.y) * (a3.x - a4.x);
        if (d == 0) {
            // Debug.LogError ("Lines are parallel, please check that this is not the case before calling line intersection method");
            // return Vector2.zero;
            System.out.println("Lines are parallel, please check that this is not the case before calling line intersection method");
            return new Vector2f();
        } else {
            float n = (a1.x - a3.x) * (a3.y - a4.y) - (a1.y - a3.y) * (a3.x - a4.x);
            float t = n / d;
            // return a1 + (a2 - a1) * t;
            return a1.add(a2.subtract(a1)).mult(t);
        }
    }

    public static Vector2f ClosestPointOnLineSegment (Vector2f p, Vector2f a, Vector2f b) {
        // Vector2f aB = b - a;
        Vector2f aB = b.subtract(a);
        // Vector2f aP = p - a;
        Vector2f aP = p.subtract(a);
        // float sqrLenAB = aB.sqrMagnitude;
        float sqrLenAB = aB.lengthSquared();

        if (sqrLenAB == 0)
            return a;

        // float t = Mathf.Clamp01 (Vector2.Dot (aP, aB) / sqrLenAB);
        float t = FastMath.clamp(aP.dot(aB) / sqrLenAB, 0, 1);
        // return a + aB * t;
        return  a.add(aB).mult(t);
    }

    public static Vector3f ClosestPointOnLineSegment (Vector3f p, Vector3f a, Vector3f b) {
        // Vector3f aB = b - a;
        Vector3f aB = b.subtract(a);
        // Vector3f aP = p - a;
        Vector3f aP = p.subtract(a);
        // float sqrLenAB = aB.sqrMagnitude;
        float sqrLenAB = aB.lengthSquared();

        if (sqrLenAB == 0)
            return a;

        // float t = Mathf.Clamp01 (Vector3f.Dot (aP, aB) / sqrLenAB);
        float t = FastMath.clamp(aP.dot(aB) / sqrLenAB, 0, 1);
        // return a + aB * t;
        return  a.add(aB).mult(t);
    }

    public static int SideOfLine (Vector2f a, Vector2f b, Vector2f c) {
        // return (int) Mathf.Sign ((c.x - a.x) * (-b.y + a.y) + (c.y - a.y) * (b.x - a.x));
        return (int) Math.signum ((c.x - a.x) * (-b.y + a.y) + (c.y - a.y) * (b.x - a.x));
    }

    /// returns the smallest angle between ABC. Never greater than 180
    public static float MinAngle (Vector3f a, Vector3f b, Vector3f c) {
        // return Vector3f.Angle ((a - b), (c - b));
        // Returns the angle in degrees between from and to.
        /*
        Vector3.Angle
        Returns the angle in degrees between from and to.

        The angle returned is the unsigned angle between the two vectors. This means the smaller of the two possible
        angles between the two vectors is used. The result is never greater than 180 degrees.
         */
        float angle = a.subtract(b).angleBetween(c.subtract(b)) * FastMath.RAD_TO_DEG;
        return FastMath.clamp(angle, 0, 180);
    }

    public static boolean PointInTriangle (Vector2f a, Vector2f b, Vector2f c, Vector2f p) {
        float area = 0.5f * (-b.y * c.x + a.y * (-b.x + c.x) + a.x * (b.y - c.y) + b.x * c.y);
        float s = 1 / (2 * area) * (a.y * c.x - a.x * c.y + (c.y - a.y) * p.x + (a.x - c.x) * p.y);
        float t = 1 / (2 * area) * (a.x * b.y - a.y * b.x + (a.y - b.y) * p.x + (b.x - a.x) * p.y);
        return s >= 0 && t >= 0 && (s + t) <= 1;
    }

    public static boolean PointsAreClockwise (Vector2f[] points) {
        float signedArea = 0;
        for (int i = 0; i < points.length; i++) {
            int nextIndex = (i + 1) % points.length;
            signedArea += (points[nextIndex].x - points[i].x) * (points[nextIndex].y + points[i].y);
        }

        return signedArea >= 0;
    }

    public static class PosRotScale {
        private final Vector3f position;
        private final Quaternion rotation;
        private final Vector3f scale;

        public PosRotScale (Transform transform) {
            this.position = transform.getTranslation();
            this.rotation = transform.getRotation();
            this.scale = transform.getScale();
        }

        public void SetTransform (Transform t) {
            t.setTranslation(position);
            t.setRotation(rotation);
            t.setScale(scale);
        }

        public void setPosition(Vector3f position) {
            this.position.set(position);
        }

        public void setRotation(Quaternion rotation) {
            this.rotation.set(rotation);
        }

        public void setScale(Vector3f scale) {
            this.scale.set(scale);
        }

    }

}
