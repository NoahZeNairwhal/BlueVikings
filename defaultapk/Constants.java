package jp.jaxa.iss.kibo.rpc.sampleapk;

public final class Constants {
    public static class Zone {
        //{X, Y, Z}
        public float[] min;
        public float[] max;
        //So it's easier to identify during logs
        public String name;

        public Zone() {
            min = new float[3];
            max = new float[3];
            name = "";
        }

        public Zone(float[] min, float[] max, String name) {
            this.min = min;
            this.max = max;
            this.name = name;
        }

        public void log() {
            YourService.log(name + " zone. Min: " + min[0] + ", " + min[1] + ", " + min[2]);
            YourService.log(name + " zone. Max: " + max[0] + ", " + max[1] + ", " + max[2]);
        }
    }

    public static final class GameData {
        //The two Keep-In-Zones
        public static final Zone[] KIZ = new Zone[]{
                new Zone(new float[]{10.3f, -10.2f, 4.32f}, new float[]{11.55f, -6.0f, 5.577f}, "KIZ 1"), //Big KIZ
                new Zone(new float[]{9.5f, -10.5f, 4.02f}, new float[]{10.5f, -9.6f, 4.8f}, "KIZ 2")}; //Little KIZ where Bee initially undocks
        //All size KOZ zones, organised by the number followed by the position
        public static final Zone[] KOZ = new Zone[]{
                new Zone(new float[]{10.87f, -9.5f, 4.27f}, new float[]{11.6f, -9.45f, 4.97f}, "KOZ 1-1"), //KOZ 1 position 1
                new Zone(new float[]{10.25f, -9.5f, 4.97f}, new float[]{10.87f, -9.45f, 5.62f}, "KOZ 1-2"), //KOZ 1 position 2
                new Zone(new float[]{10.87f, -8.5f, 4.97f}, new float[]{11.6f, -8.45f, 5.62f}, "KOZ 2-1"), //KOZ 2 position 1
                new Zone(new float[]{10.25f, -8.5f, 4.27f}, new float[]{10.7f, -8.45f, 4.97f}, "KOZ 2-2"), //KOZ 2 position 2
                new Zone(new float[]{10.87f, -7.4f, 4.27f}, new float[]{11.6f, -7.35f, 4.97f}, "KOZ 3-1"), //KOZ 3 position 1
                new Zone(new float[]{10.25f, -7.4f, 4.97f}, new float[]{10.87f, -7.35f, 5.62f}, "KOZ 3-2")}; //KOZ 3 position 2
        //The areas where the objects could be. Planes, technically, but it's easier to store as three dimensional for now
        public static final Zone[] Areas = new Zone[]{
                new Zone(new float[]{10.42f, -10.58f, 4.82f}, new float[]{11.48f, -10.58f, 5.57f}, "Area 1"),
                new Zone(new float[]{10.3f, -9.25f, 3.76203f}, new float[]{11.55f, -8.5f, 3.76203f}, "Area 2"),
                new Zone(new float[]{10.3f, -8.4f, 3.76093f}, new float[]{11.55f, -7.45f, 3.76093f}, "Area 3"),
                new Zone(new float[]{9.866984f, -7.34f, 4.32f}, new float[]{9.866984f, -6.365f, 5.57f}, "Area 4")};
        //Start position and orientation of Bee
        public static final float[] startPosition = new float[]{9.815f, -9.806f, 4.293f};
        public static final float[] startOrientation = new float[]{1f, 0f, 0f, 0f};
        //Position and orientation of the astronaut
        public static final float[] astronautPosition = new float[]{11.143f, -6.7607f, 4.9654f};
        public static final float[] astronautOrientation = new float[]{0f, 0f, 0.707f, 0.707f};
    }

    //Stores the offsets of various components of Astrobee from the centre of Astrobee
    //All are stored in the order {X, Y, Z} and measured in metres
    public static final class MechanicalOffsets {
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

    public static final class Calculations {
        //Volume of the KIZ 1 squared
        public static final float KIZ1VolumeSquared =
                (GameData.KIZ[0].max[0] - GameData.KIZ[0].min[0]) * (GameData.KIZ[0].max[0] - GameData.KIZ[0].min[0])
                        + (GameData.KIZ[0].max[1] - GameData.KIZ[0].min[1]) * (GameData.KIZ[0].max[1] - GameData.KIZ[0].min[1])
                        + (GameData.KIZ[0].max[2] - GameData.KIZ[0].min[2]) * (GameData.KIZ[0].max[2] - GameData.KIZ[0].min[2]);
        //Volume of the KIZ 2 squared
        public static final float KIZ2VolumeSquared =
                (GameData.KIZ[1].max[0] - GameData.KIZ[1].min[0]) * (GameData.KIZ[1].max[0] - GameData.KIZ[1].min[0])
                        + (GameData.KIZ[1].max[1] - GameData.KIZ[1].min[1]) * (GameData.KIZ[1].max[1] - GameData.KIZ[1].min[1])
                        + (GameData.KIZ[1].max[2] - GameData.KIZ[1].min[2]) * (GameData.KIZ[1].max[2] - GameData.KIZ[1].min[2]);
        //The metres a given side of Bee should try to keep from the KOZ and KIZ bounds
        public static final float clearance = 0.05f; //TODO: find a non-arbitrary value
        //The avoidance value to be used in calculations
        public static final float avoidance = /*(((float) Math.sqrt(3)) * Specifications.sideLength / 2.0f) + */clearance;
        //The precision in metres of creating the masterPoints list. 0.05 since that's the minimum distance needed for Bee to move
        public static final float masterPointsPrecision = 0.05f;
        //A list of points for KIZ 1 used for path planning
        public static float[][][][] KIZ1masterPoints = initMasterPoints(0);
        //A list of points for KIZ 2 used for path planning
        public static float[][][][] KIZ2masterPoints = initMasterPoints(1);
    }

    //Helper method to create the masterPoints array
    public static float[][][][] initMasterPoints(int kizIndex) {
        float[][][][] output = new float
                [(int) Math.ceil((GameData.KIZ[kizIndex].max[0] - GameData.KIZ[kizIndex].min[0] - 2 * Calculations.avoidance) / Calculations.masterPointsPrecision)]
                [(int) Math.ceil((GameData.KIZ[kizIndex].max[1] - GameData.KIZ[kizIndex].min[1] - 2 * Calculations.avoidance) / Calculations.masterPointsPrecision)]
                [(int) Math.ceil((GameData.KIZ[kizIndex].max[2] - GameData.KIZ[kizIndex].min[2] - 2 * Calculations.avoidance) / Calculations.masterPointsPrecision)]
                [3];
        float tempX = GameData.KIZ[kizIndex].min[0] + Calculations.avoidance;
        float tempY = GameData.KIZ[kizIndex].min[1] + Calculations.avoidance;
        float tempZ = GameData.KIZ[kizIndex].min[2] + Calculations.avoidance;
        int xIndex = 0;
        int yIndex = 0;
        int zIndex = 0;

        while(tempX <= GameData.KIZ[kizIndex].max[0] - Calculations.avoidance && xIndex < output.length) {
            while(tempY <= GameData.KIZ[kizIndex].max[1] - Calculations.avoidance && yIndex < output[0].length) {
                while(tempZ <= GameData.KIZ[kizIndex].max[2] - Calculations.avoidance && zIndex < output[0][0].length) {
                    output[xIndex][yIndex][zIndex] = new float[]{tempX, tempY, tempZ};

                    tempZ += Calculations.masterPointsPrecision;
                    zIndex++;
                }

                tempZ = GameData.KIZ[kizIndex].min[2] + Calculations.avoidance;
                zIndex = 0;
                tempY += Calculations.masterPointsPrecision;
                yIndex++;
            }

            tempY = GameData.KIZ[kizIndex].min[1] + Calculations.avoidance;
            yIndex = 0;
            tempX += Calculations.masterPointsPrecision;
            xIndex++;
        }

        return output;
    }
}