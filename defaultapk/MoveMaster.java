package jp.jaxa.iss.kibo.rpc.sampleapk;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.Kinematics;

import java.util.*;

//This class is used to generate the points for Bee to move to to avoid KOZ
//It does not generate any quaternion info
class MoveMaster {
    //Used to get the current position of Bee
    static Kinematics myKinematics;

    //Used to store a Path up to a point, since we use Dijkstra's to explore possible paths
    private static class Path {
        //The path before this one
        private Path prev;
        //The list of new points that make this path an add on of the previous
        private ArrayList<Point> points;
        //The length traveled up to the last point in this path (includes previous path's arcLength)
        private double arcLength;
        //The distance to the endpoint, assuming there is no obstacles
        private double airDistance;
        //The value used for its ordering in the queue
        private double queueValue;

        //Default constructor, used for beginPath
        Path() {}

        Path(Path prev, ArrayList<Point> points, Point endpoint) {
            this.prev = prev;
            this.points = points;
            this.arcLength = prev.arcLength + arcLengthSquared();
            this.airDistance = airDistanceSquared(endpoint);
            this.queueValue = arcLength + airDistance;
        }

        //Gets the arcLength of just between the points in the points ArrayList. Squared because everything is
        private double arcLengthSquared() {
            double sum = 0;

            for(int i = 0; i < points.size() - 1; i++) {
                sum += (points.get(i + 1).getX() - points.get(i).getX()) * (points.get(i + 1).getX() - points.get(i).getX())
                        + (points.get(i + 1).getY() - points.get(i).getY()) * (points.get(i + 1).getY() - points.get(i).getY())
                        + (points.get(i + 1).getZ() - points.get(i).getZ()) * (points.get(i + 1).getZ() - points.get(i).getZ());
            }

            return sum;
        }

        //Distance to endpoint assuming there's no obstacles
        private double airDistanceSquared(Point endpoint) {
            return (points.get(points.size() - 1).getX() - endpoint.getX()) * (points.get(points.size() - 1).getX() - endpoint.getX())
                    + (points.get(points.size() - 1).getY() - endpoint.getY()) * (points.get(points.size() - 1).getY() - endpoint.getY())
                    + (points.get(points.size() - 1).getZ() - endpoint.getZ()) * (points.get(points.size() - 1).getZ() - endpoint.getZ());
        }

        //Returns true if other would be later in the queue than this
        //Otherwise false
        boolean compareTo(Path other) {
            return this.queueValue < other.queueValue;
        }

        //Checks if the last point in the ArrayList equals the last point of some other path
        boolean equals(Path other) {
            return Constants.Calculations.customPointEquals(this.points.get(this.points.size() - 1), other.points.get(other.points.size() - 1));
        }
    }

    //Uses the position returned by kinematics as the start point. Don't use if kinematics is bad
    public static ArrayList<Point> moveTo(Point endpoint) {
        return moveTo(myKinematics.getPosition(), endpoint);
    }

    //Returns a list of Points, in order, to go to to reach endpoint without hitting KOZs
    //TODO: Make if so it can go between zones when there are no KOZ in the way
    static ArrayList<Point> moveTo(Point beginpoint, Point endpoint) {
        YourService.log("Begin moveTo. Creating all the variables");
        ArrayList<Point> output = new ArrayList<Point>();
        ArrayList<Path> queue = new ArrayList<Path>();
        Point[][] pointArr = new Point[9][3];
        ArrayList<Point> newPathPoints = new ArrayList<Point>();
        Path currentPath;
        Point currentPoint;
        double currentMagnitude, currentAngleZ, currentAngleXY;
        double minMagnitude, maxMagnitude, minAngleZ, maxAngleZ, minAngleXY, maxAngleXY;
        double endMagnitude = endpoint.getX() * endpoint.getX() + endpoint.getY() * endpoint.getY() + endpoint.getZ() * endpoint.getZ();
        double endAngleZ = Math.atan2(endpoint.getY(), endpoint.getX());
        double endAngleXY = Math.atan2(endpoint.getZ(), Math.sqrt(endpoint.getX() * endpoint.getX() + endpoint.getY() * endpoint.getY()));

        YourService.log("beginPath construction");
        Path beginPath = new Path();
        beginPath.prev = null;
        beginPath.points = new ArrayList<Point>();
        beginPath.points.add(beginpoint);
        beginPath.arcLength = 0d;
        beginPath.airDistance = beginPath.airDistanceSquared(endpoint);
        beginPath.queueValue = beginPath.airDistance;

        currentPath = beginPath;
        currentPoint = beginpoint;

        while(!(Constants.Calculations.customPointEquals(currentPoint, endpoint))) {
            YourService.log("Creating magnitude + angle stuff and finding min/max magnitude");
            currentMagnitude = currentPoint.getX() * currentPoint.getX() + currentPoint.getY() * currentPoint.getY() + currentPoint.getZ() * currentPoint.getZ();
            currentAngleZ = Math.atan2(currentPoint.getY(), currentPoint.getX());
            currentAngleXY = Math.atan2(currentPoint.getZ(), Math.sqrt(currentPoint.getX() * currentPoint.getX() + currentPoint.getY() * currentPoint.getY()));

            if(currentMagnitude < endMagnitude) {
                minMagnitude = currentMagnitude;
                maxMagnitude = endMagnitude;
            } else {
                minMagnitude = endMagnitude;
                maxMagnitude = currentMagnitude;
            }

            if(currentAngleZ < endAngleZ) {
                minAngleZ = currentAngleZ;
                maxAngleZ = endAngleZ;
            } else {
                minAngleZ = endAngleZ;
                maxAngleZ = currentAngleZ;
            }

            if(currentAngleXY < endAngleXY) {
                minAngleXY = currentAngleXY;
                maxAngleXY = endAngleXY;
            } else {
                minAngleXY = endAngleXY;
                maxAngleXY = currentAngleXY;
            }

            YourService.log("Figuring out if it crosses any zones");
            boolean crosses = false;
            int kozIndex = -1;

            //Changes order we traverse through KOZ based on whether we're trying to go somewhere of lesser or greater magnitude
            if(minMagnitude == currentMagnitude) {
                for(int i = 0; i < Constants.GameData.KOZ.length; i++) {
                    if(Constants.GameData.KOZ[i].couldCross(minMagnitude, maxMagnitude, minAngleZ, maxAngleZ, minAngleXY, maxAngleXY)
                        && Constants.GameData.KOZ[i].crosses(currentPoint, endpoint)) {
                        crosses = true;
                        kozIndex = i;
                        break;
                    }
                }
            } else {
                for(int i = Constants.GameData.KOZ.length - 1; i >= 0; i--) {
                    if(Constants.GameData.KOZ[i].couldCross(minMagnitude, maxMagnitude, minAngleZ, maxAngleZ, minAngleXY, maxAngleXY)
                        && Constants.GameData.KOZ[i].crosses(currentPoint, endpoint)) {
                        crosses = true;
                        kozIndex = i;
                        break;
                    }
                }
            }

            //If it doesn't cross, than it can go to the endpoint. Also guaranteed that endpoint will be the first in the queue since the queueValue will equal currentPoint.queueValue
            //(Arclength += currentPoint.airDistance and then endpoint.airDistance = 0 so currentPoint.queueValue == endPoint.queueValue)
            if(!crosses) {
                YourService.log("Crossed no zones. Adding path with endpoint to the queue");
                newPathPoints.clear();
                newPathPoints.add(endpoint);
                queue.add(0, new Path(currentPath, newPathPoints, endpoint));
            } else {
                YourService.log("Crossed " + kozIndex + ". Creating crossedZone and averages");
                Constants.Zone crossedZone = Constants.GameData.KOZ[kozIndex];
                //Average since we're trying to bridge half the value on the way to some face of the KOZ and then bridging the other half from the KOZ to the endpoint
                double averageX = (currentPoint.getX() + endpoint.getX()) / 2d;
                double averageY = (currentPoint.getY() + endpoint.getY()) / 2d;
                double averageZ = (currentPoint.getZ() + endpoint.getZ()) / 2d;

                YourService.log("Assigning pointArr stuff");
                pointArr[0] = new Point[]{new Point(crossedZone.vertices[0].getX(), crossedZone.vertices[0].getY(), averageZ), new Point(crossedZone.vertices[0].getX(), crossedZone.vertices[7].getY(), averageZ)};
                pointArr[1] = new Point[]{new Point(crossedZone.vertices[0].getX(), crossedZone.vertices[0].getY(), averageZ), new Point(crossedZone.vertices[7].getX(), crossedZone.vertices[0].getY(), averageZ)};
                pointArr[2] = new Point[]{new Point(crossedZone.vertices[0].getX(), crossedZone.vertices[0].getY(), averageZ), new Point(crossedZone.vertices[7].getX(), crossedZone.vertices[7].getY(), averageZ)};
                pointArr[3] = new Point[]{new Point(crossedZone.vertices[0].getX(), averageY, crossedZone.vertices[0].getZ()), new Point(crossedZone.vertices[0].getX(), averageY, crossedZone.vertices[7].getZ())};
                pointArr[4] = new Point[]{new Point(crossedZone.vertices[0].getX(), averageY, crossedZone.vertices[0].getZ()), new Point(crossedZone.vertices[7].getX(), averageY, crossedZone.vertices[0].getZ())};
                pointArr[5] = new Point[]{new Point(crossedZone.vertices[0].getX(), averageY, crossedZone.vertices[0].getZ()), new Point(crossedZone.vertices[7].getX(), averageY, crossedZone.vertices[7].getZ())};
                pointArr[6] = new Point[]{new Point(averageX, crossedZone.vertices[0].getY(), crossedZone.vertices[0].getZ()), new Point(averageX, crossedZone.vertices[0].getY(), crossedZone.vertices[7].getZ())};
                pointArr[7] = new Point[]{new Point(averageX, crossedZone.vertices[0].getY(), crossedZone.vertices[0].getZ()), new Point(averageX, crossedZone.vertices[7].getY(), crossedZone.vertices[0].getZ())};
                pointArr[8] = new Point[]{new Point(averageX, crossedZone.vertices[0].getY(), crossedZone.vertices[0].getZ()), new Point(averageX, crossedZone.vertices[7].getY(), crossedZone.vertices[7].getZ())};

                for(int i = 0; i < pointArr.length; i++) {
                    YourService.log("Checking Point[0]: " + pointArr[i][0].getX() + ", " + pointArr[i][0].getY() + ", " + pointArr[i][0].getZ());
                    YourService.log("Checking Point[1]: " + pointArr[i][1].getX() + ", " + pointArr[i][1].getY() + ", " + pointArr[i][1].getZ());
                    if(!Constants.Calculations.isOutOfBounds(pointArr[i][0]) && !Constants.Calculations.isOutOfBounds(pointArr[i][1])) {
                        YourService.log("Not OB");

                        //TODO: Use minMagnitude == currentMagnitude or something instead so there only needs to be one crosses check
                        if(!crossedZone.crosses(currentPoint, pointArr[i][0])) {
                            YourService.log("Got to first point. Adding to queue");
                            newPathPoints.clear();
                            newPathPoints.add(pointArr[i][0]);
                            newPathPoints.add(pointArr[i][1]);
                            customAdd(queue, new Path(currentPath, newPathPoints, endpoint));
                        }

                        if(!crossedZone.crosses(currentPoint, pointArr[i][1])) {
                            YourService.log("Got to second point. Adding to queue");
                            newPathPoints.clear();
                            newPathPoints.add(pointArr[i][1]);
                            newPathPoints.add(pointArr[i][0]);
                            customAdd(queue, new Path(currentPath, newPathPoints, endpoint));
                        }
                    }
                }
            }

            YourService.log("Getting new currentPath and currentPoint");
            currentPath = queue.remove(0);
            currentPoint = currentPath.points.get(currentPath.points.size() - 1);
        }

        YourService.log("Output construction");
        while(currentPath != null) {
            YourService.log("currentPath isn't null. Adding to output");
            for(int i = currentPath.points.size() - 1; i >= 0; i--) {
                YourService.log("Adding Point: " + currentPath.points.get(i).getX() + ", " + currentPath.points.get(i).getY() + ", " + currentPath.points.get(i).getZ());
                output.add(0, currentPath.points.get(i));
            }

            currentPath = currentPath.prev;
        }

        YourService.log("Returning output");
        return output;
    }

    private static void customAdd(ArrayList<Path> queue, Path element) {
        YourService.log("Custom Adding");

        if(queue.size() == 0) {
            YourService.log("Size was 0. Adding right away");
            queue.add(element);
            return;
        }

        int left = 0;
        int right = queue.size();
        int middle = (right + left + 1) / 2;

        YourService.log("Element: " + element.queueValue);

        while(!(middle == queue.size())
                && !(middle == 0 && element.compareTo(queue.get(0)))
                && !(element.compareTo(queue.get(middle)) && !(element.compareTo(queue.get(middle - 1))))) {
            YourService.log("Middle: " + queue.get(middle).queueValue);
            if(element.compareTo(queue.get(middle))) {
                right = middle;
            } else {
                left = middle;
            }

            middle = (right + left + 1) / 2;
        }

        for(int i = middle; i >= 0; i--) {
            if(element.equals(queue.get(i))) {
                return;
            }
        }

        for(int i = middle; i < queue.size(); i++) {
            if(element.equals(queue.get(i))) {
                queue.remove(i);
                i--;
            }
        }

        queue.add(middle, element);
    }
}