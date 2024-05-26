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
        log("Finished starting mission. Moving into big KIZ");
        myApi.moveTo(new Point(10.7d, -9.8d, 4.5d), MoveMaster.myKinematics.getOrientation(), true);
        log("Finished moving into big KIZ. Constructing path");
        ArrayList<Point> path = MoveMaster.moveTo(new float[]{10.7f, -9.8f, 4.5f}, new float[]{10.5f, -8f, 5f});
        log("Finished constructing path");

        for(Point point: path) {
            log("Moving to point in the path");
            myApi.moveTo(point, MoveMaster.myKinematics.getOrientation(), true);
            log("Finished moving to point in the path");
        }
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

