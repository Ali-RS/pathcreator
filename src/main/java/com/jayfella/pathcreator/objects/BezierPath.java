package com.jayfella.pathcreator.objects;

import com.jayfella.pathcreator.event.CreatorEvent;
import com.jayfella.pathcreator.utility.CubicBezierUtility;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class BezierPath {

    private final List<CreatorEvent> modifiedEvents = new ArrayList<>();

    public enum ControlMode { Aligned, Mirrored, Free, Automatic };

    private List<Vector3f> points;
    private boolean isClosed;
    private PathSpace space;
    private ControlMode controlMode = ControlMode.Aligned;
    private float autoControlLength = .3f;
    private boolean boundsUpToDate;
    private BoundingBox bounds;

    // Normals settings
    private List<Float> perAnchorNormalsAngle;
    private float globalNormalsAngle;
    private boolean flipNormals;

    public BezierPath(Vector3f center, boolean isClosed, PathSpace space) {

        Vector3f dir = (space == PathSpace.xz)
                ? Vector3f.UNIT_Z.clone()
                : Vector3f.UNIT_Y.clone();

        float width = 2;
        float controlHeight = .5f;
        float controlWidth = 1f;

        points = new ArrayList<>();

        Vector3f left = Vector3f.UNIT_X.negate();
        Vector3f right = Vector3f.UNIT_X.clone();

        // centre + Vector3.left * width,
        points.add(center.add(left.mult(width)));
        // centre + Vector3.left * controlWidth + dir * controlHeight,
        points.add(center.add(left.mult(dir.add(controlWidth, controlWidth, controlWidth).mult(controlHeight))));
        // centre + Vector3.right * controlWidth - dir * controlHeight,
        points.add(center.add(right.mult(controlWidth).subtract(dir).mult(controlHeight)));
        // centre + Vector3.right * width
        points.add(center.add(right.mult(width)));

        perAnchorNormalsAngle = new ArrayList<>();
        perAnchorNormalsAngle.add(0f);
        perAnchorNormalsAngle.add(0f);

        // this.space = space;
        // this.isClosed = isClosed;
        setSpace(space);
        setClosed(isClosed);

    }

    public void addModifiedEvent(CreatorEvent event) {
        modifiedEvents.add(event);
    }

    public void removeModifiedEvent(CreatorEvent event) {
        modifiedEvents.remove(event);
    }

    public boolean getFlipNormals() {
        return flipNormals;
    }

    public void setFlipNormals(boolean flipNormals) {
        if (this.flipNormals != flipNormals) {
            this.flipNormals = flipNormals;
            notifyPathModified ();
        }
    }

    /// Get world space position of point
    public Vector3f getPoint (int i) {
        return points.get(i);
    }

    public int getNumPoints() {
        return points.size();
    }

    public int getNumSegments() {
        return points.size() / 3;
    }

    public PathSpace getSpace() {
        return space;
    }

    public void setSpace(PathSpace pathSpace) {

        if (space != pathSpace) {
            PathSpace previousSpace = space;
            space = pathSpace;
            updateToNewPathSpace(previousSpace);
        }
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        if (isClosed != closed) {
            isClosed = closed;
            updateClosedState();
        }
    }

    public BoundingBox getPathBounds() {
        if (!boundsUpToDate) {
            updateBounds();
        }

        return bounds;
    }

    /**
     * Returns an array of the 4 points making up the segment (anchor1, control1, control2, anchor2)
     * @param segmentIndex the segment index.
     * @return An array of the 4 points making up the segment (anchor1, control1, control2, anchor2)
     */
    public Vector3f[] getPointsInSegment(int segmentIndex) {

        segmentIndex = (int) FastMath.clamp(segmentIndex, 0, getNumSegments() - 1);

        return new Vector3f[] {
                getPoint(segmentIndex * 3),
                getPoint(segmentIndex * 3 + 1),
                getPoint(segmentIndex * 3 + 2),
                getPoint(loopIndex(segmentIndex * 3 + 3))
        };
    }

    /**
     * Update the bounding box of the path
     */
    private void updateBounds() {

        if (boundsUpToDate) {
            return;
        }

        // Loop through all segments and keep track of the minmax points of all their bounding boxes
        MinMax3D minMax = new MinMax3D ();

        for (int i = 0; i < getNumSegments(); i++) {
            Vector3f[] p = getPointsInSegment(i);
            minMax.addValue(p[0]);
            minMax.addValue(p[3]);

            List<Float> extremePointTimes = CubicBezierUtility.extremePointTimes (p[0], p[1], p[2], p[3]);

            for (float t : extremePointTimes) {
                minMax.addValue (CubicBezierUtility.evaluateCurve (p, t));
            }
        }

        boundsUpToDate = true;
        // bounds = new Bounds ((minMax.Min + minMax.Max) / 2, minMax.Max - minMax.Min);
        bounds = new BoundingBox(
                minMax.getMin().add(minMax.getMax()).divide(2),
                minMax.getMax().subtract(minMax.getMin())
        );
    }

    /// Calculates good positions (to result in smooth path) for the controls around specified anchor
    private void autoSetAnchorControlPoints(int anchorIndex) {

        // Calculate a vector that is perpendicular to the vector bisecting the angle between this anchor and its two immediate neighbours
        // The control points will be placed along that vector

        Vector3f anchorPos = points.get(anchorIndex);
        Vector3f dir = new Vector3f();
        float[] neighbourDistances = new float[2];

        if (anchorIndex - 3 >= 0 || isClosed) {
            Vector3f offset = points.get(loopIndex(anchorIndex - 3)).subtract(anchorPos);
            dir.addLocal(offset.normalize());
            neighbourDistances[0] = offset.length();
        }
        if (anchorIndex + 3 >= 0 || isClosed) {
            Vector3f offset = points.get(loopIndex(anchorIndex + 3)).subtract(anchorPos);
            dir.subtractLocal(offset.normalize());
            neighbourDistances[1] = -offset.length();
        }

        dir.normalizeLocal();

        // Set the control points along the calculated direction, with a distance proportional to the distance to the neighbouring control point
        for (int i = 0; i < 2; i++) {
            int controlIndex = anchorIndex + i * 2 - 1;
            if (controlIndex >= 0 && controlIndex < points.size() || isClosed) {
                // points[LoopIndex (controlIndex)] = anchorPos + dir * neighbourDistances[i] * autoControlLength;
                points.set(loopIndex (controlIndex), anchorPos.add(dir.mult(neighbourDistances[i]).mult(autoControlLength)));
            }
        }

    }

    private void autoSetStartAndEndControls() {

        if (isClosed) {

            // Handle case with only 2 anchor points separately, as will otherwise result in straight line ()
            if (getNumAnchorPoints() == 2) {

                Vector3f dirAnchorAToB = points.get(3).subtract(points.get(0)).normalize();
                float dstBetweenAnchors = points.get(0).subtract(points.get(3)).length();
                Vector3f perp = dirAnchorAToB.cross((space == PathSpace.xy) ? Vector3f.UNIT_Z.clone() : Vector3f.UNIT_Y.clone());

                // points[1] = points[0] + perp * dstBetweenAnchors / 2f;
                points.set(1, points.get(0).add(perp.mult(dstBetweenAnchors)).divide(2.0f));
                // points[5] = points[0] - perp * dstBetweenAnchors / 2f;
                points.set(5, points.get(0).subtract(perp.mult(dstBetweenAnchors)).divide(2.0f));
                // points[2] = points[3] + perp * dstBetweenAnchors / 2f;
                points.set(2, points.get(3).add(perp.mult(dstBetweenAnchors)).divide(2.0f));
                // points[4] = points[3] - perp * dstBetweenAnchors / 2f;
                points.set(4, points.get(3).subtract(perp.mult(dstBetweenAnchors)).divide(2.0f));

            }
            else {
                autoSetAnchorControlPoints(0);
                autoSetAnchorControlPoints(points.size() - 3);
            }
        }
        else {

            // Handle case with 2 anchor points separately, as otherwise minor adjustments cause path to constantly flip
            if (getNumAnchorPoints() == 2) {

                // points[1] = points[0] + (points[3] - points[0]) * .25f;
                points.set(1, points.get(0).add(points.get(3).subtract(points.get(0))).mult(0.25f));
                // points[2] = points[3] + (points[0] - points[3]) * .25f;
                points.set(2, points.get(3).add(points.get(0).subtract(points.get(3))).mult(0.25f));

            }
            else {

                // points[1] = (points[0] + points[2]) * .5f;
                points.set(1, points.get(0).add(points.get(2)).mult(0.5f));
                // points[points.Count - 2] = (points[points.Count - 1] + points[points.Count - 3]) * .5f;
                points.set(points.size() - 2, points.get(points.size() - 1).add(points.get(points.size() - 3)).mult(0.5f));

            }

        }

    }

    /**
     * Update point positions for new path space
     * (for example, if changing from xy to xz path, y and z axes will be swapped so the path keeps its shape in the new space)
     * @param previousSpace the previous PathSpace
     */
    private void updateToNewPathSpace(PathSpace previousSpace) {

        // If changing from 3d to 2d space, first find the bounds of the 3d path.
        // The axis with the smallest bounds will be discarded.

        if (previousSpace == PathSpace.xyz) {

            // @todo the boundsize might not be correct.
            Vector3f boundsSize = getPathBounds().getExtent(null);
            float minBoundsSize = Math.min(boundsSize.x, Math.min(boundsSize.y, boundsSize.z));

            for (int i = 0; i < getNumPoints(); i++) {

                if (space == PathSpace.xy) {

                    float x = (minBoundsSize == boundsSize.x) ? points.get(i).z : points.get(i).x;
                    float y = (minBoundsSize == boundsSize.y) ? points.get(i).z : points.get(i).y;

                    points.set(i, new Vector3f(x, y, 0));
                }
                else if (space == PathSpace.xz) {
                    float x = (minBoundsSize == boundsSize.x) ? points.get(i).y : points.get(i).x;
                    float z = (minBoundsSize == boundsSize.z) ? points.get(i).y : points.get(i).z;

                    points.set(i, new Vector3f(x, 0, z));
                }
            }
        }
        else {
            // Nothing needs to change when going to 3d space

            if (space != PathSpace.xyz) {

                for (int i = 0; i < getNumPoints(); i++) {

                    // from xz to xy
                    if (space == PathSpace.xy) {
                        points.set(i, new Vector3f(points.get(i).x, points.get(i).z, 0));
                    }
                    // from xy to xz
                    else if (space == PathSpace.xz) {
                        points.set(i, new Vector3f(points.get(i).x, 0, points.get(i).y));
                    }
                }
            }
        }

        notifyPathModified();
    }

    /**
     * Add/remove the extra 2 controls required for a closed path
     */
    private void updateClosedState() {

        if (isClosed) {

            // Set positions for new controls to mirror their counterparts

            // Vector3 lastAnchorSecondControl = points[points.Count - 1] * 2 - points[points.Count - 2];
            Vector3f lastAnchorSecondControl = points.get(points.size() - 1).mult(2).subtract(points.get(points.size() - 2));

            // Vector3 firstAnchorSecondControl = points[0] * 2 - points[1];
            Vector3f firstAnchorSecondControl = points.get(0).mult(2).subtract(points.get(1));

            if (controlMode != ControlMode.Mirrored && controlMode != ControlMode.Automatic) {
                // Set positions for new controls to be aligned with their counterparts, but with a length of half the distance between start/end anchor

                // float dstBetweenStartAndEndAnchors = (points[points.Count - 1] - points[0]).magnitude;
                float dstBetweenStartAndEndAnchors = points.get(points.size() - 1).subtract(points.get(0)).length();
                // lastAnchorSecondControl = points[points.Count - 1] + (points[points.Count - 1] - points[points.Count - 2]).normalized * dstBetweenStartAndEndAnchors * .5f;
                lastAnchorSecondControl = points.get(points.size() - 1).add(points.get(points.size() - 1)).subtract(points.get(points.size() - 2)).normalize().mult(dstBetweenStartAndEndAnchors).mult(0.5f);
                // firstAnchorSecondControl = points[0] + (points[0] - points[1]).normalized * dstBetweenStartAndEndAnchors * .5f;
                firstAnchorSecondControl = points.get(0).add(points.get(0).subtract(points.get(1)).normalize().mult(dstBetweenStartAndEndAnchors).mult(0.5f));
            }
            points.add(lastAnchorSecondControl);
            points.add(firstAnchorSecondControl);
        }
        else {

            int start = points.size() - 2;

            for (int i = start; i < start + 1; i++) {
                points.remove(i);
            }
        }

        if (controlMode == ControlMode.Automatic) {
            autoSetStartAndEndControls ();
        }

        if (!modifiedEvents.isEmpty()) {
            CreatorEvent.triggerEvents(modifiedEvents);
        }
    }

    /// Add new anchor point to start of the path
    public void addSegmentToStart (Vector3f anchorPos) {

        if (isClosed) {
            return;
        }

        // Set position for new control to be mirror of its counterpart
        Vector3f secondControlForOldFirstAnchorOffset = points.get(0).subtract(points.get(1));

        if (controlMode != ControlMode.Mirrored && controlMode != ControlMode.Automatic) {
            // Set position for new control to be aligned with its counterpart, but with a length of half the distance from prev to new anchor
            float dstPrevToNewAnchor = points.get(0).subtract(anchorPos).length();
            // secondControlForOldFirstAnchorOffset = secondControlForOldFirstAnchorOffset.normalized * dstPrevToNewAnchor * .5f;
            secondControlForOldFirstAnchorOffset = secondControlForOldFirstAnchorOffset.normalize().mult(dstPrevToNewAnchor).mult(0.5f);
        }

        Vector3f secondControlForOldFirstAnchor = points.get(0).add(secondControlForOldFirstAnchorOffset);
        Vector3f controlForNewAnchor = anchorPos.add(secondControlForOldFirstAnchor.mult(0.5f));
        points.add(0, anchorPos);
        points.add(1, controlForNewAnchor);
        points.add(2, secondControlForOldFirstAnchor);
        perAnchorNormalsAngle.add(0, perAnchorNormalsAngle.get(0));

        if (controlMode == ControlMode.Automatic) {
            autoSetAllAffectedControlPoints (0);
        }

        notifyPathModified ();
    }

    /// Add new anchor point to end of the path
    public void addSegmentToEnd (Vector3f anchorPos) {
        if (isClosed) {
            return;
        }

        int lastAnchorIndex = points.size() - 1;
        // Set position for new control to be mirror of its counterpart
        Vector3f secondControlForOldLastAnchorOffset = points.get(lastAnchorIndex).subtract(points.get(lastAnchorIndex - 1));
        if (controlMode != ControlMode.Mirrored && controlMode != ControlMode.Automatic) {
            // Set position for new control to be aligned with its counterpart, but with a length of half the distance from prev to new anchor
            float dstPrevToNewAnchor = points.get(lastAnchorIndex).subtract(anchorPos).length();
            // secondControlForOldLastAnchorOffset = (points[lastAnchorIndex] - points[lastAnchorIndex - 1]).normalized * dstPrevToNewAnchor * .5f;
            secondControlForOldLastAnchorOffset = points.get(lastAnchorIndex).subtract(points.get(lastAnchorIndex - 1)).normalize().mult(dstPrevToNewAnchor * 0.5f);
        }
        Vector3f secondControlForOldLastAnchor = points.get(lastAnchorIndex).add(secondControlForOldLastAnchorOffset);
        Vector3f controlForNewAnchor = anchorPos.add(secondControlForOldLastAnchor.mult(.5f));

        points.add(secondControlForOldLastAnchor);
        points.add(controlForNewAnchor);
        points.add(anchorPos);
        perAnchorNormalsAngle.add(perAnchorNormalsAngle.get(perAnchorNormalsAngle.size() - 1));

        if (controlMode == ControlMode.Automatic) {
            autoSetAllAffectedControlPoints (points.size() - 1);
        }

        notifyPathModified ();
    }

    /// Determines good positions (for a smooth path) for the control points affected by a moved/inserted anchor point
    private void autoSetAllAffectedControlPoints (int updatedAnchorIndex) {
        for (int i = updatedAnchorIndex - 3; i <= updatedAnchorIndex + 3; i += 3) {
            if (i >= 0 && i < points.size() || isClosed) {
                autoSetAnchorControlPoints(loopIndex (i));
            }
        }

        autoSetStartAndEndControls ();
    }

    /// Move an existing point to a new position
    public void movePoint (int i, Vector3f pointPos, boolean suppressPathModifiedEvent /* = false */) {

        if (space == PathSpace.xy) {
            pointPos.z = 0;
        } else if (space == PathSpace.xz) {
            pointPos.y = 0;
        }
        Vector3f deltaMove = pointPos.subtract(points.get(i)); // pointPos - points[i];
        boolean isAnchorPoint = i % 3 == 0;

        // Don't process control point if control mode is set to automatic
        if (isAnchorPoint || controlMode != ControlMode.Automatic) {
            // points[i] = pointPos;
            points.set(i, pointPos);

            if (controlMode == ControlMode.Automatic) {
                autoSetAllAffectedControlPoints(i);
            } else {
                // Move control points with anchor point
                if (isAnchorPoint) {
                    if (i + 1 < points.size() || isClosed) {
                        // points[LoopIndex (i + 1)] += deltaMove;
                        points.get(loopIndex (i + 1)).addLocal(deltaMove);
                    }
                    if (i - 1 >= 0 || isClosed) {
                        // points[LoopIndex (i - 1)] += deltaMove;
                        points.get(loopIndex (i - 1)).addLocal(deltaMove);
                    }
                }
                // If not in free control mode, then move attached control point to be aligned/mirrored (depending on mode)
                else if (controlMode != ControlMode.Free) {
                    boolean nextPointIsAnchor = (i + 1) % 3 == 0;
                    int attachedControlIndex = (nextPointIsAnchor) ? i + 2 : i - 2;
                    int anchorIndex = (nextPointIsAnchor) ? i + 1 : i - 1;

                    if (attachedControlIndex >= 0 && attachedControlIndex < points.size() || isClosed) {
                        float distanceFromAnchor = 0;
                        // If in aligned mode, then attached control's current distance from anchor point should be maintained
                        if (controlMode == ControlMode.Aligned) {
                            // distanceFromAnchor = (points[loopIndex (anchorIndex)] - points[loopIndex (attachedControlIndex)]).magnitude;
                            distanceFromAnchor = points.get(loopIndex(anchorIndex)).subtract(points.get(loopIndex(attachedControlIndex))).length();
                        }
                        // If in mirrored mode, then both control points should have the same distance from the anchor point
                        else if (controlMode == ControlMode.Mirrored) {
                            // distanceFromAnchor = (points[loopIndex (anchorIndex)] - points[i]).magnitude;
                            distanceFromAnchor = points.get(loopIndex(anchorIndex)).subtract(points.get(i)).length();

                        }
                        // Vector3f dir = (points[loopIndex (anchorIndex)] - pointPos).normalized;
                        Vector3f dir = points.get(loopIndex(anchorIndex)).subtract(pointPos).normalize();
                        // points[loopIndex (attachedControlIndex)] = points[loopIndex (anchorIndex)] + dir * distanceFromAnchor;
                        points.set(loopIndex(attachedControlIndex), points.get(loopIndex(anchorIndex)).add(dir.mult(distanceFromAnchor)));
                    }
                }
            }

            if (!suppressPathModifiedEvent) {
                notifyPathModified ();
            }
        }
    }

    public ControlMode getControlMode() {
        return controlMode;
    }

    public void setControlMode(ControlMode controlMode) {
        if (this.controlMode != controlMode) {
            this.controlMode = controlMode;
            if (controlMode == ControlMode.Automatic) {
                autoSetAllControlPoints();
                notifyPathModified();
            }
        }
    }

    /// Determines good positions (for a smooth path) for all control points
    void autoSetAllControlPoints () {
        if (getNumAnchorPoints() > 2) {
            for (int i = 0; i < points.size(); i += 3) {
                autoSetAnchorControlPoints(i);
            }
        }

        autoSetStartAndEndControls ();
    }

    /// Global angle that all normal vectors are rotated by (only relevant for paths in 3D space)
    public float getGlobalNormalsAngle() {
        return globalNormalsAngle;
    }

    public void setGlobalNormalsAngle(float angle) {
        if (angle != globalNormalsAngle) {
            globalNormalsAngle = angle;
            notifyPathModified();
        }
    }

    /// Get the desired angle of the normal vector at a particular anchor (only relevant for paths in 3D space)
    public float getAnchorNormalAngle (int anchorIndex) {
        return perAnchorNormalsAngle.get(anchorIndex) % 360;
    }

    /// Number of anchor points making up the path
    public int getNumAnchorPoints() {
        return (isClosed()) ? points.size() / 3 : (points.size() + 2) / 3;
    }

    /// Loop index around to start/end of points array if out of bounds (useful when working with closed paths)
    private int loopIndex (int i) {
        return (i + points.size()) % points.size();
    }

    // Called when the path is modified
    public void notifyPathModified() {
        boundsUpToDate = false;

        if (!modifiedEvents.isEmpty()) {
            CreatorEvent.triggerEvents(modifiedEvents);
        }
    }

}
