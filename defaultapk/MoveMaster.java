package jp.jaxa.iss.kibo.rpc.sampleapk;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.Kinematics;

import java.util.*;

//This class is used to generate the points for Kibo to move to to avoid KOZ
//It does not generate any quaternion info
public class MoveMaster {
    //Used to get the current position of Kibo
    public static Kinematics myKinematics;

    public static class intArrHelper {
        public int[] arr;

        public intArrHelper() {
            this(new int[3]);
        }


        public intArrHelper(int[] arr) {
            this.arr = arr;
        }

        public boolean equals(intArrHelper other) {
            return this.arr[0] == other.arr[0] && this.arr[1] == other.arr[1] &&this.arr[2] == other.arr[2];
        }

        public String toString() {
            return "intArrHelper indices are: " + arr[0] + ", " + arr[1] + ", " + arr[2];
        }
    }

    //Helper class that's linked such that it sees its parents but not its children
    public static class moveData implements Comparable<moveData> {
        //Parent moveData
        public moveData prev;
        //The corresponding indices {X, Y, Z} of the point in the masterPoints list (see Constants.java)
        //Not used for the beginning or end point
        public intArrHelper indices;
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
            this(prev, new intArrHelper(new int[]{-1, -1, -1}), point, 0f);
        }

        //Used to generate all other points
        public moveData(moveData prev, intArrHelper indices, float[] point, float airDistanceSquared) {
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
                return Constants.Calculations.KIZ1VolumeSquared + Constants.Calculations.KIZ2VolumeSquared;
            } else {
                //Otherwise returns the distance squared between this point and its parent
                return (prev.point[0] - point[0]) * (prev.point[0] - point[0]) + (prev.point[1] - point[1]) * (prev.point[1] - point[1]) + (prev.point[2] - point[2]) * (prev.point[2] - point[2]);
            }
        }

        //Checks whether moving from the parent to this would cross a KOZ or if this point is out of bounds
        public boolean isIllegalMove() {
            //Checks if this point is out of bounds
            YourService.log("Prev: " + prev.point[0] + ", " + prev.point[1] + ", " + prev.point[2]);
            YourService.log("Current: " + point[0] + ", " + point[1] + ", " + point[2]);
            if(point[0] - Constants.Calculations.avoidance < Constants.GameData.KIZ[0].min[0] || point[0] + Constants.Calculations.avoidance > Constants.GameData.KIZ[0].max[0]
                    || point[1] - Constants.Calculations.avoidance < Constants.GameData.KIZ[0].min[1] || point[1] + Constants.Calculations.avoidance > Constants.GameData.KIZ[0].max[1]
                    || point[2] - Constants.Calculations.avoidance < Constants.GameData.KIZ[0].min[2] || point[2] + Constants.Calculations.avoidance > Constants.GameData.KIZ[0].max[2]) {
                YourService.log("Not in KIZ 1");
                return true;
            } else if(point[0] - Constants.Calculations.avoidance < Constants.GameData.KIZ[1].min[0] || point[0] + Constants.Calculations.avoidance > Constants.GameData.KIZ[1].max[0]
                    || point[1] - Constants.Calculations.avoidance < Constants.GameData.KIZ[1].min[1] || point[1] + Constants.Calculations.avoidance > Constants.GameData.KIZ[1].max[1]
                    || point[2] - Constants.Calculations.avoidance < Constants.GameData.KIZ[1].min[2] || point[2] + Constants.Calculations.avoidance > Constants.GameData.KIZ[1].max[2]) {
                YourService.log("Not in KIZ 2");
                return true;
            } else {
                //For each zone
                for (int i = 0; i < Constants.GameData.KOZ.length; i++) {
                    //Checks if it may cross due to the zone being within the parent and this when considering with respect to only one axis
                    if ((Constants.GameData.KOZ[i].min[0] >= point[0] && Constants.GameData.KOZ[i].min[0] <= prev.point[0]) || (Constants.GameData.KOZ[i].min[0] >= prev.point[0] && Constants.GameData.KOZ[i].min[0] <= point[0])
                            || (Constants.GameData.KOZ[i].max[0] >= point[0] && Constants.GameData.KOZ[i].max[0] <= prev.point[0]) || (Constants.GameData.KOZ[i].max[0] >= prev.point[0] && Constants.GameData.KOZ[i].max[0] <= point[0])
                            || (Constants.GameData.KOZ[i].min[1] >= point[1] && Constants.GameData.KOZ[i].min[1] <= prev.point[1]) || (Constants.GameData.KOZ[i].min[1] >= prev.point[1] && Constants.GameData.KOZ[i].min[1] <= point[1])
                            || (Constants.GameData.KOZ[i].max[1] >= point[1] && Constants.GameData.KOZ[i].max[1] <= prev.point[1]) || (Constants.GameData.KOZ[i].max[1] >= prev.point[1] && Constants.GameData.KOZ[i].max[1] <= point[1])
                            || (Constants.GameData.KOZ[i].min[2] >= point[2] && Constants.GameData.KOZ[i].min[2] <= prev.point[2]) || (Constants.GameData.KOZ[i].min[2] >= prev.point[2] && Constants.GameData.KOZ[i].min[2] <= point[2])
                            || (Constants.GameData.KOZ[i].max[2] >= point[2] && Constants.GameData.KOZ[i].max[2] <= prev.point[2]) || (Constants.GameData.KOZ[i].max[2] >= prev.point[2] && Constants.GameData.KOZ[i].max[2] <= point[2])) {
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
                        if (Math.abs(deltaX) > 0.01) {
                            //The minimum x will be the min x for the KOZ, since that's where the x intersects the KOZ.
                            minX = Constants.GameData.KOZ[i].min[0];
                            //Above for x
                            maxX = Constants.GameData.KOZ[i].max[0];
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
                            if (((minY + Constants.Calculations.avoidance >= Constants.GameData.KOZ[i].min[1] && minY - Constants.Calculations.avoidance <= Constants.GameData.KOZ[i].max[1])
                                    && (minZ + Constants.Calculations.avoidance >= Constants.GameData.KOZ[i].min[2] && minZ - Constants.Calculations.avoidance <= Constants.GameData.KOZ[i].max[2]))
                                    || ((maxY + Constants.Calculations.avoidance >= Constants.GameData.KOZ[i].min[1] && maxY - Constants.Calculations.avoidance <= Constants.GameData.KOZ[i].max[1])
                                    && (maxZ + Constants.Calculations.avoidance >= Constants.GameData.KOZ[i].min[2] && maxZ - Constants.Calculations.avoidance <= Constants.GameData.KOZ[i].max[2]))) {
                                YourService.log(i + ": Bad X Check");
                                return true;
                            }
                        }
                        //Above but with respect to y
                        if (Math.abs(deltaY) > 0.01) {
                            minY = Constants.GameData.KOZ[i].min[1];
                            maxY = Constants.GameData.KOZ[i].max[1];
                            minT = (minY - prev.point[1]) / deltaY;
                            maxT = (maxY - prev.point[1]) / deltaY;
                            minX = prev.point[0] + minT * deltaX;
                            maxX = prev.point[0] + maxT * deltaX;
                            minZ = prev.point[2] + minT * deltaZ;
                            maxZ = prev.point[2] + maxT * deltaZ;

                            if (((minX + Constants.Calculations.avoidance >= Constants.GameData.KOZ[i].min[0] && minX - Constants.Calculations.avoidance <= Constants.GameData.KOZ[i].max[0])
                                    && (minZ + Constants.Calculations.avoidance >= Constants.GameData.KOZ[i].min[2] && minZ - Constants.Calculations.avoidance <= Constants.GameData.KOZ[i].max[2]))
                                    || ((maxX + Constants.Calculations.avoidance >= Constants.GameData.KOZ[i].min[0] && maxX - Constants.Calculations.avoidance <= Constants.GameData.KOZ[i].max[0])
                                    && (maxZ + Constants.Calculations.avoidance >= Constants.GameData.KOZ[i].min[2] && maxZ - Constants.Calculations.avoidance <= Constants.GameData.KOZ[i].max[2]))) {
                                YourService.log(i + ": Bad Y Check");
                                return true;
                            }
                        }
                        //Respect to z
                        if (Math.abs(deltaZ) > 0.01) {
                            minZ = Constants.GameData.KOZ[i].min[2];
                            maxZ = Constants.GameData.KOZ[i].max[2];
                            minT = (minZ - prev.point[2]) / deltaZ;
                            maxT = (maxZ - prev.point[2]) / deltaZ;
                            minX = prev.point[0] + minT * deltaX;
                            maxX = prev.point[0] + maxT * deltaX;
                            minY = prev.point[1] + minT * deltaY;
                            maxY = prev.point[1] + maxT * deltaY;

                            if (((minX + Constants.Calculations.avoidance >= Constants.GameData.KOZ[i].min[0] && minX - Constants.Calculations.avoidance <= Constants.GameData.KOZ[i].max[0])
                                    && (minY + Constants.Calculations.avoidance >= Constants.GameData.KOZ[i].min[1] && minY - Constants.Calculations.avoidance <= Constants.GameData.KOZ[i].max[1]))
                                    || ((maxX + Constants.Calculations.avoidance >= Constants.GameData.KOZ[i].min[0] && maxX - Constants.Calculations.avoidance <= Constants.GameData.KOZ[i].max[0])
                                    && (maxY + Constants.Calculations.avoidance >= Constants.GameData.KOZ[i].min[1] && maxY - Constants.Calculations.avoidance <= Constants.GameData.KOZ[i].max[1]))) {
                                YourService.log(i + ": Bad Z Check");
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
            return Float.compare(this.queueValue, other.queueValue);
        }

        //Checks equality using indices. The only "incorrect" thing it does is add a check if the queue value of other is more than this.
        //This is done due to queue.remove(a) removing based on the presence of all elements that fulfill b.equals(a) where b is an element in the queue.
        //Adding a check where an element doesn't get removed if it's more optimal results in less unneccessary adding elements to the queue.
        public boolean equals(moveData other) {
            return this.indices.equals(other.indices);
        }

        //Gets the point form of this moveData to make it easier to construct the path at the end
        public Point getPoint() {
            return new Point(point[0], point[1], point[2]);
        }
    }

    public static ArrayList<Point> moveTo(float[] endpoint) {
        return moveTo(new float[]{(float) myKinematics.getPosition().getX(), (float) myKinematics.getPosition().getY(), (float) myKinematics.getPosition().getZ()}, endpoint);
    }

    //Returns a list of Points to move to
    //Parameter is array and not a point since Points contain double, so to (hopefully) make things faster floats are used when possible
    //Order of parameter and output is {X, Y, Z}
    //TODO: Make it so it can go between zones
    public static ArrayList<Point> moveTo(float[] current, float[] endpoint) {
        YourService.log("Creating all the stuff");
        //The output points
        ArrayList<Point> output = new ArrayList<Point>();
        //Based on Dijkstra's algorithm
        //Need to be able to easily insert elements
        ArrayList<moveData> queue = new ArrayList<moveData>();
        //HashSet prevents duplicate checked indices being added (not that that should happen)
        //Also I think it has better .contains(a) time complexity than another queue or a list
        HashSet<intArrHelper> checked = new HashSet<intArrHelper>();
        //Used to temporarily store all points to be added. Lets O(n) operations on the queue to be reduced
        PriorityQueue<moveData> temporaryQueue = new PriorityQueue<moveData>();
        //The iterator to be used for this queue
        Iterator<moveData> temporaryQueueIterator;

        //Constructs a moveData representation of the beginning point. Default constructor is used since we make queueValue specifically just the airDistance
        moveData beginData = new moveData();
        beginData.point = new float[]{current[0], current[1], current[2]};
        beginData.indices = new intArrHelper(new int[]{
                (int) ((beginData.point[0] - Constants.GameData.KIZ[0].min[0]) / Constants.Calculations.masterPointsPrecision),
                (int) ((beginData.point[1] - Constants.GameData.KIZ[0].min[1]) / Constants.Calculations.masterPointsPrecision),
                (int) ((beginData.point[2] - Constants.GameData.KIZ[0].min[2]) / Constants.Calculations.masterPointsPrecision)});
        beginData.airDistanceSquared = getAirDistanceSquared(beginData.point, endpoint);
        beginData.queueValue = beginData.airDistanceSquared;
        beginData.prev = null;
        YourService.log("Finished creating stuff. Moving on to queue things");
        queue.add(beginData);

        //We'll reuse this to represent moving from the element removed from the queue to the endpoint
        moveData endData;

        //While the lowest value element in the queue is not the endpoint
        while(queue.get(0).point != endpoint) {
            //Remove it so we can expand from it
            moveData tempData = queue.remove(0);

            while(checked.contains(tempData.indices)) {
                YourService.log("Failed check. " + tempData.indices.toString());
                tempData = queue.remove(0);
            }

            //Add it to already checked points so we don't go back to it
            checked.add(tempData.indices);

            YourService.log("Passed check. " + tempData.indices.toString());

            for(int x = tempData.indices.arr[0] - 1; x <= tempData.indices.arr[0] + 1; x++) {
                if(x < 0 || x >= Constants.Calculations.KIZ1masterPoints.length) {
                    YourService.log("Illegal X: " + x);
                    continue;
                }
                for(int y = tempData.indices.arr[1] - 1; y <= tempData.indices.arr[1] + 1; y++) {
                    if(y < 0 || y >= Constants.Calculations.KIZ1masterPoints[0].length) {
                        YourService.log("Illegal Y: " + y);
                        continue;
                    }
                    for(int z = tempData.indices.arr[2] - 1; z <= tempData.indices.arr[2] + 1; z++) {
                        if(z < 0 || z >= Constants.Calculations.KIZ1masterPoints[0][0].length) {
                            YourService.log("Illegal Z: " + z);
                            continue;
                        }

                        moveData newData = new moveData(
                                tempData,
                                new intArrHelper(new int[]{x, y, z}),
                                Constants.Calculations.KIZ1masterPoints[x][y][z],
                                getAirDistanceSquared(endpoint, Constants.Calculations.KIZ1masterPoints[x][y][z]));

                        if(!checked.contains(newData.indices)) {
                            YourService.log("Adding newData to queue. " + newData.indices.toString());
                            temporaryQueue.add(newData);
                        }
                    }
                }
            }

            //Creates an endData using tempData as the parent
            YourService.log("Creating endData and adding to queue");
            endData = new moveData(tempData, endpoint);
            temporaryQueue.add(endData);

            temporaryQueueIterator = temporaryQueue.iterator();
            moveData newData;

            do {
                YourService.log("Grabbing next thing in queue");
                newData = temporaryQueueIterator.next();

                if(newData.queueValue >= Constants.Calculations.KIZ1VolumeSquared + Constants.Calculations.KIZ2VolumeSquared) {
                    YourService.log("newData queueValue too big");
                    continue;
                }

                if(queue.size() == 0) {
                    queue.add(newData);
                    continue;
                }

                int left = 0;
                int middle = queue.size() / 2;
                int right = queue.size() - 1;

                while(!(newData.compareTo(queue.get(middle)) <= 0 && (middle == 0 || newData.compareTo(queue.get(middle - 1)) >= 0))) {
                    YourService.log("QueueValue: " + newData.queueValue + ", MiddleValue: " + queue.get(middle).queueValue);
                    if(newData.compareTo(queue.get(middle)) <= 0) {
                        right = middle;
                    } else {
                        left = middle;
                    }

                    middle = (left + right) / 2;
                    YourService.log("Middle: " + middle);
                }

                boolean lessThan = false;

                for(int i = middle - 1; i >= 0; i--) {
                    if(newData.equals(queue.get(i))) {
                        YourService.log("There was something lower, breaking");
                        lessThan = true;
                        break;
                    }
                }

                if(lessThan) {
                    continue;
                }

                for(int i = middle; i < queue.size(); i++) {
                    if(newData.equals(queue.get(i))) {
                        YourService.log(i + " was removed");
                        queue.remove(i);
                        i--;
                    }
                }

                YourService.log("Adding to queue");
                queue.add(middle, newData);
            } while(temporaryQueueIterator.hasNext());
        }

        YourService.log("Constructing list in reverse");
        //Constructs the list of output points in reverse
        YourService.log("Removing first queue element");
        moveData wow = queue.remove(0);
        //Adds the last child (endpoint) to the list
        YourService.log("Getting point");
        output.add(wow.getPoint());
        //Goes back one
        YourService.log("Setting wow to parent");
        wow = wow.prev;

        //While there are still parents
        while(wow != null) {
            //Adds them in front of all their children
            YourService.log("Getting point");
            output.add(0, wow.getPoint());
            //Goes to their parent
            YourService.log("Setting wow to parent");
            wow = wow.prev;
        }

        //Returns the list of points in the correct order since we inserted the reverse list in reverse order
        YourService.log("Finished constructing path");
        return output;
    }

    //Returns the square of the distance between the given point and the endpoint. Again, square to save on computation
    public static float getAirDistanceSquared(float[] endpoint, float[] point) {
        return (point[0] - endpoint[0]) * (point[0] - endpoint[0]) + (point[1] - endpoint[1]) * (point[1] - endpoint[1]) + (point[2] - endpoint[2]) * (point[2] - endpoint[2]);
    }
}