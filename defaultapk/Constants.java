package jp.jaxa.iss.kibo.rpc.defaultapk;

import java.util.*;

public static final class Constants {
    //The minimum X value of the overall area
    public static final float minX = 10.3f;
    //The maximum X value of the overall area
    public static final float maxX = 11.55f;
    //The minimum Y value of the overall area
    public static final float minY = -10.2f;
    //The maximum Y value of the overall area
    public static final float maxY = -6.0f;
    //The minimum Z value of the overall area
    public static final float minZ = 4.32f;
    //The maximum Z value of the overall area
    public static final float maxZ = 5.57f;
    //The diagonal of the overall area squared
    public static final float areaVolumeSquared = (maxX - minX) * (maxX - minX) * (maxY - minY) * (maxY - minY) * (maxZ - minZ) * (maxZ - minZ);
    //KOZ stands for keep out zones
    //Array of each zone, for each zone the minimum {X, Y, Z} values of that zone. Measured in metres
    public static final float[][] minKOZ = new float[][]{{10.87, -9.5, 4.27},
                                                            {10.25, -9.5, 4.97},
                                                            {10.87, -8.5, 4.97},
                                                            {10.25, -8.5, 4.27},
                                                            {10.87, -7.4, 4.27},
                                                            {10.25, -7.4, 4.97}};
    //Same as above, but for the maximum {X, Y, Z} values of each zone. Measured in metres
    public static final float[][] maxKOZ = new float[][]{{11.6, -9.45, 4.97},
                                                            {10.87, -9.45, 5.62},
                                                            {11.6, -8.45, 5.62},
                                                            {10.7, -8.45, 4.97},
                                                            {11.6, -7.35, 4.97},
                                                            {10.87, -7.35, 5.62}};
    //The true distance in metres a given side of Astrobee should keep from the KOZ and area bounds.
    //See below Specifications to see the avoidance to be used in calculations
    public static final float trueAvoidance = 0.05f; //TODO: find a non-arbitrary value
    //A list of points which Astrobee should try and move to
    //Array of X indices, for each Y indices, for each Z indices, for each actual {X, Y, Z} values
    public static final float[][][][] masterPoints = initMasterPoints();
    //How precise the list should be. Difference between two X/Y/Z values should equal difference. Measured in metres
    public static final float masterPointsPrecision = 0.05f;

    //Stores the offsets of various components of Astrobee from the centre of Astrobee
    //All are stored in the order {X, Y, Z} and measured in metres
    public static final class Offsets {
        //All the listed camera are monochrome
        //Camera used for image processing and taking a photo after sending finish command
        public static final float[] navCam = new float[]{0.1177f, -0.0422f, -0.0826f};
        //Camera used for detecting obstacles within 30cm
        public static final float[] hazCam = new float[]{0.1328f, 0.0362f, -0.0826f};
        //Camera used for docking
        public static final float[] dockCam = new float[]{-0.1061f, -0.054f, -0.0064f};
        //Camera used for grabbing a handrail
        public static final float[] perchCam = new float[]{-0.1331f, 0.0509f, -0.0166f};
    }

    //The physical specifications of Astrobee
    public static final class Specifications {
        //The mass of Astrobee. Measured in kilograms
        public static final float mass = 10f;
        //The max speed of Astrobee. Measured in metres/second
        public static final float maxSpeed = 0.5f;
        //The maximum thrust of Astrobee along it's X axis. Measured in newtons
        public static final float maxThrustX = 0.6f;
        //The maximum thrust of Astrobee along it's Y and Z axis. Measured in newtons
        public static final float maxThrustY = 0.3f;
        //The minimum moving distance of Astrobee. Measured in metres
        public static final float minMove = 0.05f;
        //The minimum rotating angle of Astrobee. Measured in degrees
        public static final float minRotDeg = 7.5f;
        //The above except measured in radians
        public static final float minRotRad = 0.1309f;
        //The length of one side of Astrobee. Astrobee is a cube. Measured in metres
        public static final float sideLength = 0.32f;
    }

    //The avoidance value to be used in calculations. Takes into account the diagonal length, since move functions use the centre. Measured in metres
    public static final float avoidance = ((float) Math.sqrt(3)) * Specifications.sideLength + trueAvoidance;

    //Helper method to create the masterPoints array
    public static final float[][][][] initMasterPoints() {
        float[][][] output = new float[(int) Math.ceil((maxX - minX - 2 * Specifications.sideLength) / precision)][(int) Math.ceil((maxY - minY - 2 * Specifications.sideLength) / precision)][(int) Math.ceil((maxZ - minZ - 2 * Specifications.sideLength) / precision)];
        float tempX = minX + Specifications.sideLength;
        float tempY = minY + Specifications.sideLength;
        float tempZ = minZ + Specifications.sideLength;
        int xIndex = 0;
        int yIndex = 0;
        int zIndex = 0;

        while(tempX <= maxX - Specifications.sideLength && xIndex < output.length) {
            while(tempY <= maxY - Specifications.sideLength && yIndex < output[0].length) {
                while(tempZ <= maxZ - Specifications.sideLength && zIndex < output[0][0].length) {
                    output[xIndex][yIndex][zIndex] = new float[]{tempX, tempY, tempZ};

                    tempZ += masterPointsPrecision;
                    zIndex++;
                }

                tempZ = minZ + Specifications.sideLength;
                tempY += masterPointsPrecision;
                yIndex++;
            }

            tempY = minY + Specifications.sideLength;
            tempX += masterPointsPrecision;
            xIndex++;
        }

        return output;
    }
}