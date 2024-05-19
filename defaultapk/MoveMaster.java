package jp.jaxa.iss.kibo.rpc.defaultapk;

import gov.nasa.arc.astrobee.types.Quaternion;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.Kinematics;

import java.util.*;

public class MoveMaster {
    public static Kinematics myKinematics = YourService.myApi.getRobotKinematics();

    public class moveData {
        public moveData prev;
        public int[] indices;
        public float[] point;
        public float airDistanceSquared;
        public float queueValue;

        public moveData(){};

        //Special constructor for the end point
        public moveData(moveData prev, float[] point) {
            this.prev = prev;
            this.indices = new int[]{-1, -1, -1};
            this.point = point;
            this.airDistanceSquared = 0f;
            this.queueValue = airDistanceSquared + this.getAirDistanceSquared() + (prev.queueValue - airDistanceSquared);
        }

        public moveData(moveData prev, int[] indices, float[] point, float airDistanceSquared) {
            this.prev = prev;
            this.indices = indices;
            this.point = point;
            this.airDistanceSquared = airDistanceSquared;
            this.queueValue = airDistanceSquared + this.getAirDistanceSquared() + (prev.queueValue - airDistanceSquared);
        }

        public float getAirDistanceSquared() {
            if(isIllegalMove()) {
                return Constants.areaDiagonalSquared;
            } else {
                return (prev.point[0] - point[0]) * (prev.point[0] - point[0]) + (prev.point[1] - point[1]) * (prev.point[1] - point[1]) + (prev.point[2] - point[2]) * (prev.point[2] - point[2]);
            }
        }

        public boolean isIllegalMove() {
            if(point[0] - Constants.avoidance < Constants.minX || point[0] + Constants.avoidance >= Constants.maxX
                || point[1] - Constants.avoidance < Constants.minY || point[1] + Constants.avoidance >= Constants.maxY
                || point[2] - Constants.avoidance < Constants.minZ || point[2] + Constants.avoidance >= Constants.maxZ) {
                return true;
            } else {
                for(int i = 0; i < Contants.minKOZ.length; i++) {
                    if((Contants.minKOZ[i][0] >= point[0] && Contants.maxKOZ[0] <= prev.point[0]) || (Contants.minKOZ[i][0] >= prev.point[0] && Contants.maxKOZ[0] <= point[0])
                            || (Contants.minKOZ[i][1] >= point[1] && Contants.maxKOZ[1] <= prev.point[1]) || (Contants.minKOZ[i][1] >= prev.point[1] && Contants.maxKOZ[1] <= point[1]
                            || (Contants.minKOZ[i][2] >= point[2] && Contants.maxKOZ[2] <= prev.point[2]) || (Contants.minKOZ[i][2] >= prev.point[2] && Contants.maxKOZ[2] <= point[2])) {
                        float deltaX = point[0] - prev.point[0];
                        float deltaY = point[1] - prev.point[1];
                        float deltaZ = point[2] - prev.point[2];
                        float minX;
                        float maxX;
                        float minY;
                        float maxY;
                        float minZ;
                        float maxZ;
                        float minT;
                        float maxT;

                        //TODO: find a non-abritrary threshold value
                        if(Math.abs(deltaX) > 0.01) {
                            minX = Constants.minKOZ[i][0];
                            maxX = Constants.maxKOZ[i][0];
                            minT = (minX - prev.point[0]) / deltaX;
                            maxT = (maxX - prev.point[0]) / deltaX;
                            minY = prev.point[1] + minT * deltaY;
                            maxY = prev.point[1] + maxT * deltaY;
                            minZ = prev.point[2] + minT * deltaZ;
                            maxZ = prev.point[2] + maxT * deltaZ;

                            if(((minY + Constants.avoidance >= Constants.minKOZ[i][1] && minY - Constants.avoidance <= Constants.minKOZ[i][1])
                                && (maxY + Constants.avoidance >= Constants.minKOZ[i][1] && maxY - Constants.avoidance <= Constants.minKOZ[i][1]))
                                || ((minZ + Constants.avoidance >= Constants.minKOZ[i][2] && minZ - Constants.avoidance <= Constants.minKOZ[i][2])
                                && (maxZ + Constants.avoidance >= Constants.minKOZ[i][2] && maxZ - Constants.avoidance <= Constants.minKOZ[i][2]))) {
                                return true;
                            }
                        }
                        if(Math.abs(deltaY) > 0.01) {
                            minY = Constants.minKOZ[i][1];
                            maxY = Constants.maxKOZ[i][1];
                            minT = (minY - prev.point[1]) / deltaY;
                            maxT = (maxY - prev.point[1]) / deltaY;
                            minX = prev.point[0] + minT * deltaX;
                            maxX = prev.point[0] + maxT * deltaX;
                            minZ = prev.point[2] + minT * deltaZ;
                            maxZ = prev.point[2] + maxT * deltaZ;

                            if(((minX + Constants.avoidance >= Constants.minKOZ[i][0] && minX - Constants.avoidance <= Constants.minKOZ[i][0])
                                && (maxX + Constants.avoidance >= Constants.minKOZ[i][0] && maxX - Constants.avoidance <= Constants.minKOZ[i][0]))
                                || ((minZ + Constants.avoidance >= Constants.minKOZ[i][2] && minZ - Constants.avoidance <= Constants.minKOZ[i][2])
                                && (maxZ + Constants.avoidance >= Constants.minKOZ[i][2] && maxZ - Constants.avoidance <= Constants.minKOZ[i][2]))) {
                                return true;
                            }
                        }
                        if(Math.abs(deltaZ) > 0.01) {
                            minZ = Constants.minKOZ[i][2];
                            maxZ = Constants.maxKOZ[i][2];
                            minT = (minZ - prev.point[2]) / deltaZ;
                            maxT = (maxZ - prev.point[2]) / deltaZ;
                            minX = prev.point[0] + minT * deltaX;
                            maxX = prev.point[0] + maxT * deltaX;
                            minY = prev.point[1] + minT * deltaY;
                            maxY = prev.point[1] + maxT * deltaY;

                            if(((minX + Constants.avoidance >= Constants.minKOZ[i][0] && minX - Constants.avoidance <= Constants.minKOZ[i][0])
                                && (maxX + Constants.avoidance >= Constants.minKOZ[i][0] && maxX - Constants.avoidance <= Constants.minKOZ[i][0]))
                                || ((minY + Constants.avoidance >= Constants.minKOZ[i][1] && minY - Constants.avoidance <= Constants.minKOZ[i][1])
                                && (maxY + Constants.avoidance >= Constants.minKOZ[i][1] && maxY - Constants.avoidance <= Constants.minKOZ[i][1]))) {
                                return true;
                            }
                        }
                    }
                }

                return false;
            }
        }

        public int compareTo(moveData other) {
            return this.queueValue - other.queueValue;
        }

        public boolean equals(moveData other) {
            return this.indices[0] == other.indices[0] && this.indices[1] == other.indices[1] && this.indices[2] == other.indices[2] && this.queueValue <= other.queueValue;
        }

        public Point getPoint() {
            return new Point(point[0], point[1], point[2]);
        }
    }

    //Returns a list of Points to move to
    //Parameter is array and not a point since Points contain double, so to (hopefully) make things faster floats are used when possible
    //Order of parameter and output is {X, Y, Z}
    public static ArrayList<Point> moveTo(float[] endpoint) {
        moveTo(endpoint);
    }

    public static ArrayList<Point> moveTo(float[] endpoint) {
        ArrayList<Point> output = new ArrayList<Point>();
        PriorityBlockingQueue<moveData> queue = new PriorityBlockingQueue<moveData>();
        HashSet<int[]> checked = new HashSet<int[]>();

        moveData beginData = new moveData();
        beginData.point = new float[]{myKinematics.getPosition().getX(), myKinematics.getPosition().getY(), myKinematics.getPosition().getZ()};
        beginData.airDistanceSquared = getAirDistanceSquared(beginData.point, endpoint);
        beginData.queueValue = beginData.airDistanceSquared;
        beginData.prev = null;

        int masterPointTotalLength = Constants.masterPoints.length * Constants.masterPoints[0].length * Constants.masterPoints[0][0].length;

        int availableProcessors = Runtime.getRuntime().getAvailableProcessors();
        Thread threadArr = new Thread[availableProcessors];
        int threadIndexLength = masterPointTotalLength / availableProcessors;

        for(int i = 0; i < availableProcessors; i++) {
            threadArr[i] = new Thread(() -> {
                int currIndex = i * threadIndexLength;
                int endIndex = (i + 1) * threadIndexLength;
                int xIndex = currIndex / Constants.masterPoints.length;
                int yIndex = (currIndex % Constants.masterPoints.length) / Constants.masterPoints[0].length;
                int zIndex = currIndex % Constants.masterPoints[0].length;

                while(currIndex < endIndex) {
                    queue.add(new moveData(beginData,
                            new int[]{xIndex, yIndex, zIndex},
                            Constants.masterPoints[xIndex][yIndex][zIndex],
                            getAirDistanceSquared(endpoint, Constants.masterPoints[xIndex][yIndex][zIndex])));

                    currIndex++;
                    zIndex++;

                    if(zIndex == Constants.masterPoints[0][0].length) {
                        zIndex = 0;
                        yIndex++;

                        if(yIndex == Constants.masterPoints[0].length) {
                            yIndex = 0;
                            xIndex++;
                        }
                    }
                }
            });
        }

        for(Thread thread: threadArr) {
            thread.join();
        }

        queue.add(new moveData(beginData, endpoint));

        while(queue.element().point != endpoint) {
            moveData tempData = queue.remove();
            checked.add(tempData.indices);

            int availableProcessors = Runtime.getRuntime().getAvailableProcessors();
            Thread threadArr = new Thread[availableProcessors];
            int threadIndexLength = masterPointTotalLength / availableProcessors;

            for(int i = 0; i < availableProcessors; i++) {
                threadArr[i] = new Thread(() -> {
                    int currIndex = i * threadIndexLength;
                    int endIndex = (i + 1) * threadIndexLength;
                    int xIndex = currIndex / Constants.masterPoints.length;
                    int yIndex = (currIndex % Constants.masterPoints.length) / Constants.masterPoints[0].length;
                    int zIndex = currIndex % Constants.masterPoints[0].length;
                    int[] indices = new int[]{xIndex, yIndex, zIndex};

                    while(currIndex < endIndex) {
                        if(!checked.contains(indices)) {
                            moveData newData = new moveData(beginData,
                                    indices,
                                    Constants.masterPoints[xIndex][yIndex][zIndex],
                                    getAirDistanceSquared(endpoint, Constants.masterPoints[xIndex][yIndex][zIndex]));

                            if(queue.remove(newData)) {
                                queue.add(newData);
                            }
                        }

                        currIndex++;
                        indices[2]++;

                        if(indices[2] == Constants.masterPoints[0][0].length) {
                            indices[2] = 0;
                            indices[1]++;

                            if(indices[1] == Constants.masterPoints[0].length) {
                                indices[1] = 0;
                                indices[0]++;
                            }
                        }
                    }
                });
            }

            for(Thread thread: threadArr) {
                thread.join();
            }

            queue.add(new moveData(tempData, endpoint));
        }

        moveData wow = queue.remove();
        output.add(wow.getPoint());
        wow = output.prev;

        while(wow != null) {
            output.add(0, wow.getPoint());
            wow = output.prev;
        }

        return output;
    }

    public static float getAirDistanceSquared(float[] endpoint, float[] point) {
        return (point[0] - endpoint[0]) * (point[0] - endpoint[0]) + (point[1] - endpoint[1]) * (point[1] - endpoint[1]) + (point[2] - endpoint[2]) * (point[2] - endpoint[2]);
    }
}