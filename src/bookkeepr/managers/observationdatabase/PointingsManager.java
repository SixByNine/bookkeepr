/*
 * Copyright (C) 2005-2008 Michael Keith, Australia Telescope National Facility, CSIRO
 * 
 * email: mkeith@pulsarastronomy.net
 * www  : www.pulsarastronomy.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package bookkeepr.managers.observationdatabase;

import bookkeepr.BookKeeprException;

import bookkeepr.managers.ObservationManager;
import bookkeepr.managers.TypeIdManager;
import bookkeepr.xml.IdAble;
import bookkeepr.xmlable.Configuration;
import bookkeepr.xmlable.CreatePointingsRequest;
import bookkeepr.xmlable.ExtendedPointing;
import bookkeepr.xmlable.Limits;
import bookkeepr.xmlable.Pointing;
import bookkeepr.xmlable.PointingIndex;
import bookkeepr.xmlable.PointingSelectRequest;
import bookkeepr.xmlable.Psrxml;
import bookkeepr.xmlable.Receiver;
import bookkeepr.xmlable.Session;
import bookkeepr.xmlable.Telescope;
import coordlib.Coordinate;
import coordlib.CoordinateDistanceComparitorGalactic;
import coordlib.Dec;
import coordlib.RA;
import coordlib.SkyLocated;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author kei041
 */
public class PointingsManager {

    private ObservationManager obsmanager;
    private static final double[] zoneCentres = new double[]{-87, -81, -75, -69, -63, -56.5, -49.5, -41.5, -31.5, 0, 31.5, 41.5, 49.5, 56.5, 63, 69, 75, 81, 87};
    private static final double[] zoneSizes = new double[]{6, 6, 6, 6, 6, 6.5, 7, 8.5, 10, 52, 10, 8.5, 7, 6.5, 6, 6, 6, 6, 6};

    public PointingsManager(ObservationManager obsmanager) {
        this.obsmanager = obsmanager;
    }
    /*
     * This function is REALLY inefficient.
     * 
     * Making schedules etc would be a lot faster if it wasn't so rubbish
     * 
     * @todo: Make this function faster.
     */

    public PointingIndex getPointings(PointingSelectRequest req) {

        // If they have specified an exact Id to get then just return that
        if (req.getExactId() > 0) {
            // we want exactly one result with a specific ID.
            IdAble idable = obsmanager.getDbManager().getById(req.getExactId());
            if (idable != null && idable instanceof Pointing) {
                PointingIndex idx = new PointingIndex();
                ExtendedPointing ptg = new ExtendedPointing((Pointing) idable);

                Configuration conf = (Configuration) obsmanager.getDbManager().getById(ptg.getConfigurationId());
                if (conf != null) {
                    ptg.setTobs(conf.getTobs());
                    ptg.setScheduleLine(conf.getScheduleLine((Pointing) idable));
                }
                ptg.setObservations(obsmanager.getObservations(ptg));

                idx.addItem(ptg);
                return idx;
            } else {
                return new PointingIndex();
            }
        }

        // otherwise, do a real search.

        // first filter by position if we can.
        ArrayList<Pointing> result;
        ArrayList<Pointing> interim;

        if (req.getTarget() != null) {
            result = getPointingsNear(req.getTarget(), req.getTargetSeperation());
        } else {
            result = (ArrayList<Pointing>) obsmanager.getAllPointings().clone();
        }


        // now filter by gridId.
        if (req.getGridId() != null) {
            if (req.getUseRegularExpresions()) {
                // use a regex

                Pattern pat = Pattern.compile(req.getGridId());
                interim = new ArrayList<Pointing>();
                for (Pointing ptg : result) {
                    if (pat.matcher(ptg.getGridId()).matches()) {
                        interim.add(ptg);
                        continue;
                    }
                    for (Psrxml psrxml : obsmanager.getObservations(ptg)) {
                        if (pat.matcher(psrxml.getSourceName()).matches()) {
                            interim.add(ptg);
                            continue;
                        }
                    }
                }
            } else {
                interim = new ArrayList<Pointing>();
                for (Pointing ptg : result) {
                    if (ptg.getGridId().contains(req.getGridId())) {
                        interim.add(ptg);
                    }

                    List<Psrxml> psrxmlList = obsmanager.getObservations(ptg);
                    if (psrxmlList != null) {
                        for (Psrxml psrxml : psrxmlList) {
                            if (psrxml.getSourceName().contains(req.getGridId())) {

                                interim.add(ptg);
                                continue;
                            }
                        }
                    }
                }
            }

            // update the result array
            result = interim;
            interim = null;
        }

        // now filter by configuration.
        if (req.getConfigurationId() > 0) {

            interim = new ArrayList<Pointing>();
            for (Pointing ptg : result) {
                if (ptg.getConfigurationId() == req.getConfigurationId()) {
                    interim.add(ptg);
                }
            }
            // update the result array
            result = interim;
            interim = null;
        }

        if (!Float.isNaN(req.getMaxTobs()) || !Float.isNaN(req.getMinTobs())) {

            ArrayList<Long> acceptConfigIds = new ArrayList<Long>();

            List<IdAble> configs = this.obsmanager.getDbManager().getAllOfType(TypeIdManager.getTypeFromClass(Configuration.class));
            float maxTobs = Float.MAX_VALUE;
            float minTobs = Float.MIN_VALUE;
            if (!Float.isNaN(req.getMaxTobs())) {
                maxTobs = req.getMaxTobs();
            }
            if (!Float.isNaN(req.getMinTobs())) {
                minTobs = req.getMinTobs();
            }

            for (IdAble idable : configs) {
                Configuration config = (Configuration) idable;
                if (config.getTobs() < maxTobs && config.getTobs() > minTobs) {
                    acceptConfigIds.add(config.getId());
                }
            }

            interim = new ArrayList<Pointing>();
            for (Pointing ptg : result) {
                if (acceptConfigIds.contains(ptg.getConfigurationId())) {
                    interim.add(ptg);
                }
            }
            // update the result array
            result = interim;
            interim = null;
        }

        if (!Float.isNaN(req.getVisibleAt())) {
            // we need to check if things are 'up'
            interim = new ArrayList<Pointing>();

            for (Pointing ptg : result) {
                float rise = ptg.getRise();
                float set = ptg.getSet();
                if (rise < req.getVisibleAt()) {
                    rise += 24;
                }
                if (set < req.getVisibleAt()) {
                    set += 24;
                }
                if (set < rise) {
                    // if it sets before it rises, then it's up!
                    interim.add(ptg);
                }
            }
            // update the result array
            result = interim;
            interim = null;
        }

        if (!Float.isNaN(req.getSetAt())) {
            // we need to check if things are 'up'
            interim = new ArrayList<Pointing>();

            for (Pointing ptg : result) {
                float rise = ptg.getRise();
                float set = ptg.getRise();
                if (rise < req.getSetAt()) {
                    rise += 24;
                }
                if (set < req.getSetAt()) {
                    set += 24;
                }
                if (set > rise) {
                    // if it sets after it rises, then it's set!
                    interim.add(ptg);
                }
            }
            // update the result array
            result = interim;
            interim = null;
        }

        if (!req.getSelectToObserve()) {
            interim = new ArrayList<Pointing>();
            for (Pointing ptg : result) {
                if (!ptg.getToBeObserved()) {
                    interim.add(ptg);
                }
            }
            // update the result array
            result = interim;
            interim = null;
        }

        if (!req.getSelectNotToObserve()) {
            interim = new ArrayList<Pointing>();
            for (Pointing ptg : result) {
                if (ptg.getToBeObserved()) {
                    interim.add(ptg);
                }
            }
            // update the result array
            result = interim;
            interim = null;
        }

        if (req.getIgnoreIdList() != null) {
            interim = new ArrayList<Pointing>();
            long[] list = req.getIgnoreIdList();
            Arrays.sort(list);
            for (Pointing ptg : result) {
                if (Arrays.binarySearch(list, ptg.getId()) < 0) {
                    interim.add(ptg);
                }
            }
            // update the result array
            result = interim;
            interim = null;
        }

        PointingIndex idx = new PointingIndex();
        int count = 0;
        Configuration conf = null;

        ArrayList<Pointing> e_result = new ArrayList<Pointing>();
        for (Pointing p : result) {
            count++;
            if (count > req.getMaxResults()) {
                break;
            }
            ExtendedPointing ptg = new ExtendedPointing(p);
            ptg.setObservations(obsmanager.getObservations(ptg));
            if (conf == null || conf.getId() != ptg.getConfigurationId()) {
                conf = (Configuration) obsmanager.getDbManager().getById(ptg.getConfigurationId());
            }
            ptg.setScheduleLine(conf.getScheduleLine(p));
            ptg.setTobs(conf.getTobs());
            e_result.add(ptg);
        }
idx.setList(e_result);

        return idx;
    }

    public ArrayList<Pointing> getPointingsNear(final Coordinate coord, float sepn) {

        CoordinateDistanceComparitorGalactic distComp = new CoordinateDistanceComparitorGalactic(coord.getGl(), coord.getGb()) {

            @Override
            public int compare(SkyLocated o1, SkyLocated o2) {
                if (o1 instanceof Pointing && o2 instanceof Pointing) {
                    Pointing p1 = (Pointing) o1;
                    Pointing p2 = (Pointing) o2;
                    double min1 = Double.MAX_VALUE;
                    double min2 = Double.MAX_VALUE;
                    for (Coordinate c1 : p1.getCoverage()) {
                        double d = super.difference(c1.getGl(), c1.getGb(), coord.getGl(), coord.getGb());
                        if (d < min1) {
                            min1 = d;
                        }
                    }
                    for (Coordinate c2 : p2.getCoverage()) {
                        double d = super.difference(c2.getGl(), c2.getGb(), coord.getGl(), coord.getGb());
                        if (d < min2) {
                            min2 = d;
                        }
                    }
                    return (int) (100000.0 * (min1 - min2));
                } else {
                    return super.compare(o1, o2);
                }
            }
        };
        double dist = 360;
        Coordinate quicky = null;
        if (sepn < obsmanager.quickSearchRange) {
            for (Coordinate c : obsmanager.getPointingQuickSearch().keySet()) {
                double newDist = distComp.difference(coord.getGl(), coord.getGb(), c.getGl(), c.getGb());
                if (newDist < dist) {
                    dist = newDist;
                    quicky = c;
                }
            }
        }
        ArrayList<Pointing> result = new ArrayList<Pointing>();
        ArrayList<Pointing> duplicate;

        if (quicky == null) {
            duplicate = (ArrayList<Pointing>) obsmanager.getAllPointings().clone();
        } else {
            duplicate = (ArrayList<Pointing>) obsmanager.getPointingQuickSearch().get(quicky).clone();
        }
        nextptg:
        for (Pointing ptg : duplicate) {


            if (distComp.difference(ptg.getTarget().getGl(), ptg.getTarget().getGb(), coord.getGl(), coord.getGb()) < sepn) {
                result.add(ptg);
                continue nextptg;
            }
            for (Coordinate c : ptg.getCoverage()) {
                if (distComp.difference(c.getGl(), c.getGb(), coord.getGl(), coord.getGb()) < sepn) {
                    result.add(ptg);
                    continue nextptg;
                }
            }
            List<Psrxml> psrxmlList = obsmanager.getObservations(ptg);
            if (psrxmlList != null) {
                for (Psrxml psrxml : psrxmlList) {

                    if (distComp.difference(psrxml.getStartCoordinate().getGl(), psrxml.getStartCoordinate().getGb(), coord.getGl(), coord.getGb()) < sepn) {
                        result.add(ptg);
                        continue nextptg;
                    }
                }
            }

        }

        Collections.sort(result, distComp);
        return result;
    }

    public void createNewPointings(CreatePointingsRequest request, PrintWriter messageLog) {


        // get the config...
        Configuration config = null;
        try {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Finding Configuration " + Long.toHexString(request.getConfigurationId()));
            config =
                    (Configuration) obsmanager.getDbManager().getById(request.getConfigurationId());
        } catch (ClassCastException e) {
            String msg = "Bad configuration id " + Long.toHexString(request.getConfigurationId()) + " in create pointings request";
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, msg);
            messageLog.println(msg);
            throw new RuntimeException(msg);
        }

        if (config == null) {
            String msg = "Unknown configuration id " + Long.toHexString(request.getConfigurationId()) + " in create pointings request";
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, msg);
            messageLog.println(msg);
            throw new RuntimeException(msg);
        }
        /*
         * For non-galactic orientation, switch ra for gl and dec for gb.
         * Note that the offsets etc keep their names, but are switched also.
         * 
         */
        boolean galactic = config.getGalacticOrientated();

// get the receiver...
        Receiver receiver = null;
        try {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Finding Receiver " + Long.toHexString(config.getReceiverId()));
            receiver =
                    (Receiver) obsmanager.getDbManager().getById(config.getReceiverId());
        } catch (ClassCastException e) {
            String msg = "Bad receiver id " + Long.toHexString(config.getReceiverId()) + " in config for create pointings request";
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, msg);
            messageLog.println(msg);
            throw new RuntimeException(msg);
        }

        if (config == null) {
            String msg = "Unknown receiver id " + Long.toHexString(config.getReceiverId()) + " in config for create pointings request";
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, msg);
            messageLog.println(msg);
            throw new RuntimeException(msg);
        }
// get the telescope...

        Telescope telescope = null;
        try {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Finding Telescope " + Long.toHexString(config.getTelescopeId()));
            telescope =
                    (Telescope) obsmanager.getDbManager().getById(config.getTelescopeId());
        } catch (ClassCastException e) {
            String msg = "Bad telescope id " + Long.toHexString(config.getTelescopeId()) + " in config for create pointings request";
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, msg);
            messageLog.println(msg);
            throw new RuntimeException(msg);
        }

        if (config == null) {
            String msg = "Unknown telescope id " + Long.toHexString(config.getTelescopeId()) + " in config for create pointings request";
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, msg);
            messageLog.println(msg);
            throw new RuntimeException(msg);
        }

        Limits gbLimits = request.getLimit("gb");
        Limits glLimits = request.getLimit("gl");

        Limits raLimits = request.getLimit("ra");
        Limits decLimits = request.getLimit("dec");

        if (gbLimits == null) {
            gbLimits = new Limits();
            gbLimits.setMax(90);
            gbLimits.setMin(-90);
        }

        if (glLimits == null) {
            glLimits = new Limits();
            glLimits.setMax(180);
            glLimits.setMin(-180);
        }

        if (raLimits == null) {
            raLimits = new Limits();
            raLimits.setMax(360);
            raLimits.setMin(0);
        }

        if (decLimits == null) {
            decLimits = new Limits();
            decLimits.setMax(90);
            decLimits.setMin(-90);
        }

        double zonePadding = 0.7;// zone overlap size in degrees


        int lowestBzone = 0;
        int highestBzone = zoneCentres.length - 1;

        if (galactic) {
            double maxB = zoneCentres[lowestBzone] + zoneSizes[lowestBzone] / 2;
            while (maxB < gbLimits.getMin() && lowestBzone < zoneCentres.length) {
                lowestBzone++;
                maxB = zoneCentres[lowestBzone] + zoneSizes[lowestBzone] / 2;
            }


            double minB = zoneCentres[highestBzone] - zoneSizes[highestBzone] / 2;
            while (minB > gbLimits.getMax() && highestBzone > 0) {
                highestBzone--;
                minB = zoneCentres[highestBzone] - zoneSizes[highestBzone] / 2;
            }
        } else {
            double maxB = zoneCentres[lowestBzone] + zoneSizes[lowestBzone] / 2;
            while (maxB < decLimits.getMin() && lowestBzone < zoneCentres.length) {
                lowestBzone++;
                maxB = zoneCentres[lowestBzone] + zoneSizes[lowestBzone] / 2;
            }


            double minB = zoneCentres[highestBzone] - zoneSizes[highestBzone] / 2;
            while (minB > decLimits.getMax() && highestBzone > 0) {
                highestBzone--;
                minB = zoneCentres[highestBzone] - zoneSizes[highestBzone] / 2;
            }
        }

        String msg;
        if (galactic) {
            msg = "Creating Pointings for b-zones " + lowestBzone + " -> " + highestBzone;
        } else {
            msg = "Creating Pointings for dec-zones " + lowestBzone + " -> " + highestBzone;
        }
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, msg);
        messageLog.println(msg);
        Session session = new Session();
        int count = 0;
        HashMap<String, String> myGridIds = new HashMap<String, String>();
        //TEST
//        ArrayList<Pointing> testArr = new ArrayList<Pointing>();

        // Do each zone seperately...

        for (int zone = lowestBzone; zone <= highestBzone; zone++) {
            msg = "Working on bzone " + zone;
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, msg);
            messageLog.println(msg);
            double lfactor = 1.0 / Math.cos(Math.toRadians(zoneCentres[zone]));
            //   out.printf("zone: %d (%3.1f > %3.1f, lfac: %f)\n", zone, bzoneSize * zone + (bzoneSize / 2 + 0.7), bzoneSize * zone - (bzoneSize / 2 + 0.7), lfactor);
            // we need to make a grid for each part of the pattern.
            float[] glGridPattern = receiver.getGlGridPattern();
            float[] gbGridPattern = receiver.getGbGridPattern();
            for (int i = 0; i < glGridPattern.length; i++) {

                // make a new offset grid
                Grid grid;
                if (galactic) {
                    double xmax = glLimits.getMax();
                    double xmin = glLimits.getMin() + glGridPattern[i] * lfactor; // offset the grid by the gl offset

                    double ymax = zoneCentres[zone] + zoneSizes[zone] / 2 + zonePadding;
                    double ymin = zoneCentres[zone] - zoneSizes[zone] / 2 - zonePadding + gbGridPattern[i]; // offset the grid by the gb offset

                    grid = new Grid(xmax, xmin, ymax, ymin);
                } else {
                    double xmax = raLimits.getMax();
                    double xmin = raLimits.getMin() + glGridPattern[i] * lfactor; // offset the grid by the ra offset

                    double ymax = zoneCentres[zone] + zoneSizes[zone] / 2 + zonePadding;
                    double ymin = zoneCentres[zone] - zoneSizes[zone] / 2 - zonePadding + gbGridPattern[i]; // offset the grid by the dec offset

                    grid = new Grid(xmax, xmin, ymax, ymin);
                }
                grid.setXstep(receiver.getGlGridOffset()[0] * lfactor);
                grid.setXoff(receiver.getGbGridOffset()[0] * lfactor);
                grid.setYstep(receiver.getGbGridOffset()[1]);

                ArrayList<Point> points = grid.getGrid();
                for (Point p : points) {
                    if (p.getY() > 90 || p.getY() < -90) {
                        continue;
                    }

                    Coordinate coord;
                    if (galactic) {
                        coord = new Coordinate(p.getX(), p.getY());
                    } else {
                        coord = new Coordinate(new RA(p.getX()), new Dec(p.getY()));
                    }
                    if (!(coord.getRA().toDegrees() > raLimits.getMax() || coord.getRA().toDegrees() < raLimits.getMin() ||
                            coord.getDec().toDegrees() > decLimits.getMax() || coord.getDec().toDegrees() < decLimits.getMin() ||
                            coord.getGl() > glLimits.getMax() || coord.getGl() < glLimits.getMin() ||
                            coord.getGb() > gbLimits.getMax() || coord.getGb() < gbLimits.getMin())) {
                        float[] riseSet = this.getRiseAndSet(coord, telescope);
                        StringBuffer gridId;
                        double gl = coord.getGl();
                        if (gl < 0) {
                            gl += 360;
                        }

                        gridId = new StringBuffer();
                        Formatter gridIdFormatter = new Formatter(gridId);
                        gridIdFormatter.format(Locale.UK, "G%05.1f%+05.1f", gl, coord.getGb());
                        if (this.obsmanager.getPointingFromGridId(gridId.toString()) != null || myGridIds.containsKey(gridId.toString())) {
                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Grid ID " + gridId + " already exists in the database, skipping...");
                            continue;
                        }
                        myGridIds.put(gridId.toString(), gridId.toString());

                        Pointing ptg = new Pointing();
                        ptg.setTarget(coord);
                        ptg.setRise(riseSet[0]);
                        ptg.setSet(riseSet[1]);
                        ptg.setGridId(gridId.toString());
                        ptg.setConfigurationId(config.getId());
//                        System.out.println(gridId.toString()+" "+coord.toString(false));
                        for (int beam = 0; beam < receiver.getGlBeamPattern().length; beam++) {
                            Coordinate ccc;
                            if (galactic) {
                                ccc = coord.offsetGlGb(receiver.getGlBeamPattern()[beam], receiver.getGbBeamPattern()[beam]);
                            } else {
                                ccc = coord.offsetRaDec(receiver.getGlBeamPattern()[beam], receiver.getGbBeamPattern()[beam]);
                            }
                            ptg.addToCoverage(ccc);
                        }
                        coord.cleanRA();

//                        testArr.add(ptg);
                        obsmanager.getDbManager().add(ptg, session);
                        count++;

                    } else {
                        // outside the range...
                    }
                }
            }
        }
        try {

            obsmanager.getDbManager().save(session);
        } catch (BookKeeprException ex) {
            Logger.getLogger(PointingsManager.class.getName()).log(Level.WARNING, "Could not add pointings to database!", ex);
        }

        msg = "Created " + count + " new pointings";
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Created " + count + " new pointings");


//        try {
//        PrintStream testOut = new PrintStream("test.out");
//        for(Pointing p : testArr){
//            testOut.printf("%f\t%f\n",p.getCoordinate().getGl(),p.getCoordinate().getGb());
//        }
//        
//            ImageIO.write(obsmanager.makeImage(testArr), "PNG", new File("made.png"));
//        } catch (IOException ex) {
//            Logger.getLogger(PointingsManager.class.getName()).log(Level.SEVERE, null, ex);
//        }




    }

    public float[] getRiseAndSet(Coordinate coord, Telescope tel) {

        double zenith = tel.getZenithLimit();
        double lat = tel.getLattitude();
        double dec = coord.getDec().toDegrees();
        double ra = coord.getRA().toDegrees();

        double v = (Math.sin(Math.toRadians(zenith) - Math.sin(Math.toRadians(lat)) * Math.sin(Math.toRadians(dec)))) / (Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(dec)));

        double off = Math.toDegrees(Math.acos(v));

        double rise = (ra - off) / 15;
        if (rise < 0) {
            rise += 24;
        }

        double set = (ra + off) / 15;
        if (set > 24) {
            set -= 24;
        }

        return new float[]{(float) rise, (float) set};


    }

    private class Grid {

        /* for each row 'X'
         * there are points from xmin->xmax, seperated by xstep
         *
         * 
         * each row is seperated by ystep
         * there are rows from ymin->ymax
         * 
         * each row is offset by xoff
         */
        private double xstep;
        private double ystep;
        private double xoff;
        private double xmax;
        private double xmin;
        private double ymax;
        private double ymin;

        public Grid(double xmax, double xmin,
                double ymax, double ymin) {
            this.xmax = xmax;
            this.xmin = xmin;
            this.ymax = ymax;
            this.ymin = ymin;
        }

        public void setXmax(double xmax) {
            this.xmax = xmax;
        }

        public void setXmin(double xmin) {
            this.xmin = xmin;
        }

        public void setXoff(double xoff) {
            this.xoff = xoff;
        }

        public void setXstep(double xstep) {
            this.xstep = xstep;
        }

        public void setYmax(double ymax) {
            this.ymax = ymax;
        }

        public void setYmin(double ymin) {
            this.ymin = ymin;
        }

        public void setYstep(double ystep) {
            this.ystep = ystep;
        }

        private ArrayList<Point> getGrid() {
            ArrayList<Point> list = new ArrayList<Point>();
            double thisXmin = xmin;
            for (double y = ymin; y < ymax; y += ystep) {
                thisXmin += xoff;
                while (thisXmin - xstep > xmin) {
                    thisXmin -= xstep;
                }
                for (double x = thisXmin; x < xmax; x += xstep) {
                    list.add(new Point(x, y));
                }
            }
            return list;
        }
    }

    private class Point {

        double x,   y;

        public Point(
                double x, double y) {
            this.x = x;

            this.y = y;



        }

        public double getX() {


            return x;
        }

        public double getY() {
            return y;
        }
    }
}
