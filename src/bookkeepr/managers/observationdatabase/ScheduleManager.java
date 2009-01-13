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

import bookkeepr.managers.ObservationManager;
import bookkeepr.xmlable.Configuration;
import bookkeepr.xmlable.CreateScheduleRequest;
import bookkeepr.xmlable.Pointing;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author kei041
 */
public class ScheduleManager {
    
   private ObservationManager obsmanager;

    public ScheduleManager(ObservationManager obsmanager) {
        this.obsmanager = obsmanager;
    }
    
    
 public ArrayList<String> createSchedule(CreateScheduleRequest request) {


        // get the config...
        Configuration config = null;
        try {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Finding Configuration " + Long.toHexString(request.getConfigurationId()));
            config = (Configuration) obsmanager.getDbManager().getById(request.getConfigurationId());
        } catch (ClassCastException e) {
            String msg = "Bad configuration id " + Long.toHexString(request.getConfigurationId()) + " in create pointings request";
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, msg);
            throw new RuntimeException(msg);
        }
        if (config == null) {
            String msg = "Unknown configuration id " + Long.toHexString(request.getConfigurationId()) + " in create pointings request";
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, msg);
            throw new RuntimeException(msg);
        }


        float currentLST = request.getStartLst();
        ArrayList<String> result = new ArrayList<String>();
        int nobs = (int) (24.0 * 3600.0 / config.getTobs());

        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Making schedule for " + nobs + " observations in " + request.getTotalTime() + " hours");


        ArrayList<Pointing> selectFrom = new ArrayList<Pointing>();

        for (Pointing p : obsmanager.getAllPointings()) {
            double dec = p.getCoordinate().getDec().toDegrees();
            if (p.getToBeObserved() && p.getConfigurationId() == request.getConfigurationId() && dec < request.getMaxDec() && dec > request.getMinDec()) {
                selectFrom.add(p);
            }
        }
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Have " + selectFrom.size() + " Pointings to choose from");

        // we now have all the pointings to observe...

        // now randomise

        Collections.shuffle(selectFrom);
        Collections.shuffle(selectFrom);
        Collections.shuffle(selectFrom);
        Collections.shuffle(selectFrom);
        Collections.shuffle(selectFrom);
        Collections.shuffle(selectFrom);
        Collections.shuffle(selectFrom);

        int nselect = nobs * 4;
        if (selectFrom.size() < nselect) {

            nselect = selectFrom.size();
        }

        Stack<Pointing> targets = new Stack<Pointing>();

        for (int i = 0; i < nselect; i++) {
            Pointing p = selectFrom.get(i);
            System.out.println(p.getCoordinate() + " " + p.getRise() + " " + p.getSet());
            targets.push(selectFrom.get(i));
        }
        System.out.println("\n----\n\n");
        selectFrom = null;


        {
            final double finalLst = currentLST;
            final double finalOffset = request.getZenithOffset();

            // now sort the stack so that the best target will be next.
            Collections.sort(targets, new Comparator<Pointing>() {

                public int compare(Pointing o1, Pointing o2) {
                    double ra1 = o1.getCoordinate().getRA().toDegrees() - (finalLst - finalOffset) * 15;
                    double ra2 = o2.getCoordinate().getRA().toDegrees() - (finalLst - finalOffset) * 15;


                    if (ra1 > 180) {
                        ra1 -= 360;
                    }
                    if (ra2 > 180) {
                        ra2 -= 360;
                    }

                    if (ra1 * ra2 > 0) {
                        return (int) (1000 * (ra2 - ra1));
                    } else {
                        return (int) (1000 * (ra1 - ra2));
                    }
                }
            });


        }

        Stack<Pointing> skipped = new Stack<Pointing>();

        int count = 0;
        double time = 0;
        int skip = 0;
        double totalObs = 0;
        double totalWaste = 0;
        double totalDrive = 0;
        double waste = 0;
        int oldhour = -1000;
        //  System.out.println("-----");

        while (targets.size() > 0) {

            if ((int) currentLST != oldhour) {
                oldhour = (int) currentLST;
                result.add("# LST: " + oldhour + ":00");
            }

            Pointing target = targets.pop();
//            System.out.println(target.getCentreBeam().getCoord());
            if (time > request.getTotalTime()) {
                if (waste > 0) {

                    totalWaste += waste;
                    result.add("# Waste " + waste + " hours");
                }
                break;
            }

            if (skip > 0) {
                skipped.push(target);
                skip--;
                continue;
            }

            double set = target.getSet();
            double rise = target.getRise();

            if (set < currentLST) {
                set += 24;
            }
            if (rise < currentLST) {
                rise += 24;
            }
            boolean visible = rise > set;

            double timesincerise = currentLST - (rise - 24);
            double timetoset = set - currentLST;

            if (visible) {
                if (waste > 0) {

                    totalWaste += waste;
                    result.add("# Waste " + waste + " hours");
                    waste = 0;
                }

                /*
                 * Drive time?
                 */

                currentLST += config.getTobs() / 3600.0;
                time += config.getTobs() / 3600.0;

                totalObs += config.getTobs() / 3600.0;

                count++;

                if (request.getNormalise()) {
                    skip = (int) (3 * (24 / request.getTotalTime()));
                } else {
                    skip = 3;
                }




                if (timetoset < 1) {
                    skip -= 2;
                }
                if (timesincerise < 1) {
                    skip += 2;
                }
                if (skip < 0) {
                    skip = 0;
                }



                String str = config.getScheduleLine(target);

                result.add(str);

            } else {
                System.out.printf("Source set: %s    %f %f %f\n", target.getCoordinate().toString(), target.getRise(), currentLST, target.getSet());
                // we have gone too far!
                {
                    final double finalLst = currentLST;
                    final double finalOffset = request.getZenithOffset();
                    Collections.sort(skipped, new Comparator<Pointing>() {

                        public int compare(Pointing o1, Pointing o2) {
                            double ra1 = o1.getCoordinate().getRA().toDegrees() - (finalLst - finalOffset) * 15;
                            double ra2 = o2.getCoordinate().getRA().toDegrees() - (finalLst - finalOffset) * 15;


                            if (ra1 > 180) {
                                ra1 -= 360;
                            }
                            if (ra2 > 180) {
                                ra2 -= 360;
                            }

                            if (Math.abs(ra2) < Math.abs(ra1)) {
                                return -1;
                            } else {
                                return 1;
                            }
                        }
                    });
                }

                if (!skipped.empty()) {
                    Pointing newtarget = skipped.pop();
                    targets.push(newtarget);

                    // put target on the back of the stack
                    Stack<Pointing> reverse = new Stack<Pointing>();
                    while (!targets.empty()) {
                        reverse.push(targets.pop());
                    }
                    targets.push(target);
                    while (!reverse.empty()) {
                        targets.push(reverse.pop());
                    }

                } else {
                    currentLST += 0.05;
                    time += 0.05;
                    waste += 0.05;

                    targets.push(target);
                    final double finalLst = currentLST;
                    final double finalOffset = request.getZenithOffset();

                    // now sort the stack so that the best target will be next.
                    Collections.sort(targets, new Comparator<Pointing>() {

                        public int compare(Pointing o1, Pointing o2) {
                            double ra1 = o1.getCoordinate().getRA().toDegrees() - (finalLst - finalOffset) * 15;
                            double ra2 = o2.getCoordinate().getRA().toDegrees() - (finalLst - finalOffset) * 15;


                            if (ra1 > 180) {
                                ra1 -= 360;
                            }
                            if (ra2 > 180) {
                                ra2 -= 360;
                            }

                            if (Math.abs(ra2) < Math.abs(ra1)) {
                                return -1;
                            } else {
                                return 1;
                            }
                        }
                    });
                }
            }

            if (currentLST > 24) {
                currentLST -= 24;
            }
        }


        return result;
    }

}
