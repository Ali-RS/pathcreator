package com.jayfella.pathcreator;

import com.jayfella.pathcreator.objects.BezierPath;
import com.jayfella.pathcreator.objects.PathCreatorData;
import com.jayfella.pathcreator.objects.VertexPath;
import com.jme3.scene.Node;

public class PathCreator {

    private final Node node = new Node("Path Creator");
    private final PathCreatorData editorData;

    public PathCreator() {
        this.editorData = new PathCreatorData(false);
    }

    public Node getNode() {
        return node;
    }

    public PathCreatorData getEditorData() {
        return editorData;
    }

    public VertexPath getVertexPath() {
        return editorData.getVertexPath(node);
    }

    public BezierPath getBezierPath() {
        return editorData.getBezierPath();
    }

}
