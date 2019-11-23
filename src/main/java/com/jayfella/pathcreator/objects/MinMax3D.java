package com.jayfella.pathcreator.objects;

import com.jme3.math.Vector3f;

public class MinMax3D {

    private final Vector3f min;
    private final Vector3f max;

    public MinMax3D()
    {
        // Min = Vector3.one * float.MaxValue;
        // Max = Vector3.one * float.MinValue;
        this.min = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        this.max = new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
    }

    public Vector3f getMin() {
        return min;
    }

    public Vector3f getMax() {
        return max;
    }

    public void addValue(Vector3f v)
    {
        // Min = new Vector3(Mathf.Min(Min.x, v.x), Mathf.Min(Min.y,v.y), Mathf.Min(Min.z,v.z));
        min.set(Math.min(min.x, v.x), Math.min(min.y, v.y), Math.min(min.z, v.z));

        // Max = new Vector3(Mathf.Max(Max.x, v.x), Mathf.Max(Max.y,v.y), Mathf.Max(Max.z,v.z));
        max.set(Math.max(max.x, v.x), Math.max(max.y, v.y), Math.max(max.z, v.z));
    }

}
