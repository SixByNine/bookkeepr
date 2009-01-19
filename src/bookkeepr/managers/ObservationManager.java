package bookkeepr.managers;

import bookkeepr.BookKeeprException;

import bookkeepr.managers.observationdatabase.PointingsManager;
import bookkeepr.managers.observationdatabase.PsrXMLManager;
import bookkeepr.managers.observationdatabase.PsrXMLManager.PointingNotFoundException;
import bookkeepr.managers.observationdatabase.ScheduleManager;
import bookkeepr.managers.observationdatabase.SkyViewManager;
import bookkeepr.xml.IdAble;
import bookkeepr.xmlable.Backend;
import bookkeepr.xmlable.CreatePointingsRequest;
import bookkeepr.xmlable.CreateScheduleRequest;
import bookkeepr.xmlable.DatabaseManager;
import bookkeepr.xmlable.Observation;
import bookkeepr.xmlable.ObservationStatus;
import bookkeepr.xmlable.Pointing;
import bookkeepr.xmlable.PointingIndex;
import bookkeepr.xmlable.PointingSelectRequest;
import bookkeepr.xmlable.Psrxml;
import bookkeepr.xmlable.Receiver;
import bookkeepr.xmlable.Session;
import bookkeepr.xmlable.Telescope;
import coordlib.Coordinate;
import coordlib.CoordinateDistanceComparitorGalactic;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mike Keith
 */
public class ObservationManager implements ChangeListener {

    private HashMap<Coordinate, ArrayList<Pointing>> pointingQuickSearch = new HashMap<Coordinate, ArrayList<Pointing>>();
    private ArrayList<Pointing> allPointings = new ArrayList<Pointing>();
    private HashMap<Coordinate, ArrayList<Psrxml>> observationQuickSearch = new HashMap<Coordinate, ArrayList<Psrxml>>();
    private HashMap<String, Pointing> gidToPointing = new HashMap<String, Pointing>();
    private ArrayList<Psrxml> allObservations = new ArrayList<Psrxml>();
    private HashMap<Long, ArrayList<Long>> pointingObservations = new HashMap<Long, ArrayList<Long>>();
    private ArrayList<Long> orphanedObservations = new ArrayList<Long>();
    public static final int quickSearchRange = 20;
    private HashMap<Telescope, Telescope> telescopes = new HashMap<Telescope, Telescope>();
    private HashMap<Receiver, Receiver> receivers = new HashMap<Receiver, Receiver>();
    private HashMap<Backend, Backend> backends = new HashMap<Backend, Backend>();
    private HashMap<String, Psrxml> psrxmlKeys = new HashMap<String, Psrxml>();
    private DatabaseManager dbManager;
    private ScheduleManager scheduleManager;
    private PointingsManager pointingsManager;
    private PsrXMLManager psrxmlManager;
    private SkyViewManager skyViewManager;
    private Comparator<IdAble> idAbleComparator = new Comparator<IdAble>() {

        public int compare(IdAble o1, IdAble o2) {
            return ((Long) o2.getId()).compareTo(o1.getId());
        }
    };

    public ObservationManager(DatabaseManager dbManager) {
        scheduleManager = new ScheduleManager(this);
        pointingsManager = new PointingsManager(this);
        psrxmlManager = new PsrXMLManager(this);
        skyViewManager = new SkyViewManager(this);
        this.dbManager = dbManager;

        dbManager.addListener(TypeIdManager.getTypeFromClass(Observation.class), this);
        dbManager.addListener(TypeIdManager.getTypeFromClass(Pointing.class), this);
        dbManager.addListener(TypeIdManager.getTypeFromClass(Psrxml.class), this);

        dbManager.addListener(TypeIdManager.getTypeFromClass(Telescope.class), this);

        dbManager.addListener(TypeIdManager.getTypeFromClass(Receiver.class), this);

        dbManager.addListener(TypeIdManager.getTypeFromClass(Backend.class), this);

        for (double b = -89; b <= 89; b += quickSearchRange / 1.5) {
            for (double l = -180; l <= 180; l += quickSearchRange / 1.5 / Math.cos(Math.toRadians(b))) {
                this.pointingQuickSearch.put(new Coordinate(l, b), new ArrayList<Pointing>());
                this.observationQuickSearch.put(new Coordinate(l, b), new ArrayList<Psrxml>());
            }
        }

    }

    public ObservationStatus getStatus() {
        ObservationStatus ret = new ObservationStatus();
        return ret;
    }

    /*
     * I have hidden the contents of these functions in sub objects to make the
     * code more manageable.
     * 
     * MJK 2008 
     * 
     */
    public ArrayList<String> createSchedule(CreateScheduleRequest request) {
        return this.scheduleManager.createSchedule(request);
    }

    public BufferedImage makeImage(ArrayList<Pointing> ptg) {
        return this.skyViewManager.makeImage(ptg, 600, 800);
    }

    public BufferedImage makeImage(int height, int width) {
        return this.skyViewManager.makeImage(this.allPointings, height, width);
    }

    public BufferedImage makeImage(int height, int width, int xmult, int ymult, int xoff, int yoff) {
        return this.skyViewManager.makeImage(allPointings, height, width, xmult, ymult, xoff, yoff);
    }

    public BufferedImage makeImage(double[] par, int height, int width, int xmult, int ymult, int xoff, int yoff) {
        ArrayList<Pointing> ptg = null;
        if ((float) (par[2]) < 20) {
            ptg = this.getPointingsNear(new Coordinate(par[0], par[1]), (float) (par[2]));
        } else {
            ptg = allPointings;
        }
        return this.skyViewManager.makeImage(ptg, height, width, xmult, ymult, xoff, yoff);
    }

    public double[] getParamsOfImgSquare(int height, int width, int xmult, int ymult, int xoff, int yoff) {
        return skyViewManager.getParamsOfImgSquare(height, width, xmult, ymult, xoff, yoff);
    }

    public PointingIndex getPointings(PointingSelectRequest req) {
        return this.pointingsManager.getPointings(req);

    }

    public ArrayList<Pointing> getPointingsNear(Coordinate coord, float sepn) {
        return this.pointingsManager.getPointingsNear(coord, sepn);
    }

    public void createNewPointings(CreatePointingsRequest request, PrintWriter messageLog) {
        this.pointingsManager.createNewPointings(request, messageLog);
    }

    public ArrayList<Psrxml> getAllObservations() {
        return allObservations;
    }

    public ArrayList<Pointing> getAllPointings() {
        return allPointings;
    }

    public DatabaseManager getDbManager() {
        return dbManager;
    }

    public HashMap<Coordinate, ArrayList<Psrxml>> getObservationQuickSearch() {
        return observationQuickSearch;
    }

    public ArrayList<Long> getOrphanedObservations() {
        return orphanedObservations;
    }

//    public HashMap<Long, ArrayList<Long>> getPointingObservations() {
//        return pointingObservations;
//    }
//    
    public ArrayList<Psrxml> getObservations(Pointing p) {
        ArrayList<Psrxml> ret = new ArrayList<Psrxml>();
        List<Long> ids = pointingObservations.get(p.getId());
        if (ids == null) {
            return null;
        }
        for (long id : ids) {
            ret.add((Psrxml) dbManager.getById(id));
        }
        return ret;
    }

    public HashMap<Coordinate, ArrayList<Pointing>> getPointingQuickSearch() {
        return pointingQuickSearch;
    }

    public void addPsrXML(Psrxml header) throws PointingNotFoundException, BookKeeprException {
        psrxmlManager.addPsrXML(header);
    }
    
    
    public Psrxml queryPsrXML(Psrxml header) throws PointingNotFoundException, BookKeeprException {
        return psrxmlManager.queryPsrXML(header);
    }

    public void addPsrXML(Psrxml header, Session session) throws PointingNotFoundException, BookKeeprException {
        psrxmlManager.addPsrXML(header, session);
    }

    public Pointing getPointingFromGridId(String gridId) {
        return this.gidToPointing.get(gridId);
    }

    public Psrxml getPsrxmlFromUtcBeam(String utc, int beam) {
        return this.psrxmlKeys.get(utc + beam);
    }

    public void itemUpdated(DatabaseManager dbMan, IdAble item, boolean remoteChange, boolean modified) {


        if (item instanceof Pointing) {
            synchronized (allPointings) {
                Pointing ptg = (Pointing) item;



                if (modified) {
                    int posn = Collections.binarySearch(allPointings, item, idAbleComparator);
                    if (posn >= 0) {
                        allPointings.remove(posn);
                    } else {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Psrxml claims to be modified, but we have not heard of it yet...");
                    }
                    for (Coordinate c : pointingQuickSearch.keySet()) {
                        pointingQuickSearch.get(c).remove(item);
                    }
                    this.gidToPointing.remove(ptg.getGridId());
                }

                // Keep the observations array sorted by ID for faster indexing.
                int posn = Collections.binarySearch(allPointings, item, idAbleComparator);
                if (posn < 0) {
                    posn = -(posn + 1);
                } else {
                    // something bad has happened!
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Psrxml added to database but Id already exists!");
                    return;
                }
                this.allPointings.add(posn, ptg);
                this.gidToPointing.put(ptg.getGridId(), ptg);

                // currently we don't update the quicky. assume it doesn't move
                CoordinateDistanceComparitorGalactic distComp = new CoordinateDistanceComparitorGalactic(0, 0);
                for (Coordinate c : pointingQuickSearch.keySet()) {
                    if (Math.abs((ptg.getTarget().getGb() - c.getGb())) < quickSearchRange) {
                        double dist = distComp.difference((float) c.getGl(), (float) c.getGb(), ptg.getTarget().getGl(), ptg.getTarget().getGb());

                        if (dist < quickSearchRange) {
                            pointingQuickSearch.get(c).add(ptg);
                        }
                    }
                }
                if (!modified) {

                    // ID can't change so we are not going to match it a second 
                    for (long psrXmlId : (List<Long>) orphanedObservations.clone()) {
                        Psrxml psrxml = (Psrxml) dbMan.getById(psrXmlId);
                        if (psrxml.getPointingId() == ptg.getId()) {
                            // we have found the parent of this orphan! Yay :D
                            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Parent found for oprhaned observation " + Long.toHexString(psrXmlId));

                            ArrayList<Long> lst = this.pointingObservations.get(ptg.getId());
                            if (lst == null) {
                                lst = new ArrayList<Long>();
                                pointingObservations.put(ptg.getId(), lst);
                            }
                            lst.add(psrXmlId);
                            orphanedObservations.remove(psrXmlId);
                        }
                    }
                }
            }
        }

        if (item instanceof Psrxml) {
            synchronized (allObservations) {
                Psrxml psrxml = (Psrxml) item;
                if (modified) {
                    int posn = Collections.binarySearch(allObservations, item, idAbleComparator);
                    if (posn >= 0) {
                        allObservations.remove(posn);
                    } else {
                        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Psrxml claims to be modified, but we have not heard of it yet...");
                    }
                }

                String xkey = psrxml.getUtc() + psrxml.getReceiverBeam();
                if (this.psrxmlKeys.containsKey(xkey)) {
                    this.psrxmlKeys.remove(xkey);

                }
                this.psrxmlKeys.put(xkey, psrxml);
                // Keep the observations array sorted by ID for faster indexing.
                int posn = Collections.binarySearch(allObservations, item, idAbleComparator);
                if (posn < 0) {
                    posn = -(posn + 1);
                } else {
                    // something bad has happened!
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Psrxml added to database but Id already exists!");
                    return;
                }
                this.allObservations.add(posn, psrxml);


                Telescope tel = this.telescopes.get(psrxml.getTelescope());

                if (tel == null) {
                    tel = psrxml.getTelescope();
                    this.telescopes.put(tel, tel);
                } else {
                    psrxml.setTelescope(tel);
                }

                Receiver rx = this.receivers.get(psrxml.getReceiver());
                if (rx == null) {
                    rx = psrxml.getReceiver();
                    this.receivers.put(rx, rx);
                } else {
                    psrxml.setReceiver(rx);
                }
                Backend backend = this.backends.get(psrxml.getBackend());
                if (backend == null) {
                    backend = psrxml.getBackend();
                    this.backends.put(backend, backend);
                } else {
                    psrxml.setBackend(backend);
                }


                if (!modified) {
                    // we assume these values are never modified.
                    Pointing ptg = (Pointing) dbMan.getById(psrxml.getPointingId());
                    if (ptg == null) {
                        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Orphaned observation detected: " + Long.toHexString(psrxml.getId()) + " Supposed to have pointing " + Long.toHexString(psrxml.getPointingId()));
                        this.orphanedObservations.add(psrxml.getId());
                    } else {
                        ArrayList<Long> lst = this.pointingObservations.get(psrxml.getPointingId());
                        if (lst == null) {
                            lst = new ArrayList<Long>();
                            pointingObservations.put(psrxml.getPointingId(), lst);
                        }
                        lst.add(psrxml.getId());
                    }


                    CoordinateDistanceComparitorGalactic distComp = new CoordinateDistanceComparitorGalactic(0, 0);
                    for (Coordinate c : observationQuickSearch.keySet()) {
                        if (Math.abs((psrxml.getStartCoordinate().getGb() - c.getGb())) < quickSearchRange) {
                            double dist = distComp.difference((float) c.getGl(), (float) c.getGb(), psrxml.getStartCoordinate().getGl(), psrxml.getStartCoordinate().getGb());
                            if (dist < quickSearchRange) {
                                observationQuickSearch.get(c).add(psrxml);
                            }
                        }
                    }
                }
            }
        }
    }
}
