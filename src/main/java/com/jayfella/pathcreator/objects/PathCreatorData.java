package com.jayfella.pathcreator.objects;

import com.jayfella.pathcreator.event.CreatorEvent;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

public class PathCreatorData {

    private CreatorEvent bezierPathEdited;

    private CreatorEvent bezierOrVertexPathModified;
    private CreatorEvent bezierCreated;

    private boolean vertexPathUpToDate = false;

    // vertex path settings
    public float vertexPathMaxAngleError = .3f;
    public float vertexPathMinVertexSpacing = 0.01f;

    private BezierPath bezierPath;
    private VertexPath vertexPath;

    public PathCreatorData(boolean defaultIs2D) {

        bezierPathEdited = this::bezierPathEdited;

        createBezier (new Vector3f(), defaultIs2D);
        vertexPathUpToDate = false;

        bezierPath.addModifiedEvent(bezierPathEdited);
    }

    private void createBezier(Vector3f center) {
        createBezier(center, false);
    }

    private void createBezier(Vector3f center, boolean defaultIs2D) {

        if (bezierPath != null) {
            // bezierPath.OnModified -= BezierPathEdited;
            bezierPath.removeModifiedEvent(bezierPathEdited);
        }

        PathSpace space = (defaultIs2D) ? PathSpace.xy : PathSpace.xyz;
        bezierPath = new BezierPath (center, false, space);

        // bezierPath.OnModified += BezierPathEdited;
        bezierPath.addModifiedEvent(bezierPathEdited);
        vertexPathUpToDate = false;

        if (bezierOrVertexPathModified != null) {
            bezierOrVertexPathModified.eventTriggered();
        }
        if (bezierCreated != null) {
            bezierCreated.eventTriggered();
        }

    }

    public void setBezierOrVertexPathModified(CreatorEvent event) {
        this.bezierOrVertexPathModified = event;
    }

    public BezierPath getBezierPath() {
        return bezierPath;
    }

    // get the current vertex path
    public VertexPath getVertexPath(Spatial spatial) {

        if (!vertexPathUpToDate || vertexPath == null) {

            vertexPathUpToDate = true;
            vertexPath = new VertexPath (bezierPath, spatial, vertexPathMaxAngleError, vertexPathMinVertexSpacing);

        }

        return vertexPath;
    }

    public void setBezierPath(BezierPath bezierPath) {

        if (this.bezierPath != null) {
            this.bezierPath.removeModifiedEvent(bezierPathEdited);
            this.vertexPathUpToDate = false;
            this.bezierPath = bezierPath;
            this.bezierPath.addModifiedEvent(bezierPathEdited);
        }

    }

    private void bezierPathEdited () {
        vertexPathUpToDate = false;
        if (bezierOrVertexPathModified != null) {
            bezierOrVertexPathModified.eventTriggered();
        }
    }

    public float getVertexPathMaxAngleError() {
        return vertexPathMaxAngleError;
    }

    public void setVertexPathMaxAngleError(float vertexPathMaxAngleError) {
        if (this.vertexPathMaxAngleError != vertexPathMaxAngleError) {
            this.vertexPathMaxAngleError = vertexPathMaxAngleError;
            bezierPathEdited();
        }
    }

    public float getVertexPathMinVertexSpacing() {
        return vertexPathMinVertexSpacing;
    }

    public void setVertexPathMinVertexSpacing(float vertexPathMinVertexSpacing) {
        if (this.vertexPathMinVertexSpacing != vertexPathMinVertexSpacing) {
            this.vertexPathMinVertexSpacing = vertexPathMinVertexSpacing;
            bezierPathEdited();
        }
    }
}
