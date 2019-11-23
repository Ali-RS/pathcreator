package com.jayfella.pathcreator.editor;

import com.jayfella.pathcreator.PathCreator;
import com.jayfella.pathcreator.PathEditorState;
import com.jayfella.pathcreator.ext.RoadMesh;
import com.jayfella.pathcreator.objects.BezierPath;
import com.jayfella.pathcreator.objects.PathCreatorData;
import com.simsilica.lemur.Checkbox;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.RollupPanel;
import com.simsilica.lemur.TabbedPanel;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.props.PropertyPanel;

public class EditorGui extends TabbedPanel {

    private final PathEditorState editorState;
    private final PathCreator pathCreator;

    private VersionedReference<Boolean> showRoadsRef;

    public EditorGui(PathEditorState editorState, PathCreator pathCreator) {
        super();

        this.editorState = editorState;
        this.pathCreator = pathCreator;

        createBezierPathTab();
        createVertexPathTab();
        createRoadTab();
    }

    private void createBezierPathTab() {

        Container container = new Container();

        BezierPath path = pathCreator.getEditorData().getBezierPath();

        // Bezier Path Options
        PropertyPanel pathOptions = new PropertyPanel("glass");
        pathOptions.addEnumProperty("Space", path, "space");
        pathOptions.addEnumProperty("Control Mode", path, "controlMode");
        pathOptions.addBooleanProperty("Closed Path", path, "closed");

        RollupPanel pathOptionsRollup = container.addChild(new RollupPanel("Bezier Path Options", pathOptions, "glass"));
        pathOptionsRollup.setOpen(true);

        // Normal Options
        PropertyPanel normalOptions = new PropertyPanel("glass");
        normalOptions.addBooleanProperty("Flip", path, "flipNormals");

        RollupPanel normalOptionsRollup = container.addChild(new RollupPanel("Normal Options", normalOptions, "glass"));
        normalOptionsRollup.setOpen(false);

        // Display Options
        PropertyPanel displayOptions = new PropertyPanel("glass");
        displayOptions.addBooleanProperty("Show Anchor Points", editorState, "showAnchorPoints");
        displayOptions.addBooleanProperty("Show Control Points", editorState, "showControlPoints");
        displayOptions.addBooleanProperty("Show Path Bounds", editorState, "showPathBounds");
        displayOptions.addBooleanProperty("Show Segment Bounds", editorState, "showSegmentBounds");

        RollupPanel displayOptionsRollup = container.addChild(new RollupPanel("Display Options", displayOptions, "glass"));
        displayOptionsRollup.setOpen(false);

        addTab("Bezier Path", container);

    }

    private void createVertexPathTab() {

        Container container = new Container();

        // VertexPath path = pathCreator.getVertexPath();
        PathCreatorData pathCreatorData = pathCreator.getEditorData();

        PropertyPanel vertexPathOptions = new PropertyPanel("glass");
        vertexPathOptions.addFloatProperty("Max Angle Error", pathCreatorData, "vertexPathMaxAngleError", 0, 45, 0.01f);
        vertexPathOptions.addFloatProperty("Min Vertex Dst", pathCreatorData, "vertexPathMinVertexSpacing", 0, 1, 0.01f);

        RollupPanel vertexOptionsRollup = container.addChild(new RollupPanel("Vertex Path Options", vertexPathOptions, "glass"));

        addTab("Vertex Path", container);
    }

    private void createRoadTab() {

        Container container = new Container();

        Checkbox showRoadsCheckBox = container.addChild(new Checkbox("Show Roads"));
        showRoadsRef = showRoadsCheckBox.getModel().createReference();

        addTab("Road", container);
    }

    private RoadMesh roadMesh;

    public void update() {

        if (showRoadsRef.update()) {

            boolean value = showRoadsRef.get();

            if (value) {
                if (roadMesh == null) {
                    roadMesh = new RoadMesh(pathCreator, editorState.getApplication().getAssetManager());
                }

                pathCreator.getNode().attachChild(roadMesh.getGeometry());
            }
            else {
                if (roadMesh != null) {
                    roadMesh.getGeometry().removeFromParent();
                }
            }

        }

    }

}
