package com.jayfella.pathcreator.ext;

import com.jayfella.pathcreator.PathCreator;
import com.jayfella.pathcreator.objects.PathSpace;
import com.jayfella.pathcreator.objects.VertexPath;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

public class RoadMesh {

    private final PathCreator pathCreator;
    private final AssetManager assetManager;

    private float roadWidth = .4f;
    private float thickness = .15f;
    private boolean flattenSurface;

    private Material roadMaterial;
    private Material undersideMaterial;
    private float textureTiling = 1;

    private Geometry geometry;

    public RoadMesh(PathCreator pathCreator, AssetManager assetManager) {
        this.pathCreator = pathCreator;
        this.assetManager = assetManager;
        createRoadMesh();

        pathCreator.getBezierPath().addModifiedEvent(this::updateMesh);
    }

    public void updateMesh() {
        createRoadMesh();
    }

    private void createRoadMesh() {

        // BezierPath path = pathCreator.getBezierPath();
        VertexPath path = pathCreator.getVertexPath();

        Vector3f[] verts = new Vector3f[path.getNumPoints() * 8];
        Vector2f[] uvs = new Vector2f[verts.length];
        Vector3f[] normals = new Vector3f[verts.length];

        int numTris = 2 * (path.getNumPoints() - 1) + ((path.isClosedLoop()) ? 2 : 0);
        int[] roadTriangles = new int[numTris * 3];
        int[] underRoadTriangles = new int[numTris * 3];
        int[] sideOfRoadTriangles = new int[numTris * 2 * 3];

        int vertIndex = 0;
        int triIndex = 0;

        // Vertices for the top of the road are layed out:
        // 0  1
        // 8  9
        // and so on... So the triangle map 0,8,1 for example, defines a triangle from top left to bottom left to bottom right.
        int[] triangleMap = { 0, 8, 1, 1, 8, 9 };
        int[] sidesTriangleMap = { 4, 6, 14, 12, 4, 14, 5, 15, 7, 13, 15, 5 };

        boolean usePathNormals = !(path.getSpace() == PathSpace.xyz && flattenSurface);

        for (int i = 0; i < path.getNumPoints(); i++) {

            // Vector3f localUp = (usePathNormals) ? Vector3.Cross (path.GetTangent (i), path.GetNormal (i)) : path.up;
            Vector3f localUp = (usePathNormals) ? path.getTangent(i).cross(path.getNormal(i)) : path.getUp();
            // Vector3f localRight = (usePathNormals) ? path.GetNormal (i) : Vector3.Cross (localUp, path.GetTangent (i));
            Vector3f localRight = (usePathNormals) ? path.getNormal(i) : localUp.cross(path.getTangent(i));

            // Find position to left and right of current path vertex
            Vector3f vertSideA = path.getPoint(i).subtract(localRight.mult(Math.abs(roadWidth)));
            Vector3f vertSideB = path.getPoint(i).add(localRight.mult(Math.abs(roadWidth)));

            // Add top of road vertices
            verts[vertIndex + 0] = vertSideA;
            verts[vertIndex + 1] = vertSideB;
            // Add bottom of road vertices
            verts[vertIndex + 2] = vertSideA.subtract(localUp.mult(thickness));
            verts[vertIndex + 3] = vertSideB.subtract(localUp.mult(thickness));

            // Duplicate vertices to get flat shading for sides of road
            verts[vertIndex + 4] = verts[vertIndex + 0];
            verts[vertIndex + 5] = verts[vertIndex + 1];
            verts[vertIndex + 6] = verts[vertIndex + 2];
            verts[vertIndex + 7] = verts[vertIndex + 3];

            // Set uv on y axis to path time (0 at start of path, up to 1 at end of path)
            uvs[vertIndex + 0] = new Vector2f (0, path.getTime(i));
            uvs[vertIndex + 1] = new Vector2f (1, path.getTime(i));

            // Top of road normals
            normals[vertIndex + 0] = localUp;
            normals[vertIndex + 1] = localUp;
            // Bottom of road normals
            normals[vertIndex + 2] = localUp.negate();
            normals[vertIndex + 3] = localUp.negate();
            // Sides of road normals
            normals[vertIndex + 4] = localRight.negate();
            normals[vertIndex + 5] = localRight;
            normals[vertIndex + 6] = localRight.negate();
            normals[vertIndex + 7] = localRight;

            // Set triangle indices
            if (i < path.getNumPoints() - 1 || path.isClosedLoop()) {
                for (int j = 0; j < triangleMap.length; j++) {


                    // roadTriangles[triIndex + j] = (vertIndex + triangleMap[j]) % verts.length;
                    roadTriangles[triIndex + j] = (vertIndex + triangleMap[triangleMap.length - 1 - j] + 2) % verts.length;

                    // reverse triangle map for under road so that triangles wind the other way and are visible from underneath
                    underRoadTriangles[triIndex + j] = (vertIndex + triangleMap[triangleMap.length - 1 - j] + 2) % verts.length;
                }
                for (int j = 0; j < sidesTriangleMap.length; j++) {
                    sideOfRoadTriangles[triIndex * 2 + j] = (vertIndex + sidesTriangleMap[j]) % verts.length;
                }

            }

            vertIndex += 8;
            triIndex += 6;
        }

        if (geometry == null) {
            geometry = new Geometry("Road Geometry", new Mesh());
            geometry.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
            geometry.getMaterial().setColor("Color", ColorRGBA.Yellow);
            geometry.getMaterial().getAdditionalRenderState().setWireframe(true);
        }


        FloatBuffer pb = BufferUtils.createFloatBuffer(verts);
        geometry.getMesh().setBuffer(VertexBuffer.Type.Position, 3, pb);

        // indexes
        int[] allIndexes = new int[roadTriangles.length + underRoadTriangles.length + sideOfRoadTriangles.length];
        int idx = 0;

        for (int i = 0; i < roadTriangles.length; i++) allIndexes[idx++] = roadTriangles[i];
        for (int i = 0; i < underRoadTriangles.length; i++) allIndexes[idx++] = underRoadTriangles[i];
        for (int i = 0; i < sideOfRoadTriangles.length; i++) allIndexes[idx++] = sideOfRoadTriangles[i];

        IntBuffer ib = BufferUtils.createIntBuffer(allIndexes);
        geometry.getMesh().setBuffer(VertexBuffer.Type.Index, 3, ib);


        FloatBuffer nb = BufferUtils.createFloatBuffer(normals);
        geometry.getMesh().setBuffer(VertexBuffer.Type.Normal, 3, nb);

        FloatBuffer tb = BufferUtils.createFloatBuffer(uvs);
        geometry.getMesh().setBuffer(VertexBuffer.Type.TexCoord, 2, tb);

        geometry.updateModelBound();

        /*
        mesh.Clear ();
        mesh.vertices = verts;
        mesh.uv = uvs;
        mesh.normals = normals;
        mesh.subMeshCount = 3;
        mesh.SetTriangles (roadTriangles, 0);
        mesh.SetTriangles (underRoadTriangles, 1);
        mesh.SetTriangles (sideOfRoadTriangles, 2);
        mesh.RecalculateBounds ();
         */

    }

    public Geometry getGeometry() {
        return geometry;
    }

}
