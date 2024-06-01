package jp.jaxa.iss.kibo.rpc.sampleapk;

import gov.nasa.arc.astrobee.types.Point;

public final class Constants {
    public static class Zone {
        //{X, Y, Z}
        //The vertices of the rectangular KOZ
        Point[] vertices;
        //Essentially, if we were to convert all points to spherical coordinates
        //Of the form {magnitude, rotation around the z-axis, angle above the xy-plane}
        //The following are the absolute minimum/maximum angles and magnitudes of the zone
        //(Used in the path planning)

        //Rotation around the x-axis
        double minAngleZ;
        double maxAngleZ;
        //Angle above the XY plane
        double minAngleXY;
        double maxAngleXY;
        //All magnitudes are squared since every damn thing is squared
        double minMagnitude;
        double maxMagnitude;
        //So it's easier to identify during logs
        String name;

        Zone(double[] min, double[] max, String name) {
            //TODO: Add/subtract avoidance (first see if avoidance is even needed)
            vertices = new Point[]{
                    new Point(min[0], min[1], min[2]),
                    new Point(min[0], min[1], max[2]),
                    new Point(min[0], max[1], min[2]),
                    new Point(min[0], max[1], max[2]),
                    new Point(max[0], min[1], min[2]),
                    new Point(max[0], min[1], max[2]),
                    new Point(max[0], max[1], min[2]),
                    new Point(max[0], max[1], max[2])};

            for(Point point: vertices) {
                double magnitude = (point.getX()) * (point.getX())
                                    + (point.getY()) * (point.getY())
                                    + (point.getZ()) * (point.getZ());
                double angleZ = Math.atan2(point.getY(), point.getX());
                double angleXY = Math.atan2(point.getZ(), Math.sqrt(point.getX() * point.getX() + point.getY() * point.getY()));

                if(magnitude < minMagnitude) {
                    minMagnitude = magnitude;
                } else if(magnitude > maxMagnitude) {
                    maxMagnitude = magnitude;
                }

                if(angleZ < minAngleZ) {
                    minAngleZ = angleZ;
                } else if(angleZ > maxAngleZ) {
                    maxAngleZ = angleZ;
                }

                if(angleXY < minAngleXY) {
                    minAngleXY = angleXY;
                } else if(angleXY > maxAngleXY) {
                    maxAngleXY = angleXY;
                }
            }

            this.name = name;
        }

        public void log() {
            YourService.log(name + " zone. Min: " + vertices[0].getX() + ", " + vertices[0].getY() + ", " + vertices[0].getZ());
            YourService.log(name + " zone. Max: " + vertices[7].getX() + ", " + vertices[7].getY() + ", " + vertices[7].getZ());
        }

        public boolean couldCross(double minMagnitude, double maxMagnitude, double minAngleZ, double maxAngleZ, double minAngleXY, double maxAngleXY) {
            return !(this.maxMagnitude < minMagnitude || this.minMagnitude > maxMagnitude)
                    && !(this.maxAngleZ < minAngleZ || this.minAngleZ > maxAngleZ)
                    && !(this.maxAngleXY < minAngleXY || this.minAngleXY > maxAngleXY);
        }

        public boolean crosses(Point start, Point end) {
            double deltaX = end.getX() - start.getX();
            double deltaY = end.getY() - start.getY();
            double deltaZ = end.getZ() - start.getZ();

            double minX, maxX, minY, maxY, minZ, maxZ, minT, maxT;

            //TODO: find a non-arbitrary threshold value
            if(Math.abs(deltaX) > 0.01d) {
                minX = vertices[0].getX();
                maxX = vertices[7].getX();
                minT = (minX - start.getX()) / deltaX;
                maxT = (maxX - start.getX()) / deltaX;
                minY = start.getY() + minT * deltaY;
                maxY = start.getY() + maxT * deltaY;
                minZ = start.getZ() + minT * deltaZ;
                maxZ = start.getZ() + maxT * deltaZ;

                if((minY >= vertices[0].getY() && minY <= vertices[7].getY()
                    && minZ >= vertices[0].getZ() && minZ <= vertices[7].getZ())
                    || (maxY >= vertices[0].getY() && maxY <= vertices[7].getY()
                    && maxZ >= vertices[0].getZ() && maxZ <= vertices[7].getZ())) {
                    return true;
                }
            }

            if(Math.abs(deltaY) > 0.01d) {
                minY = vertices[0].getY();
                maxY = vertices[7].getY();
                minT = (minY - start.getY()) / deltaY;
                maxT = (maxY - start.getY()) / deltaY;
                minX = start.getX() + minT * deltaX;
                maxX = start.getX() + maxT * deltaX;
                minZ = start.getZ() + minT * deltaZ;
                maxZ = start.getZ() + maxT * deltaZ;

                if((minX >= vertices[0].getX() && minX <= vertices[7].getX()
                    && minZ >= vertices[0].getZ() && minZ <= vertices[7].getZ())
                    || (maxX >= vertices[0].getX() && maxX <= vertices[7].getX()
                    && maxZ >= vertices[0].getZ() && maxZ <= vertices[7].getZ())) {
                    return true;
                }
            }

            if(Math.abs(deltaZ) > 0.01d) {
                minZ = vertices[0].getZ();
                maxZ = vertices[7].getZ();
                minT = (minZ - start.getZ()) / deltaZ;
                maxT = (maxZ - start.getZ()) / deltaZ;
                minX = start.getX() + minT * deltaX;
                maxX = start.getX() + maxT * deltaX;
                minY = start.getY() + minT * deltaY;
                maxY = start.getY() + maxT * deltaY;

                if((minX >= vertices[0].getX() && minX <= vertices[7].getX()
                        && minY >= vertices[0].getY() && minY <= vertices[7].getY())
                        || (maxX >= vertices[0].getX() && maxX <= vertices[7].getX()
                        && maxY >= vertices[0].getY() && maxY <= vertices[7].getY())) {
                    return true;
                }
            }

            return false;
        }
    }

    public static final class GameData {
        //The two Keep-In-Zones
        public static final Zone[] KIZ = new Zone[]{
                new Zone(new double[]{10.3d, -10.2d, 4.32d}, new double[]{11.55d, -6.0d, 5.577d}, "KIZ 1"), //Big KIZ
                new Zone(new double[]{9.5d, -10.5d, 4.02d}, new double[]{10.5d, -9.6d, 4.8d}, "KIZ 2")}; //Little KIZ where Bee initially undocks
        //All size KOZ zones, organised by their minimum magnitude
        public static final Zone[] KOZ = new Zone[]{
                new Zone(new double[]{10.25d, -7.4d, 4.97d}, new double[]{10.87d, -7.35d, 5.62d}, "KOZ 3-2"), //KOZ 3 position 2, Magnitude: 183.7859
                new Zone(new double[]{10.87d, -7.4d, 4.27d}, new double[]{11.6d, -7.35d, 4.97d}, "KOZ 3-1"), //KOZ 3 position 1, Magnitude: 190.4123
                new Zone(new double[]{10.25d, -8.5d, 4.27d}, new double[]{10.7d, -8.45d, 4.97d}, "KOZ 2-2"), //KOZ 2 position 2, Magnitude: 194.6979
                new Zone(new double[]{10.87d, -8.5d, 4.97d}, new double[]{11.6d, -8.45d, 5.62d}, "KOZ 2-1"), //KOZ 2 position 1, Magnitude: 214.2603
                new Zone(new double[]{10.25d, -9.5d, 4.97d}, new double[]{10.87d, -9.45d, 5.62d}, "KOZ 1-2"), //KOZ 1 position 2, Magnitude: 219.0659
                new Zone(new double[]{10.87d, -9.5d, 4.27d}, new double[]{11.6d, -9.45d, 4.97d}, "KOZ 1-1")}; //KOZ 1 position 1, Magnitude: 225.6923

        //The areas where the objects could be. Planes, technically, but it's easier to store as three dimensional for now
        public static final Zone[] Areas = new Zone[]{
                new Zone(new double[]{10.42d, -10.58d, 4.82d}, new double[]{11.48d, -10.58d, 5.57d}, "Area 1"),
                new Zone(new double[]{10.3d, -9.25d, 3.76203d}, new double[]{11.55f, -8.5d, 3.76203d}, "Area 2"),
                new Zone(new double[]{10.3d, -8.4d, 3.76093d}, new double[]{11.55d, -7.45d, 3.76093d}, "Area 3"),
                new Zone(new double[]{9.866984d, -7.34d, 4.32d}, new double[]{9.866984d, -6.365d, 5.57d}, "Area 4")};
        //Start position and orientation of Bee
        public static final double[] startPosition = new double[]{9.815d, -9.806d, 4.293d};
        public static final double[] startOrientation = new double[]{1d, 0d, 0d, 0d};
        //Position and orientation of the astronaut
        public static final double[] astronautPosition = new double[]{11.143d, -6.7607d, 4.9654d};
        public static final double[] astronautOrientation = new double[]{0d, 0d, 0.707d, 0.707d};
    }

    //Stores the offsets of various components of Astrobee from the centre of Astrobee
    //All are stored in the order {X, Y, Z} and measured in metres
    public static final class MechanicalOffsets {
        //All the listed camera are monochrome
        //Camera used for image processing and taking a photo after sending finish command
        public static final double[] navCam = new double[]{0.1177d, -0.0422d, -0.0826d};
        //Camera used for detecting obstacles within 30cm
        public static final double[] hazCam = new double[]{0.1328d, 0.0362d, -0.0826d};
        //Camera used for docking
        public static final double[] dockCam = new double[]{-0.1061d, -0.054d, -0.0064d};
        //Camera used for grabbing a handrail
        public static final double[] perchCam = new double[]{-0.1331d, 0.0509d, -0.0166d};
    }

    //The physical specifications of Astrobee
    public static final class Specifications {
        //The mass of Astrobee. Measured in kilograms
        public static final double mass = 10d;
        //The max speed of Astrobee. Measured in metres/second
        public static final double maxSpeed = 0.5d;
        //The maximum thrust of Astrobee along it's X axis. Measured in newtons
        public static final double maxThrustX = 0.6d;
        //The maximum thrust of Astrobee along it's Y and Z axis. Measured in newtons
        public static final double maxThrustY = 0.3d;
        //The minimum moving distance of Astrobee. Measured in metres
        public static final double minMove = 0.05d;
        //The minimum rotating angle of Astrobee. Measured in degrees
        public static final double minRotDeg = 7.5d;
        //The above except measured in radians
        public static final double minRotRad = 0.1309d;
        //The length of one side of Astrobee. Astrobee is a cube. Measured in metres
        public static final double sideLength = 0.32d;
    }

    public static final class Calculations {
        //The metres a given side of Bee should try to keep from the KOZ and KIZ bounds
        static final double clearance = 0.00d; //TODO: Figure out if this is needed, then if there is a non-arbitrary value for it
        //The avoidance value to be used in calculations
        public static final double avoidance = /*(((float) Math.sqrt(3)) * Specifications.sideLength / 2.0f) + */clearance;
        //The precision in metres of creating the masterPoints list. 0.05 since that's the minimum distance needed for Bee to move
        public static final double masterPointsPrecision = 0.05d;

        static boolean customPointEquals(Point one, Point two) {
            //TODO: Non-arbitrary epsilon?
            return Math.abs(one.getX() - two.getX()) < 0.001d && Math.abs(one.getY() - two.getY()) < 0.001d && Math.abs(one.getZ() - two.getZ()) < 0.001d;
        }

        //TODO: Improve check to take in a start point and end point and ensure the line between them doesn't go out of bounds
        static boolean isOutOfBounds(Point point) {
            for(Constants.Zone zone: Constants.GameData.KIZ) {
                if(point.getX() <= zone.vertices[0].getX() || point.getX() >= zone.vertices[0].getX()
                        || point.getY() <= zone.vertices[0].getY() || point.getY() >= zone.vertices[0].getY()
                        || point.getZ() <= zone.vertices[0].getZ() || point.getZ() >= zone.vertices[0].getZ()) {
                    return true;
                }
            }

            return false;
        }

    }
}