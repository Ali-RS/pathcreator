package com.jayfella.pathcreator.utility;

import com.jme3.math.FastMath;

public class UnityMath {

    public static float deltaAngle( double angle1, double angle2 )
    {
        double diff = ( angle2 - angle1 + 180 ) % 360 - 180;
        return (float) (diff < -180 ? diff + 360 : diff);
    }

    /*
        Calculates the linear parameter t that produces the interpolant value within the range [a, b].

        The a and b values define the start and end of the line. Value is a location between a and b. Subtract a from
        both a and b and value to make a', b' and value'. This makes a' to be zero and b' and value' to be reduced.
        Finally divide value' by b'. This gives the InverseLerp amount.
     */
    public static float inverseLerp(float a, float b, float t) {
        b = b - a;
        return t / b;
    }

    // PingPongs the value t, so that it is never larger than length and never smaller than 0.
    public static float pingPong(float t, float length) {
        float L = 2 * length;
        float T = t % L;
        float x = L - T;
        return x;
    }

    /*
    Same as Lerp but makes sure the values interpolate correctly when they wrap around 360 degrees.
    The parameter t is clamped to the range [0, 1]. Variables a and b are assumed to be in degrees.
     */
    public static float lerpAngle(float a, float b, float t) {
        float lerp = FastMath.interpolateLinear(a, b, t);
        return truncateDegrees(lerp);
    }

    // wraps angles to 360 degrees.
    private static float truncateDegrees(float a) {

        if (a > 360.0) {
            while (a > 360.0f) {
                a -= 360.0f;
            }
        }
        else if (a < 0) {
            while (a < 0) {
                a += 360 - a;
            }
        }

        return a;
    }

}
