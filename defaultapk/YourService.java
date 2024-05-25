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
    public static KiboRpcApi myApi;

    @Override
    protected void runPlan1(){
        myLogger.log(Level.INFO, "YS rp1 1. runPlan1 Begin ./. Assign YourService.myApi and create MoveMaster.myKinematics");
        myApi = api;
        MoveMaster.myKinematics = myApi.getRobotKinematics();

        myLogger.log(Level.INFO, "YS rp1 2. Starting Mission");
        myApi.startMission();
        myLogger.log(Level.INFO, "YS rp1 3. Creating a path to test movement");
        ArrayList<Point> path = MoveMaster.moveTo(new float[]{10.5f, -8f, 5f});

        myLogger.log(Level.INFO, "YS rp1 4. Following path point by point");
        for(Point point: path) {
            myApi.moveTo(point, MoveMaster.myKinematics.getOrientation(), true);
        }

        myLogger.log(Level.INFO, "YS rp1 5. Finished");
    }

    @Override
    protected void runPlan2(){
        // write your plan 2 here
    }

    @Override
    protected void runPlan3(){
        // write your plan 3 here
    }
}

