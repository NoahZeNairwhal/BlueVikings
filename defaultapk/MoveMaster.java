package jp.jaxa.iss.kibo.rpc.sampleapk;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.Kinematics;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.lang.Thread;
import java.util.logging.Level;

//This class is used to generate the points for Kibo to move to to avoid KOZ
//It does not generate any quaternion info
public class MoveMaster {
    //Used to get the current position of Kibo
    public static Kinematics myKinematics;

    //Helper class that's linked such that it sees its parents but not its children
    public static class moveData implements Comparable<moveData> {
        //Parent moveData
        public moveData prev;
        //The corresponding indices {X, Y, Z} of the point in the masterPoints list (see Constants.java)
        //Not used for the beginning or end point
        public int[] indices;
        //The actual point data {X, Y, Z}
        public float[] point;
        //The square of the distance between this point and the endpoint.
        //(Not taking the square root means the distance isn't true, but it saves on calculations made)
        public float airDistanceSquared;
        //The overall value of this moveData. Used by the queue
        //Equal to the distance (airDistanceSquared) from this to the endpoint, plus the distance squared
        //of the path traveled between all parents to get to this point
        //These heuristics should always result in the true shortest path being found
        public float queueValue;

        //Special constructor for the beginning point, where we fill in the members manually
        public moveData(){};

        //Special constructor for the end point
        //Has no indices. If there is no KOZ between the endpoint and the parent however, we are guaranteed this will go to the top of the queue
        //Looking back I could've just filled in the parameters manually for the endpoint, but I guess I'm lazy
        public moveData(moveData prev, float[] point) {
            this(prev, new int[]{-1, -1, -1}, point, 0f);
        }

        //Used to generate all other points
        public moveData(moveData prev, int[] indices, float[] point, float airDistanceSquared) {
            this.indices = indices;
            this.point = point;
            this.airDistanceSquared = airDistanceSquared;

            //Tries to cut out the parent if it's possible to go straight to this moveData from the parent's parent
            //Checks if the parent is null (then we're root) or the parent of the parent is null (in which case we can't cut the parent)
            if(prev != null && prev.prev != null) {
                this.prev = prev.prev;

                //Checks if a KOZ would be touched going from the parent's parent to this, and if it is illegal, then we keep the parent
                if(isIllegalMove()) {
                    this.prev = prev;
                }
            } else {
                //We are root or need to keep the parent
                this.prev = prev;
            }
            //Subtracting the previous airDistanceSquared is necessary since we have a new distance between this and the endpoint
            //I apologise for the poorly named variables. this.getAirDistanceSquared() builds the "path" between all parents to this point, and thus does not get subtracted ever
            this.queueValue = airDistanceSquared + this.getAirDistanceSquared() + (prev.queueValue - prev.airDistanceSquared);
        }

        public float getAirDistanceSquared() {
            //If it's illegal, add the volume since in the absolute worse case scenario we have to travel through every point in masterPoints
            //i.e. .length * [].length * [][].length, which is equivalent to length * height * width and thus the volume. Squared for aforementioned computation saving
            if(isIllegalMove()) {
                return Constants.areaVolumeSquared;
            } else {
                //Otherwise returns the distance squared between this point and its parent
                return (prev.point[0] - point[0]) * (prev.point[0] - point[0]) + (prev.point[1] - point[1]) * (prev.point[1] - point[1]) + (prev.point[2] - point[2]) * (prev.point[2] - point[2]);
            }
        }

        //Checks whether moving from the parent to this would cross a KOZ or if this point is out of bounds
        public boolean isIllegalMove() {
            //Checks if this point is out of bounds
            if(point[0] - Constants.avoidance < Constants.minX || point[0] + Constants.avoidance >= Constants.maxX
                    || point[1] - Constants.avoidance < Constants.minY || point[1] + Constants.avoidance >= Constants.maxY
                    || point[2] - Constants.avoidance < Constants.minZ || point[2] + Constants.avoidance >= Constants.maxZ) {
                return true;
            } else {
                //For each zone
                for(int i = 0; i < Constants.minKOZ.length; i++) {
                    //Checks if it may cross due to the zone being within the parent and this when considering with respect to only one axis
                    if((Constants.minKOZ[i][0] >= point[0] && Constants.minKOZ[i][0] <= prev.point[0]) || (Constants.minKOZ[i][0] >= prev.point[0] && Constants.minKOZ[i][0] <= point[0])
                            || (Constants.maxKOZ[i][0] >= point[0] && Constants.maxKOZ[i][0] <= prev.point[0]) || (Constants.maxKOZ[i][0] >= prev.point[0] && Constants.maxKOZ[i][0] <= point[0])
                            || (Constants.minKOZ[i][1] >= point[1] && Constants.minKOZ[i][1] <= prev.point[1]) || (Constants.minKOZ[i][1] >= prev.point[1] && Constants.minKOZ[i][1] <= point[1])
                            || (Constants.maxKOZ[i][1] >= point[1] && Constants.maxKOZ[i][1] <= prev.point[1]) || (Constants.maxKOZ[i][1] >= prev.point[1] && Constants.maxKOZ[i][1] <= point[1])
                            || (Constants.minKOZ[i][2] >= point[2] && Constants.minKOZ[i][2] <= prev.point[2]) || (Constants.minKOZ[i][2] >= prev.point[2] && Constants.minKOZ[i][2] <= point[2])
                            || (Constants.maxKOZ[i][2] >= point[2] && Constants.maxKOZ[i][2] <= prev.point[2]) || (Constants.maxKOZ[i][2] >= prev.point[2] && Constants.maxKOZ[i][2] <= point[2])) {
                        //Calculates the total change in only the X directions
                        float deltaX = point[0] - prev.point[0];
                        //Total change in only the Y direction
                        float deltaY = point[1] - prev.point[1];
                        //Total change in only the Z direction
                        float deltaZ = point[2] - prev.point[2];

                        //Okay to understand this next part you have to understand that this check works on the assumption that a vector only crosses a KOZ if it could span the entire length of the KOZ
                        //with respect to one axis and that: if at either of the two points where it intersects the KOZ in that one axis, the values of the other two axis are contained within the face of
                        //the prism corresponding to that point on the KOZ, then it intersects. We don't have to check anywhere in the middle since we do this check with respect to each of the three axis.

                        //The x-value corresponding to when the vector intersects the face of the prism corresponding to the minKOZ for the axis we are currently checking with respect to
                        float minX;
                        //X-value for the maxKOZ
                        float maxX;
                        //Above for y
                        float minY;
                        float maxY;
                        //Above for z
                        float minZ;
                        float maxZ;
                        //T is used to parametricise this, which is what allows us to calculate the values of the other two axis to see if they intersect a face.
                        //T = 1 is considered the time it takes to go from the parent to this. In calculations of the min/max x/y/z, t should almost always be less than 1.
                        float minT;
                        float maxT;

                        //If the change in X is too small, we may end up with division by zero. Because the x-change is small though, we can assume Astrobee isn't crossing a KOZ with respect to this axis.
                        //TODO: find a non-abritrary threshold value
                        if(Math.abs(deltaX) > 0.01) {
                            //The minimum x will be the min x for the KOZ, since that's where the x intersects the KOZ.
                            minX = Constants.minKOZ[i][0];
                            //Above for x
                            maxX = Constants.maxKOZ[i][0];
                            //Calculates the t it takes for the vector to "reach" the minX, i.e. one of two possible intersection faces
                            minT = (minX - prev.point[0]) / deltaX;
                            //The t for the maxX, the other face the vector may intersect
                            maxT = (maxX - prev.point[0]) / deltaX;
                            //Calculates the y-value when the vector may intersect the face at the minX of the KOZ
                            minY = prev.point[1] + minT * deltaY;
                            //Above calculation using the maxT
                            maxY = prev.point[1] + maxT * deltaY;
                            //Above for Z
                            minZ = prev.point[2] + minT * deltaZ;
                            maxZ = prev.point[2] + maxT * deltaZ;

                            //Checks if it intersects either the "min" face or the "max" face. If so, it returns that moving from the parent to this is illegal
                            if(((minY + Constants.avoidance >= Constants.minKOZ[i][1] && minY - Constants.avoidance <= Constants.minKOZ[i][1])
                                    && (minZ + Constants.avoidance >= Constants.minKOZ[i][2] && minZ - Constants.avoidance <= Constants.minKOZ[i][2]))
                                    || ((maxY + Constants.avoidance >= Constants.minKOZ[i][1] && maxY - Constants.avoidance <= Constants.minKOZ[i][1])
                                    && (maxZ + Constants.avoidance >= Constants.minKOZ[i][2] && maxZ - Constants.avoidance <= Constants.minKOZ[i][2]))) {
                                return true;
                            }
                        }
                        //Above but with respect to y
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
                                    && (minZ + Constants.avoidance >= Constants.minKOZ[i][2] && minZ - Constants.avoidance <= Constants.minKOZ[i][2]))
                                    || ((maxX + Constants.avoidance >= Constants.minKOZ[i][0] && maxX - Constants.avoidance <= Constants.minKOZ[i][0])
                                    && (maxZ + Constants.avoidance >= Constants.minKOZ[i][2] && maxZ - Constants.avoidance <= Constants.minKOZ[i][2]))) {
                                return true;
                            }
                        }
                        //Respect to z
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
                                    && (minY + Constants.avoidance >= Constants.minKOZ[i][1] && minY - Constants.avoidance <= Constants.minKOZ[i][1]))
                                    || ((maxX + Constants.avoidance >= Constants.minKOZ[i][0] && maxX - Constants.avoidance <= Constants.minKOZ[i][0])
                                    && (maxY + Constants.avoidance >= Constants.minKOZ[i][1] && maxY - Constants.avoidance <= Constants.minKOZ[i][1]))) {
                                return true;
                            }
                        }
                    }
                }

                //Otherwise the move is legal
                return false;
            }
        }

        //Necessary due to the use of a PriorityQueue
        public int compareTo(moveData other) {
            return this.queueValue == other.queueValue ? 0 : (this.queueValue < other.queueValue ? -1 : 1);
        }

        //Checks equality using indices. The only "incorrect" thing it does is add a check if the queue value of other is more than this.
        //This is done due to queue.remove(a) removing based on the presence of all elements that fulfill b.equals(a) where b is an element in the queue.
        //Adding a check where an element doesn't get removed if it's more optimal results in less unneccessary adding elements to the queue.
        public boolean equals(moveData other) {
            return this.indices[0] == other.indices[0] && this.indices[1] == other.indices[1] && this.indices[2] == other.indices[2] && this.queueValue <= other.queueValue;
        }

        //Gets the point form of this moveData to make it easier to construct the path at the end
        public Point getPoint() {
            return new Point(point[0], point[1], point[2]);
        }
    }

    public static ArrayList<Point> moveTo(final float[] endpoint) {
        return moveTo(new float[]{(float) myKinematics.getPosition().getX(), (float) myKinematics.getPosition().getY(), (float) myKinematics.getPosition().getZ()}, endpoint);
    }

    //Returns a list of Points to move to
    //Parameter is array and not a point since Points contain double, so to (hopefully) make things faster floats are used when possible
    //Order of parameter and output is {X, Y, Z}
    public static ArrayList<Point> moveTo(float[] current, final float[] endpoint) {
        YourService.myLogger.log(Level.INFO, "MM mt 1. Creating output, queue, and checked");
        //The output points
        ArrayList<Point> output = new ArrayList<Point>();
        //Priority because this is based on Dijkstra's algorithm
        //Blocking since I implemented multi-threading
        final PriorityBlockingQueue<moveData> queue = new PriorityBlockingQueue<moveData>();
        //HashSet prevents duplicate checked indices being added (not that that should happen)
        //Also I think it has better .contains(a) time complexity than another queue or a list
        final HashSet<int[]> checked = new HashSet<int[]>();

        YourService.myLogger.log(Level.INFO, "MM mt 2. Creating beginData and adding it to the queue");
        //Constructs a moveData representation of the beginning point. Default constructor is used since we make queueValue specifically just the airDistance
        final moveData beginData = new moveData();
        beginData.point = new float[]{current[0], current[1], current[2]};
        YourService.myLogger.log(Level.INFO, "MM mt 2.25. Kinematics Point: " + myKinematics.getPosition().getX() + ", " + myKinematics.getPosition().getY() + ", " + myKinematics.getPosition().getZ());
        YourService.myLogger.log(Level.INFO, "MM mt 2.5. Point: " + beginData.point[0] + ", " + beginData.point[1] + ", " + beginData.point[2]);
        beginData.indices = new int[]{(int) ((beginData.point[0] - Constants.minX) / Constants.masterPointsPrecision), (int) ((beginData.point[1] - Constants.minY) / Constants.masterPointsPrecision), (int) ((beginData.point[2] - Constants.minZ) / Constants.masterPointsPrecision)};
        YourService.myLogger.log(Level.INFO, "MM mt 2.75. Indices: " + beginData.indices[0] + ", " + beginData.indices[1] + ", " + beginData.indices[2]);
        beginData.airDistanceSquared = getAirDistanceSquared(beginData.point, endpoint);
        beginData.queueValue = beginData.airDistanceSquared;
        beginData.prev = null;
        queue.add(beginData);

        YourService.myLogger.log(Level.INFO, "MM mt 3. Declaring endData, availableProcessors, and threadArr");
        //We'll reuse this to represent moving from the element removed from the queue to the endpoint
        moveData endData;

        //The number of available processes. Guaranteed to always be at least 1, unless the computer exploded
        int availableProcessors;
        //An array to store all the threads we'll start. Will be processes - 1 since 1 process has to be kept for the main thread
        Thread[] threadArr;

        //While the lowest value element in the queue is not the endpoint
        while(queue.element().point != endpoint) {
            YourService.myLogger.log(Level.INFO, "MM mt 4. Remove head queue element, add it to check, assign stuff and create indices");
            //Remove it so we can expand from it
            moveData tempData = queue.remove();
            //Add it to already checked points so we don't go back to it
            checked.add(tempData.indices);

            availableProcessors = Runtime.getRuntime().availableProcessors();
            threadArr = new Thread[availableProcessors - 1];
            final int xIndex = tempData.indices[0];
            final int yIndex = tempData.indices[1];
            final int zIndex = tempData.indices[2];

            //For each available process
            for(int j = 0; j < availableProcessors; j++) {
                final int i = j;
                //If not the process we're use for main
                if(j < availableProcessors - 1) {
                    YourService.myLogger.log(Level.INFO, "MM mt 5. Spawn thread");
                    //Create a thread that essentially does the following:
                    //Checks the masterPoints around tempData by creating a cube related to a certain spread, and then checks the surface of that cube
                    //If there's a just as fast/faster way to check all perimeter points of a 3d array that is cleaner than the mess I made, please implement it
                    threadArr[i] = new Thread() {
                        public void run() {
                            int spread = i + 1;
                            int[] indices = new int[]{0, 0, 0};

                            YourService.myLogger.log(Level.INFO, "MM mt tr 1. Spread and Indices created. " + xIndex + " " + yIndex + " " + zIndex);
                            for (int y = yIndex - spread; y <= yIndex + spread; y++) {
                                YourService.myLogger.log(Level.INFO, "MM mt tr 2. Inside loop 1, y: " + y);
                                for (int z = zIndex - spread; z <= zIndex + spread; z++) {
                                    YourService.myLogger.log(Level.INFO, "MM mt tr 3. Inside loop 1, z: " + z);
                                    if(y < 0 || y >= Constants.masterPoints[0].length || z < 0 || z >= Constants.masterPoints[0][0].length) {
                                        YourService.myLogger.log(Level.INFO, "MM mt tr 4. Broken: " + y + " " + z);
                                        continue;
                                    }

                                    indices[1] = y;
                                    indices[2] = z;

                                    indices[0] = xIndex - spread;
                                    YourService.myLogger.log(Level.INFO, "MM mt tr 5. Indices reconfigured");

                                    if(!(indices[0] < 0 || indices[0] >= Constants.masterPoints.length)) {
                                        YourService.myLogger.log(Level.INFO, "MM mt tr 6. Good to check indices");
                                        if(!checked.contains(indices)) {
                                            YourService.myLogger.log(Level.INFO, "MM mt tr 7. Indices are good");
                                            moveData newData = new moveData(beginData,
                                                    indices,
                                                    Constants.masterPoints[indices[0]][y][z],
                                                    getAirDistanceSquared(endpoint, Constants.masterPoints[indices[0]][y][z]));

                                            if(queue.contains(newData)) {
                                                YourService.myLogger.log(Level.INFO, "MM mt tr 8. Queue contained newData");
                                                queue.remove(newData);
                                                queue.add(newData);
                                            } else {
                                                YourService.myLogger.log(Level.INFO, "MM mt tr 9. Queue did not contain newData");
                                                queue.add(newData);
                                            }
                                        }
                                    }

                                    indices[0] = xIndex + spread;
                                    YourService.myLogger.log(Level.INFO, "MM mt tr 10. Other xIndex");

                                    if(!(indices[0] < 0 || indices[0] >= Constants.masterPoints.length)) {
                                        YourService.myLogger.log(Level.INFO, "MM mt tr 6. Good to check indices");
                                        if(!checked.contains(indices)) {
                                            YourService.myLogger.log(Level.INFO, "MM mt tr 7. Indices are good");
                                            moveData newData = new moveData(beginData,
                                                    indices,
                                                    Constants.masterPoints[indices[0]][y][z],
                                                    getAirDistanceSquared(endpoint, Constants.masterPoints[indices[0]][y][z]));

                                            if(queue.contains(newData)) {
                                                YourService.myLogger.log(Level.INFO, "MM mt tr 8. Queue contained newData");
                                                queue.remove(newData);
                                                queue.add(newData);
                                            } else {
                                                YourService.myLogger.log(Level.INFO, "MM mt tr 9. Queue did not contain newData");
                                                queue.add(newData);
                                            }
                                        }
                                    }
                                }
                            }

                            for (int x = xIndex - spread + 1; x <= xIndex + spread - 1; x++) {
                                for (int z = zIndex - spread; z <= zIndex + spread; z++) {
                                    if(x < 0 || x >= Constants.masterPoints.length || z < 0 || z >= Constants.masterPoints[0][0].length) {
                                        continue;
                                    }

                                    indices[0] = x;
                                    indices[2] = z;

                                    indices[1] = yIndex - spread;

                                    if(!(indices[1] < 0 || indices[1] >= Constants.masterPoints[0].length)) {
                                        YourService.myLogger.log(Level.INFO, "MM mt tr 6. Good to check indices");
                                        if(!checked.contains(indices)) {
                                            YourService.myLogger.log(Level.INFO, "MM mt tr 7. Indices are good");
                                            moveData newData = new moveData(beginData,
                                                    indices,
                                                    Constants.masterPoints[x][indices[1]][z],
                                                    getAirDistanceSquared(endpoint, Constants.masterPoints[x][indices[1]][z]));

                                            if(queue.contains(newData)) {
                                                YourService.myLogger.log(Level.INFO, "MM mt tr 8. Queue contained newData");
                                                queue.remove(newData);
                                                queue.add(newData);
                                            } else {
                                                YourService.myLogger.log(Level.INFO, "MM mt tr 9. Queue did not contain newData");
                                                queue.add(newData);
                                            }
                                        }
                                    }

                                    indices[1] = yIndex + spread;

                                    if(!(indices[1] < 0 || indices[1] >= Constants.masterPoints[0].length)) {
                                        YourService.myLogger.log(Level.INFO, "MM mt tr 6. Good to check indices");
                                        if(!checked.contains(indices)) {
                                            YourService.myLogger.log(Level.INFO, "MM mt tr 7. Indices are good");
                                            moveData newData = new moveData(beginData,
                                                    indices,
                                                    Constants.masterPoints[x][indices[1]][z],
                                                    getAirDistanceSquared(endpoint, Constants.masterPoints[x][indices[1]][z]));

                                            if(queue.contains(newData)) {
                                                YourService.myLogger.log(Level.INFO, "MM mt tr 8. Queue contained newData");
                                                queue.remove(newData);
                                                queue.add(newData);
                                            } else {
                                                YourService.myLogger.log(Level.INFO, "MM mt tr 9. Queue did not contain newData");
                                                queue.add(newData);
                                            }
                                        }
                                    }
                                }
                            }

                            for (int x = xIndex - spread + 1; x <= xIndex + spread - 1; x++) {
                                for (int y = yIndex - spread + 1; y <= yIndex + spread - 1; y++) {
                                    if(x < 0 || x >= Constants.masterPoints.length || y < 0 || y >= Constants.masterPoints[0].length) {
                                        continue;
                                    }

                                    indices[0] = x;
                                    indices[1] = y;

                                    indices[2] = zIndex - spread;

                                    if(!(indices[2] < 0 || indices[2] >= Constants.masterPoints[0][0].length)) {
                                        YourService.myLogger.log(Level.INFO, "MM mt tr 6. Good to check indices");
                                        if(!checked.contains(indices)) {
                                            YourService.myLogger.log(Level.INFO, "MM mt tr 7. Indices are good");
                                            moveData newData = new moveData(beginData,
                                                    indices,
                                                    Constants.masterPoints[x][y][indices[2]],
                                                    getAirDistanceSquared(endpoint, Constants.masterPoints[x][y][indices[2]]));

                                            if(queue.contains(newData)) {
                                                YourService.myLogger.log(Level.INFO, "MM mt tr 8. Queue contained newData");
                                                queue.remove(newData);
                                                queue.add(newData);
                                            } else {
                                                YourService.myLogger.log(Level.INFO, "MM mt tr 9. Queue did not contain newData");
                                                queue.add(newData);
                                            }
                                        }
                                    }

                                    indices[2] = zIndex + spread;

                                    if(!(indices[2] < 0 || indices[2] >= Constants.masterPoints[0][0].length)) {
                                        YourService.myLogger.log(Level.INFO, "MM mt tr 6. Good to check indices");
                                        if(!checked.contains(indices)) {
                                            YourService.myLogger.log(Level.INFO, "MM mt tr 7. Indices are good");
                                            moveData newData = new moveData(beginData,
                                                    indices,
                                                    Constants.masterPoints[x][y][indices[2]],
                                                    getAirDistanceSquared(endpoint, Constants.masterPoints[x][y][indices[2]]));

                                            if(queue.contains(newData)) {
                                                YourService.myLogger.log(Level.INFO, "MM mt tr 8. Queue contained newData");
                                                queue.remove(newData);
                                                queue.add(newData);
                                            } else {
                                                YourService.myLogger.log(Level.INFO, "MM mt tr 9. Queue did not contain newData");
                                                queue.add(newData);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    };
                    threadArr[i].start();
                } else {
                    YourService.myLogger.log(Level.INFO, "MM mt 6. Run in main");
                    int spread = i + 1;
                    int[] indices = new int[]{0, 0, 0};

                    for (int y = yIndex - spread; y <= yIndex + spread; y++) {
                        for (int z = zIndex - spread; z <= zIndex + spread; z++) {
                            if(y < 0 || y >= Constants.masterPoints[0].length || z < 0 || z >= Constants.masterPoints[0][0].length) {
                                break;
                            }

                            indices[1] = y;
                            indices[2] = z;

                            indices[0] = xIndex - spread;

                            if(!(indices[0] < 0 || indices[0] >= Constants.masterPoints.length)) {
                                if(!checked.contains(indices)) {
                                    moveData newData = new moveData(beginData,
                                            indices,
                                            Constants.masterPoints[indices[0]][y][z],
                                            getAirDistanceSquared(endpoint, Constants.masterPoints[indices[0]][y][z]));

                                    queue.remove(newData);
                                    queue.add(newData);
                                }
                            }

                            indices[0] = xIndex + spread;

                            if(!(indices[0] < 0 || indices[0] >= Constants.masterPoints.length)) {
                                if(!checked.contains(indices)) {
                                    moveData newData = new moveData(beginData,
                                            indices,
                                            Constants.masterPoints[indices[0]][y][z],
                                            getAirDistanceSquared(endpoint, Constants.masterPoints[indices[0]][y][z]));

                                    queue.remove(newData);
                                    queue.add(newData);
                                }
                            }
                        }
                    }

                    for (int x = xIndex - spread + 1; x <= xIndex + spread - 1; x++) {
                        for (int z = zIndex - spread; z <= zIndex + spread; z++) {
                            if(x < 0 || x >= Constants.masterPoints.length || z < 0 || z >= Constants.masterPoints[0][0].length) {
                                break;
                            }

                            indices[0] = x;
                            indices[2] = z;

                            indices[1] = yIndex - spread;

                            if(!(indices[1] < 0 || indices[1] >= Constants.masterPoints[0].length)) {
                                if (!checked.contains(indices)) {
                                    moveData newData = new moveData(beginData,
                                            indices,
                                            Constants.masterPoints[x][indices[1]][z],
                                            getAirDistanceSquared(endpoint, Constants.masterPoints[x][indices[1]][z]));

                                    queue.remove(newData);
                                    queue.add(newData);
                                }
                            }

                            indices[1] = yIndex + spread;

                            if(!(indices[1] < 0 || indices[1] >= Constants.masterPoints[0].length)) {
                                if (!checked.contains(indices)) {
                                    moveData newData = new moveData(beginData,
                                            indices,
                                            Constants.masterPoints[x][indices[1]][z],
                                            getAirDistanceSquared(endpoint, Constants.masterPoints[x][indices[1]][z]));

                                    queue.remove(newData);
                                    queue.add(newData);
                                }
                            }
                        }
                    }

                    for (int x = xIndex - spread + 1; x <= xIndex + spread - 1; x++) {
                        for (int y = yIndex - spread + 1; y <= yIndex + spread - 1; y++) {
                            if(x < 0 || x >= Constants.masterPoints.length || y < 0 || y >= Constants.masterPoints[0].length) {
                                break;
                            }

                            indices[0] = x;
                            indices[1] = y;

                            indices[2] = zIndex - spread;

                            if(!(indices[2] < 0 || indices[2] >= Constants.masterPoints[0][0].length)) {
                                if (!checked.contains(indices)) {
                                    moveData newData = new moveData(beginData,
                                            indices,
                                            Constants.masterPoints[x][y][indices[2]],
                                            getAirDistanceSquared(endpoint, Constants.masterPoints[x][y][indices[2]]));

                                    queue.remove(newData);
                                    queue.add(newData);
                                }
                            }

                            indices[2] = zIndex + spread;

                            if(!(indices[2] < 0 || indices[2] >= Constants.masterPoints[0][0].length)) {
                                if (!checked.contains(indices)) {
                                    moveData newData = new moveData(beginData,
                                            indices,
                                            Constants.masterPoints[x][y][indices[2]],
                                            getAirDistanceSquared(endpoint, Constants.masterPoints[x][y][indices[2]]));

                                    queue.remove(newData);
                                    queue.add(newData);
                                }
                            }
                        }
                    }
                }
            }

            YourService.myLogger.log(Level.INFO, "MM mt 7. Joining threads and creating endData and checking if it should be added");
            //Have to make sure all the threads have finished before we move on
            for(Thread thread: threadArr) {
                try {
                    thread.join();
                } catch(InterruptedException e) {
                    YourService.myLogger.log(Level.INFO, "MM mt 9. Error: " + e.getMessage());
                }
            }

            //Creates an endData using tempData as the parent
            endData = new moveData(tempData, endpoint);

            //If it couldn't get there, it wouldn't fulfill this condition.
            //Otherwise it gets added and the loop will break when the condition gets checked since it will be the first element in the queue
            if(endData.queueValue < Constants.areaVolumeSquared) {
                queue.add(endData);
            }
        }

        YourService.myLogger.log(Level.INFO, "MM mt 8. Constructing path through reverse point traversal");
        //Constructs the list of output points in reverse
        moveData wow = queue.remove();
        //Adds the last child (endpoint) to the list
        output.add(wow.getPoint());
        //Goes back one
        wow = wow.prev;

        //While there are still parents
        while(wow != null) {
            //Adds them in front of all their children
            output.add(0, wow.getPoint());
            //Goes to their parent
            wow = wow.prev;
        }

        //Returns the list of points in the correct order since we inserted the reverse list in reverse order
        return output;
    }

    //Returns the square of the distance between the given point and the endpoint. Again, square to save on computation
    public static float getAirDistanceSquared(float[] endpoint, float[] point) {
        return (point[0] - endpoint[0]) * (point[0] - endpoint[0]) + (point[1] - endpoint[1]) * (point[1] - endpoint[1]) + (point[2] - endpoint[2]) * (point[2] - endpoint[2]);
    }
}