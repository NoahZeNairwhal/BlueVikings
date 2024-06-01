package jp.jaxa.iss.kibo.rpc.sampleapk;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;

import gov.nasa.arc.astrobee.types.Point;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */

public class YourService extends KiboRpcService {
    public static Logger myLogger = Logger.getLogger("GlobalLogger");
    public static int loggerCounter = 1;
    public static KiboRpcApi myApi;

    @Override
    protected void runPlan1(){
        log("Starting, assigning api and getting kinematics");
        myApi = api;
        MoveMaster.myKinematics = myApi.getRobotKinematics();

        log("Starting mission");
        myApi.startMission();
        YourService.log("Creating path to point using MoveMaster");
        ArrayList<Point> path = MoveMaster.moveTo(new Point(11d, -9d, 5d));

        for(Point point: path) {
            YourService.log("Moving to Point: " + point.getX() + ", " + point.getY() + ", " + point.getZ());
            myApi.moveTo(point, MoveMaster.myKinematics.getOrientation(), true);
        }

        YourService.log("All done!");
    }

    @Override
    protected void runPlan2(){
        // write your plan 2 here
    }

    @Override
    protected void runPlan3(){
        // write your plan 3 here
    }

    public static void log(String message) {
        myLogger.log(Level.INFO, loggerCounter++ + ". " + message);
    }
}

