package com.jayfella.pathcreator;

import com.jayfella.pathcreator.editor.EditorGui;
import com.jayfella.pathcreator.objects.BezierPath;
import com.jayfella.pathcreator.objects.VertexPath;
import com.jayfella.pathcreator.utility.CubicBezierUtility;
import com.jayfella.pathcreator.utility.MathUtility;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bounding.BoundingBox;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.input.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class PathEditorState extends BaseAppState implements StateFunctionListener {

    private static final String G_PATH_EDITOR = "GROUP_EDITOR";
    private static final FunctionId F_ADD_SEGMENT_START = new FunctionId(G_PATH_EDITOR, "Add Segment To Start");
    private static final FunctionId F_ADD_SEGMENT_END = new FunctionId(G_PATH_EDITOR, "Add Segment To End");
    private static final FunctionId F_MOVE_SEGMENT = new FunctionId(G_PATH_EDITOR, "Move Segment");

    private final PathCreator pathCreator;

    private final Node node = new Node("Path Editor");
    private final Node node_handles = new Node("Handles");
    private final Node node_anchors = new Node("Anchors");
    private final Node node_controlPoints = new Node("Control Points");
    private final Node node_controlPointLines = new Node("Control Point Lines");
    private final Node node_segmentBounds = new Node("Segment Bounds");

    private Geometry bezierGeom;

    private final EditorGui editorGui;

    private boolean showAnchorPoints = true;
    private boolean showControlPoints = true;
    private boolean showPathBounds = false;
    private boolean showSegmentBounds = false;

    private Material handleMaterial;
    private Material controlMaterial;

    private static final float constantHandleScale = .01f;
    private static final float anchorSize = 0.1f;
    private static final float controlPointSize = 0.05f;
    private static final float bezierHandleScale = 1.0f;

    // used to find and store the ID's of anchors and control points that are being hovered over.
    private CollisionResult handleCollision;
    private int mouseOverHandleIndex = -1;
    private boolean draggingHandle = false;

    public PathEditorState(PathCreator pathCreator) {

        node_handles.attachChild(node_anchors);
        node_handles.attachChild(node_controlPoints);

        node.attachChild(node_handles);
        node.attachChild(node_controlPointLines);
        node.attachChild(node_segmentBounds);

        this.pathCreator = pathCreator;
        this.pathCreator.getEditorData().setBezierOrVertexPathModified(this::repaint);

        this.editorGui = new EditorGui(this, pathCreator);
    }

    @Override
    protected void initialize(Application app) {

        // bezier curve line
        Material bezierMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bezierMaterial.setColor("Color", ColorRGBA.Green);
        bezierMaterial.getAdditionalRenderState().setDepthTest(false);

        // a geom/mesh to draw the bezier curve as one complete mesh.
        bezierGeom = new Geometry("Bezier Geometry", new Mesh());
        bezierGeom.getMesh().setMode(Mesh.Mode.Lines);
        bezierGeom.setMaterial(bezierMaterial);

        // anchor points
        handleMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        handleMaterial.setColor("Color", ColorRGBA.Red);
        handleMaterial.getAdditionalRenderState().setDepthTest(false);

        // control points
        controlMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        controlMaterial.setColor("Color", ColorRGBA.Blue);
        controlMaterial.getAdditionalRenderState().setDepthTest(false);

        // put the editor GUI in the top-left corner.
        editorGui.setLocalTranslation(10, getApplication().getCamera().getHeight() - 10, 0);

        // any time the bezier curve is modified, repaint it.
        pathCreator.getBezierPath().addModifiedEvent(this::repaint);
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {
        ((SimpleApplication)getApplication()).getRootNode().attachChild(node);
        ((SimpleApplication)getApplication()).getGuiNode().attachChild(editorGui);

        bindInput();

        repaint();
    }

    @Override
    protected void onDisable() {
        node.removeFromParent();
        editorGui.removeFromParent();

        unbindInput();
    }

    public void repaint() {
        drawVertexPath();
        drawBezierAnchors();
        drawBezierControlPoints();
        drawBezierControlPointLines();
        drawPathBounds();
        drawSegmentBounds();
    }

    public boolean isShowAnchorPoints() {
        return showAnchorPoints;
    }

    public void setShowAnchorPoints(boolean showAnchorPoints) {
        if (this.showAnchorPoints != showAnchorPoints) {
            this.showAnchorPoints = showAnchorPoints;
            repaint();
        }
    }

    public boolean isShowControlPoints() {
        return showControlPoints;
    }

    public void setShowControlPoints(boolean showControlPoints) {
        if (this.showControlPoints != showControlPoints) {
            this.showControlPoints = showControlPoints;
            repaint();
        }

    }

    public boolean isShowPathBounds() {
        return showPathBounds;
    }

    public void setShowPathBounds(boolean showPathBounds) {
        if (this.showPathBounds != showPathBounds) {
            this.showPathBounds = showPathBounds;
            repaint();
        }

    }

    public boolean isShowSegmentBounds() {
        return showSegmentBounds;
    }

    public void setShowSegmentBounds(boolean showSegmentBounds) {
        if (this.showSegmentBounds != showSegmentBounds) {
            this.showSegmentBounds = showSegmentBounds;
            repaint();
        }
    }

    private void drawVertexPath() {

        VertexPath path = pathCreator.getVertexPath();

        if (path != null) {

            path.updateTransform(pathCreator.getNode().getLocalTransform());

            List<Vector3f> verts = new ArrayList<>();

            for (int i = 0; i < path.getNumPoints(); i++) {

                int nextI = i + 1;

                if (nextI >= path.getNumPoints()) {

                    if (path.isClosedLoop()) {

                        nextI %= path.getNumPoints();

                    }
                    else {
                        break;
                    }

                }

                verts.add(path.getPoint(i));
                verts.add(path.getPoint(nextI));
            }

            if (verts.size() > 0) {

                Vector3f[] vertArray = verts.toArray(new Vector3f[0]);

                FloatBuffer pb = BufferUtils.createFloatBuffer(vertArray);
                bezierGeom.getMesh().setBuffer(VertexBuffer.Type.Position, 3, pb);

                bezierGeom.updateModelBound();
            }

            if (bezierGeom.getMesh().getVertexCount() > 0) {
                node.attachChild(bezierGeom);
            }

        }
    }

    private void drawBezierAnchors() {

        node_anchors.detachAllChildren();

        if (showAnchorPoints) {

            for (int i = 0; i < pathCreator.getBezierPath().getNumPoints(); i += 3) {
                drawHandle(i, HandleType.Anchor);
            }

        }

    }

    private void drawBezierControlPoints() {

        node_controlPoints.detachAllChildren();

        if (showControlPoints) {

            for (int i = 1; i < pathCreator.getBezierPath().getNumPoints() - 1; i += 3) {

                drawHandle(i, HandleType.ControlPoint);
                drawHandle(i + 1, HandleType.ControlPoint);
            }

        }

    }

    private void drawBezierControlPointLines() {

        node_controlPointLines.detachAllChildren();

        if (showControlPoints) {

            BezierPath path = pathCreator.getBezierPath();

            for (int i = 0; i < path.getNumPoints(); i++) {

                Vector3f[] points = path.getPointsInSegment(i);

                for (int j = 0; j < points.length; j++) {
                    points[j] = MathUtility.TransformPoint (points[j], pathCreator.getNode(), path.getSpace());
                }

                Mesh mesh1 = new Mesh();
                mesh1.setMode(Mesh.Mode.Lines);

                Mesh mesh2 = new Mesh();
                mesh2.setMode(Mesh.Mode.Lines);

                FloatBuffer pb1 = BufferUtils.createFloatBuffer(points[1], points[0]);
                FloatBuffer pb2 = BufferUtils.createFloatBuffer(points[2], points[3]);

                mesh1.setBuffer(VertexBuffer.Type.Position, 3, pb1);
                mesh2.setBuffer(VertexBuffer.Type.Position, 3, pb2);

                Geometry geom1 = new Geometry("Control Point Line", mesh1);
                Geometry geom2 = new Geometry("Control Point Line", mesh2);

                geom1.updateModelBound();
                geom2.updateModelBound();

                geom1.setMaterial(controlMaterial);
                geom2.setMaterial(controlMaterial);

                node_controlPointLines.attachChild(geom1);
                node_controlPointLines.attachChild(geom2);

            }
        }
    }

    private Geometry pathBoundsGeom = null;
    private WireBox pathBoundsBox = null;
    private Material boundsMaterial = null;

    private void drawPathBounds() {

        if (showPathBounds) {

            BoundingBox boundingBox = (BoundingBox)bezierGeom.getWorldBound();
            Vector3f center = boundingBox.getCenter();
            Vector3f extent = boundingBox.getExtent(null);

            if (boundsMaterial == null) {
                boundsMaterial = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                boundsMaterial.setColor("Color", ColorRGBA.White);
            }

            if (pathBoundsGeom == null) {
                pathBoundsGeom = new Geometry("Path Bounds");
                pathBoundsGeom.setMaterial(boundsMaterial);
            }

            if (pathBoundsBox == null) {
                pathBoundsBox = new WireBox(extent.x, extent.y, extent.z);
                pathBoundsGeom.setMesh(pathBoundsBox);
            }
            else {
                pathBoundsBox.updatePositions(extent.x, extent.y, extent.z);
            }

            pathBoundsGeom.updateModelBound();
            pathBoundsGeom.setLocalTranslation(center);

            node.attachChild(pathBoundsGeom);
        }
        else {
            if (pathBoundsGeom != null) {
                pathBoundsGeom.removeFromParent();

                pathBoundsGeom = null;
                pathBoundsBox = null;
            }
        }

    }

    private void drawSegmentBounds() {

        node_segmentBounds.detachAllChildren();

        if (showSegmentBounds) {

            BezierPath path = pathCreator.getBezierPath();

            for (int i = 0; i < path.getNumSegments(); i++) {

                Vector3f[] points = path.getPointsInSegment(i);
                for (int j = 0; j < points.length; j++) {
                    points[j] = MathUtility.TransformPoint (points[j], pathCreator.getNode(), path.getSpace());
                }

                BoundingBox segmentBounds = CubicBezierUtility.calculateSegmentBounds(points[0], points[1], points[2], points[3]);
                Vector3f center = segmentBounds.getCenter();
                Vector3f extent = segmentBounds.getExtent(null);
                WireBox box = new WireBox(extent.x, extent.y, extent.z);

                if (boundsMaterial == null) {
                    boundsMaterial = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                    boundsMaterial.setColor("Color", ColorRGBA.White);
                }

                Geometry segmentBoundsGeom = new Geometry("Segment " + i + " Bounds", box);
                // segmentBoundsGeom.updateModelBound();
                segmentBoundsGeom.setLocalTranslation(center);
                segmentBoundsGeom.setMaterial(boundsMaterial);

                node_segmentBounds.attachChild(segmentBoundsGeom);
            }

        }

    }

    /**
     * Uses a ray to find the ID of the handle being hovered over.
     */
    private void findHoveredHandle() {

        // If the user is dragging and they move over another element we grab that ID, and we don't want that.
        if (draggingHandle) {
            return;
        }

        // our scene is organized in such a way that we don't collide with the bezier curve or control point lines.
        handleCollision = findMouseCollision(node_handles);

        // even though we should never collide with anything other than a handle, we should still do some error-control.
        if (handleCollision != null) {

            Integer index = handleCollision.getGeometry().getUserData("ID");
            mouseOverHandleIndex = (index == null) ? -1 : index;

            // System.out.println("Hover Index: " + mouseOverHandleIndex);
        }
        else {
            mouseOverHandleIndex = -1;
        }

    }

    @Override
    public void update(float tpf) {

        editorGui.update();

        // set the mouseOverHandleIndex field to the index of the hovered handle (anchor or control point).
        findHoveredHandle();

        if (draggingHandle) {
            if (mouseOverHandleIndex != -1) {

                BezierPath path = pathCreator.getBezierPath();

                Vector3f newPoint = calcPathPoint(mouseOverHandleIndex);
                Vector3f point = path.getPoint(mouseOverHandleIndex);
                Vector3f localHandlePosition = newPoint.subtract(point);

                // localHandlePosition = MathUtility.InverseTransformPoint(localHandlePosition, pathCreator.getNode(), path.getSpace());

                if (!localHandlePosition.equals(point)) {

                    // System.out.println("Moving: " + mouseOverHandleIndex + " to " + localHandlePosition);

                    pathCreator.getBezierPath().movePoint(mouseOverHandleIndex, localHandlePosition, false);
                    repaint();
                }
            }
        }

    }

    private final CollisionResults collisionResults = new CollisionResults();
    private final Ray mouseCollisionRay = new Ray();

    private CollisionResult findMouseCollision(Node collisionNode) {

        Vector2f mouseScreenCoords = getApplication().getInputManager().getCursorPosition();
        Vector3f mouseWorldCoords = getApplication().getCamera().getWorldCoordinates(mouseScreenCoords, 0);
        Vector3f mouseWorldCoords2 = getApplication().getCamera().getWorldCoordinates(mouseScreenCoords, 1);
        Vector3f mouseWorldDirection = mouseWorldCoords2.subtractLocal(mouseWorldCoords).normalizeLocal();

        // Ray ray = new Ray(mouseWorldCoords, mouseWorldDirection);
        mouseCollisionRay.setOrigin(mouseWorldCoords);
        mouseCollisionRay.setDirection(mouseWorldDirection);

        collisionNode.collideWith(mouseCollisionRay, collisionResults);

        if (collisionResults.size() > 0) {
            CollisionResult result = collisionResults.getClosestCollision();
            collisionResults.clear();
            return result;
        }

        return null;
    }

    private void bindInput() {

        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();

        inputMapper.map(F_ADD_SEGMENT_START, KeyInput.KEY_LSHIFT, KeyInput.KEY_LCONTROL, Button.MOUSE_BUTTON2);
        inputMapper.map(F_ADD_SEGMENT_END, KeyInput.KEY_LSHIFT, Button.MOUSE_BUTTON2);
        inputMapper.map(F_MOVE_SEGMENT, Button.MOUSE_BUTTON2);

        inputMapper.addStateListener(this, F_ADD_SEGMENT_START, F_ADD_SEGMENT_END, F_MOVE_SEGMENT);
        inputMapper.activateGroup(G_PATH_EDITOR);
    }

    private void unbindInput() {
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();

        inputMapper.removeMapping(F_ADD_SEGMENT_START, KeyInput.KEY_LSHIFT, KeyInput.KEY_LCONTROL, Button.MOUSE_BUTTON2);
        inputMapper.removeMapping(F_ADD_SEGMENT_END, KeyInput.KEY_LSHIFT, Button.MOUSE_BUTTON2);
        inputMapper.removeMapping(F_MOVE_SEGMENT, Button.MOUSE_BUTTON2);

        inputMapper.removeStateListener(this, F_ADD_SEGMENT_START, F_ADD_SEGMENT_END, F_MOVE_SEGMENT);
        inputMapper.deactivateGroup(G_PATH_EDITOR);
    }

    @Override
    public void valueChanged(FunctionId func, InputState value, double tpf) {

        if (func == F_ADD_SEGMENT_START) {
            if (value == InputState.Off) {
                Vector3f newPathPoint = calcPathPoint();
                pathCreator.getBezierPath().addSegmentToStart(newPathPoint);

                repaint();
            }
        }
        else if (func == F_ADD_SEGMENT_END) {
            if (value == InputState.Off) {
                Vector3f newPathPoint = calcPathPoint();
                pathCreator.getBezierPath().addSegmentToEnd(newPathPoint);

                repaint();
            }
        }
        else if (func == F_MOVE_SEGMENT) {

            draggingHandle = value != InputState.Off;
        }
    }
    
    private Vector3f calcPathPoint() {
        return calcPathPoint(pathCreator.getBezierPath().getNumPoints() - 1);
    }
    
    private Vector3f calcPathPoint(int pointIndex) {

        BezierPath path = pathCreator.getBezierPath();

        float dstCamToEndpoint = getApplication().getCamera().getLocation()
                .subtract(path.getPoint(pointIndex)).length();

        Vector3f newPathPoint = getMouseWorldPosition(dstCamToEndpoint);

        return MathUtility.InverseTransformPoint (newPathPoint, pathCreator.getNode(), path.getSpace());

    }

    private Vector3f getMouseWorldPosition(float depthFor3dSpace /* = 10 */) {

        Vector2f mouseScreenCoords = getApplication().getInputManager().getCursorPosition();
        Vector3f mouseWorldCoords = getApplication().getCamera().getWorldCoordinates(mouseScreenCoords, 0);
        Vector3f mouseWorldCoords2 = getApplication().getCamera().getWorldCoordinates(mouseScreenCoords, 1);
        Vector3f mouseWorldDirection = mouseWorldCoords2.subtractLocal(mouseWorldCoords).normalizeLocal();

        return mouseWorldDirection
                .mult(depthFor3dSpace)
                .addLocal(getApplication().getCamera().getLocation());
    }

    private enum HandleType { Anchor, ControlPoint }

    private void drawHandle(int i, HandleType handleType) {

        BezierPath path = pathCreator.getBezierPath();

        Vector3f handlePosition = MathUtility.TransformPoint (path.getPoint(i), pathCreator.getNode(), path.getSpace());

        Geometry handleGeom; // = new Geometry("Handle " + i, new Sphere(8,8,0.1f));

        switch (handleType) {

            case Anchor: {
                handleGeom = new Geometry("Anchor " + i, new Sphere(8,8,anchorSize));
                handleGeom.setMaterial(handleMaterial);
                node_anchors.attachChild(handleGeom);
                break;
            }

            case ControlPoint: {
                handleGeom = new Geometry("Control Point " + i, new Sphere(8,8,controlPointSize));
                handleGeom.setMaterial(controlMaterial);
                node_controlPoints.attachChild(handleGeom);
                break;
            }

            default: throw new IllegalArgumentException("Unknown HandleType: " + handleType);
        }

        handleGeom.setUserData("ID", i);
        handleGeom.setLocalTranslation(handlePosition);

    }

}
