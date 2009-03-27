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
package bookkeepr.managers;

import bookkeepr.BackgroundTaskRunner;
import bookkeepr.BookKeeprException;
import bookkeepr.xml.IdAble;
import bookkeepr.xml.XMLAble;
import bookkeepr.xml.XMLReader;
import bookkeepr.xmlable.BackgroundedTask;
import bookkeepr.xmlable.CandidateList;
import bookkeepr.xmlable.CandidateListStub;
import bookkeepr.xmlable.ClassifiedCandidate;
import bookkeepr.xmlable.DatabaseManager;
import bookkeepr.xmlable.RawCandidateBasic;
import bookkeepr.xmlable.RawCandidateMatched;
import bookkeepr.xmlable.Session;
import coordlib.CoordinateDistanceComparitor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.xml.sax.SAXException;
import pulsarhunter.jreaper.HarmonicType;

/**
 *
 * @author kei041
 */
public class CandidateMatchingManager implements ChangeListener {

    private DatabaseManager dbMan;
    private CandidateManager candMan;
    private BackgroundTaskRunner bgRunner;

    public CandidateMatchingManager(DatabaseManager dbMan, CandidateManager candMan, BackgroundTaskRunner bgRunner) {
        this.dbMan = dbMan;
        this.candMan = candMan;
        this.bgRunner = bgRunner;
        this.dbMan.addListener(TypeIdManager.getTypeFromClass(CandidateListStub.class), this);
        this.dbMan.addListener(TypeIdManager.getTypeFromClass(ClassifiedCandidate.class), this);
    }

    public void itemUpdated(DatabaseManager dbMan, IdAble item, boolean remoteChange, boolean isModified) {
        double distMax = 4;
        if (!remoteChange) {
            if (item instanceof ClassifiedCandidate) {
                List<IdAble> list = dbMan.getAllOfType(TypeIdManager.getTypeFromClass(CandidateListStub.class));
                final ArrayList<CandidateListStub> cls = new ArrayList<CandidateListStub>();
                final List<ClassifiedCandidate> ccands = Collections.singletonList((ClassifiedCandidate) item);
                CoordinateDistanceComparitor comp = new CoordinateDistanceComparitor();
             
                for (IdAble idable : list) {
                    if (dbMan.getOrigin(idable) == dbMan.getOriginId()) {
                        CandidateListStub stub = (CandidateListStub) idable;
                        double sepn=comp.difference(stub.getCoordinate(), ((ClassifiedCandidate) item).getCoordinate());
                        if (sepn < distMax) {
                            cls.add(stub);
                        }
                    }
                }
                Runnable task = new Runnable() {

                    public void run() {
                        matchCandidates(cls, ccands);
                    }
                };
                BackgroundedTask bgtask = new BackgroundedTask("Match Harmonics of " + ((ClassifiedCandidate) item).getName());
                bgtask.setTarget(task);

                bgRunner.offer(bgtask);
            } else if (item instanceof CandidateListStub) {
                //@todo: Implement this!
            }
        }
    }

    private void matchCandidates(List<CandidateListStub> cLists, List<ClassifiedCandidate> classedCands) {
        ArrayList<ClassifiedCandidate> changed = new ArrayList<ClassifiedCandidate>();
        for (CandidateListStub clStub : cLists) {
            File clFile = candMan.getCandidateListFile(clStub);
            XMLAble xmlable = null;
            try {
                xmlable = XMLReader.read(new GZIPInputStream(new FileInputStream(clFile)));
            } catch (SAXException ex) {
                Logger.getLogger(CandidateMatchingManager.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                continue;
            } catch (IOException ex) {
                Logger.getLogger(CandidateMatchingManager.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                continue;
            }
            if (!(xmlable instanceof CandidateList)) {
                Logger.getLogger(CandidateMatchingManager.class.getName()).log(Level.WARNING, "Candlist " + Long.toHexString(clStub.getId()) + " Does not exist in correct file " + clFile.toString());
                continue;
            }

            CandidateList clist = (CandidateList) xmlable;

            for (RawCandidateBasic basic : clist.getRawCandidateBasicList()) {
                for (ClassifiedCandidate cand : classedCands) {

                    HarmonicType ht = this.match(basic, cand);
                    if (ht != HarmonicType.None) {
                        try {
                            if (cand.getRawCandidateMatched(basic.getId())!=null) {
                                // already have this match!
                                continue;
                            }

                            ClassifiedCandidate clone = (ClassifiedCandidate) cand.clone();
                            RawCandidateMatched matched = new RawCandidateMatched(ht, basic);
                            clone.addRawCandidateMatched(matched);
                            changed.add(clone);
                        } catch (CloneNotSupportedException ex) {
                            Logger.getLogger(CandidateMatchingManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
        if (changed.size() > 0) {
            Session session = new Session();
            for (ClassifiedCandidate cand : changed) {
                dbMan.add(cand, session);
            }
            try {
                dbMan.save(session);
            } catch (BookKeeprException ex) {
                Logger.getLogger(CandidateMatchingManager.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

    }

    private HarmonicType match(RawCandidateBasic basic, ClassifiedCandidate cand) {
        double period1 = cand.getPreferedCandidate().getBaryPeriod();
        double period2 = basic.getBaryPeriod();
        double eta = 0.001;
        if (Math.abs(period1 - period2) < eta * period2) {
            // A fundimental Match
            return HarmonicType.Principal;
        }

        for (int intFactor = 2; intFactor < 16; intFactor++) {
            if (Math.abs(period1 - period2 * intFactor) < eta * period2 * intFactor) {
                // A harm matcth
                return HarmonicType.Integer;
            }
        }


        for (int bottomfactor = 1; bottomfactor <= 16; bottomfactor++) {
            for (int topfactor = 1; topfactor < 16; topfactor++) {
                double factor = ((double) topfactor) / ((double) bottomfactor);
                if (Math.abs(period1 - period2 * factor) < eta * period2 * factor) {

                    if (topfactor == 1) {
                        return HarmonicType.SimpleNonInteger;
                    } else {
                        return HarmonicType.ComplexNonInteger;
                    }
                }
            }
        }

        return HarmonicType.None;
    }
}
