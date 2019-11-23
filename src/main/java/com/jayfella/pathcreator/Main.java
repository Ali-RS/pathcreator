package com.jayfella.pathcreator;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;
import com.simsilica.lemur.style.StyleLoader;

public class Main extends SimpleApplication {

    public static void main(String[] args) {

        Main app = new Main();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("My Awesome Game");
        settings.setResolution(1280, 720);
        settings.setFrameRate(120);

        app.setSettings(settings);

        app.setShowSettings(false);
        app.start();

    }

    @Override
    public void simpleInitApp() {

        rootNode.addLight(new DirectionalLight(new Vector3f(-1, -1, -1).normalizeLocal()));

        cam.setLocation(new Vector3f(0, 5, 15));
        cam.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(5);

        viewPort.setBackgroundColor(new ColorRGBA(0.5f, 0.6f, 0.7f, 1.0f));

        // 45 degrees is standard.
        float fov = 45;
        float aspect = (float)cam.getWidth() / (float)cam.getHeight();
        cam.setFrustumPerspective(fov, aspect, 0.1f, 10000);

        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        Geometry box = new Geometry("Box", new Box(.1f,.1f,.1f));
        box.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        rootNode.attachChild(box);

        PathCreator pathCreator = new PathCreator();

        rootNode.attachChild(pathCreator.getNode());

        PathEditorState pathEditorState = new PathEditorState(pathCreator);
        stateManager.attach(pathEditorState);

        addGrid(100, 100, 1);
    }

    private void addGrid(int sizeX, int sizeZ, float lineDist) {

        Geometry gridGeom = new Geometry("Grid", new Grid(sizeX, sizeZ, lineDist));
        gridGeom.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        gridGeom.getMaterial().setColor("Color", ColorRGBA.Gray);
        gridGeom.setLocalTranslation(-sizeX / 2f, 0, -sizeZ / 2f);
        rootNode.attachChild(gridGeom);
    }


}