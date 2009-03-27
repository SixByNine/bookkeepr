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
import bookkeepr.xml.StringConvertable;
import bookkeepr.xml.XMLWriter;
import bookkeepr.xml.display.GenerateRawCandidatePlot;
import bookkeepr.xml.display.RasterImageCandidatePlot;
import bookkeepr.xmlable.CandListSelectionRequest;
import bookkeepr.xmlable.CandidateList;
import bookkeepr.xmlable.CandidateListIndex;
import bookkeepr.xmlable.CandidateListStub;
import bookkeepr.xmlable.ClassifiedCandidate;
import bookkeepr.xmlable.ClassifiedCandidateIndex;
import bookkeepr.xmlable.ClassifiedCandidateSelectRequest;
import bookkeepr.xmlable.DatabaseManager;
import bookkeepr.xmlable.RawCandidate;
import bookkeepr.xmlable.RawCandidateBasic;
import bookkeepr.xmlable.RawCandidateSection;
import bookkeepr.xmlable.RawCandidateStub;
import bookkeepr.xmlable.Session;
import coordlib.Coordinate;
import coordlib.CoordinateDistanceComparitor;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import pulsarhunter.jreaper.Colourmap;

/**
 *
 * @author kei041
 */
public class CandidateManager {

    private String rootPath;
    private DatabaseManager dbMan;
    private CandidateMatchingManager candMatcher;
    private CandidateSearchManager candSearchMan;

    public CandidateManager(String rootPath, DatabaseManager dbMan, BackgroundTaskRunner bgRunner) {
        this.rootPath = rootPath;
        this.dbMan = dbMan;
        candMatcher = new CandidateMatchingManager(dbMan, this, bgRunner);
        candSearchMan = new CandidateSearchManager(this);
    }

    public ClassifiedCandidateIndex searchCandidates(ClassifiedCandidateSelectRequest req) {
        return candSearchMan.searchCandidates(req);
    }

    public File getRawCandidateFile(RawCandidateStub rawCandStub) {
        return new File(this.getRawCandidateFilePath(rawCandStub));
    }

    public String getRawCandidateFilePath(RawCandidateStub stub) {
        CandidateListStub candListStub = (CandidateListStub) dbMan.getById(stub.getCandidateListId());
        return this.getRawCandidateFilePath(stub, candListStub);
    }

    public String getRawCandidateFilePath(RawCandidateStub stub, CandidateListStub candListStub) {
        long procId = candListStub.getProcessingId();
        long psrxmlId = candListStub.getPsrxmlId();
        long candlistId = candListStub.getId();
        String datetime = candListStub.getObservedDate();
        String datePath = "UNK/UNK";
        if (datetime != null) {
            datePath = datetime.substring(0, 7) + "/" + datetime.substring(0, 10);
        }
        StringBuffer buf = new StringBuffer();
        Formatter formatter = new Formatter(buf);
        formatter.format("%s/%s/%016x/%016x/%016x_%s/%016x.xml.gz", rootPath, datePath, psrxmlId, procId, candlistId, candListStub.getName(), stub.getId());
        return buf.toString();
    }

    public String getCandidateListPath(CandidateListStub candListStub) {
        long procId = candListStub.getProcessingId();
        long psrxmlId = candListStub.getPsrxmlId();
        long candlistId = candListStub.getId();
        String datetime = candListStub.getObservedDate();
        String datePath = "UNK/UNK";
        if (datetime != null) {
            datePath = datetime.substring(0, 7) + "/" + datetime.substring(0, 10);
        }
        StringBuffer buf = new StringBuffer();
        Formatter formatter = new Formatter(buf);
        formatter.format("%s/%s/%016x/%016x/%016x_%s.xml.gz", rootPath,datePath, psrxmlId, procId, candlistId, candListStub.getName());
        return buf.toString();
    }

    public String getCandidateListDirectoryPath(CandidateListStub candListStub) {
        long procId = candListStub.getProcessingId();
        long psrxmlId = candListStub.getPsrxmlId();
        long candlistId = candListStub.getId();
        String datetime = candListStub.getObservedDate();
        String datePath = "UNK/UNK";
        if (datetime != null) {
            datePath = datetime.substring(0, 7) + "/" + datetime.substring(0, 10);
        }
        StringBuffer buf = new StringBuffer();
        Formatter formatter = new Formatter(buf);
        formatter.format("%s/%s/%016x/%016x/%016x_%s", rootPath,datePath, psrxmlId, procId, candlistId, candListStub.getName());
        return buf.toString();
    }

    public String getLegacyRawCandidateFilePath(RawCandidateStub stub, CandidateListStub candListStub) {
        long procId = candListStub.getProcessingId();
        long psrxmlId = candListStub.getPsrxmlId();
        long candlistId = candListStub.getId();
        StringBuffer buf = new StringBuffer();
        Formatter formatter = new Formatter(buf);
        formatter.format("%s/%016x/%016x/%016x_%s/%016x.xml.gz", rootPath, psrxmlId, procId, candlistId, candListStub.getName(), stub.getId());
        return buf.toString();
    }

    public String getLegacyCandidateListPath(CandidateListStub candListStub) {
        long procId = candListStub.getProcessingId();
        long psrxmlId = candListStub.getPsrxmlId();
        long candlistId = candListStub.getId();
        StringBuffer buf = new StringBuffer();
        Formatter formatter = new Formatter(buf);
        formatter.format("%s/%016x/%016x/%016x_%s.xml.gz", rootPath, psrxmlId, procId, candlistId, candListStub.getName());
        return buf.toString();
    }

    public String getLegacyCandidateListDirectoryPath(CandidateListStub candListStub) {
        long procId = candListStub.getProcessingId();
        long psrxmlId = candListStub.getPsrxmlId();
        long candlistId = candListStub.getId();
        StringBuffer buf = new StringBuffer();
        Formatter formatter = new Formatter(buf);
        formatter.format("%s/%016x/%016x/%016x_%s", rootPath, psrxmlId, procId, candlistId, candListStub.getName());
        return buf.toString();
    }

    public File getCandidateListFile(CandidateListStub candListStub) {
        return new File(getCandidateListPath(candListStub));
    }

    public CandidateListIndex getCandidateLists(CandListSelectionRequest req) {

        CandidateListIndex idx = new CandidateListIndex();
        ArrayList<CandidateListStub> clists = new ArrayList<CandidateListStub>();
        ArrayList<CandidateListStub> interim;
        for (IdAble idable : dbMan.getAllOfType(TypeIdManager.getTypeFromClass(CandidateListStub.class))) {
            clists.add((CandidateListStub) idable);
        }

        if (req.getNameMatch() != null) {
            interim = new ArrayList<CandidateListStub>();
            if (req.getUseRegex()) {
                Pattern pat = Pattern.compile(req.getNameMatch());
                for (CandidateListStub cl : clists) {
                    if (pat.matcher(cl.getName()).matches()) {
                        interim.add(cl);
                    }
                }
            } else {
                for (CandidateListStub cl : clists) {
                    if (cl.getName().contains(req.getNameMatch())) {
                        interim.add(cl);
                    }
                }
            }
            clists = interim;
            interim = null;
        }

        if (req.getStartCompletedDate() != null || req.getEndCompletedDate() != null) {
            interim = new ArrayList<CandidateListStub>();

            long end = Long.MAX_VALUE;
            if (req.getEndCompletedDate() != null) {
                end = req.getEndCompletedDate().getTime();
            }
            long start = 0;
            if (req.getStartCompletedDate() != null) {
                start = req.getStartCompletedDate().getTime();
            }
            for (CandidateListStub cl : clists) {
                long t = cl.getCompletedDateAlt().getTime();
                if (t < end && t > start) {
                    interim.add(cl);
                }
            }
            clists = interim;
            interim = null;
        }

        if (req.getAcceptProcessingIds() != null) {
            interim = new ArrayList<CandidateListStub>();
            long[] pids = req.getAcceptProcessingIds();
            Arrays.sort(pids);
            for (CandidateListStub cl : clists) {
                if (Arrays.binarySearch(pids, cl.getProcessingId()) >= 0) {
                    // we find the element:
                    interim.add(cl);
                }
            }
            clists = interim;
            interim = null;
        }

        if (req.getAcceptPsrxmlIds() != null) {
            interim = new ArrayList<CandidateListStub>();
            long[] pids = req.getAcceptPsrxmlIds();
            Arrays.sort(pids);
            for (CandidateListStub cl : clists) {
                if (Arrays.binarySearch(pids, cl.getPsrxmlId()) >= 0) {
                    // we find the element:
                    interim.add(cl);
                }
            }
            clists = interim;
            interim = null;
        }

        idx.setList(clists);
        return idx;
    }

    public BufferedImage makeImageOf(RawCandidate candidate, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        RasterImageCandidatePlot plot = new RasterImageCandidatePlot(img);

        GenerateRawCandidatePlot.generate(plot, candidate, Colourmap.defaultGreyColmap, Color.RED, Color.BLUE);
        return img;
    }

    public ArrayList<ClassifiedCandidate> getClassifiedCandidatesNear(Coordinate coord, double distmax) {

        List<IdAble> idables = this.dbMan.getAllOfType(TypeIdManager.getTypeFromClass(ClassifiedCandidate.class));
        ArrayList<ClassifiedCandidate> out = new ArrayList<ClassifiedCandidate>();

        if (coord != null) {
            CoordinateDistanceComparitor distComp = new CoordinateDistanceComparitor(coord);
            for (IdAble idable : idables) {
                ClassifiedCandidate c = (ClassifiedCandidate) idable;
                if (distComp.difference(c.getCoordinate(), coord) > distmax) {
                    continue;
                }
                out.add(c);
            }
        } else {
            for (IdAble idable : idables) {
                ClassifiedCandidate c = (ClassifiedCandidate) idable;
                out.add(c);
            }
        }
        return out;
    }

    public void addRawCandidates(ArrayList<RawCandidate> rawCands, long psrxmlId, long procId) {

        long candlistId = 0;

        CandidateList candlist = new CandidateList();
        CandidateListStub candlistStub = new CandidateListStub();

        Session session = new Session();
        dbMan.add(candlistStub, session);

        candlist.setId(candlistStub.getId());

        RawCandidate cand0 = rawCands.get(0);
        candlist.setName(cand0.getSourceId());
        candlistStub.setName(cand0.getSourceId());
        Date now = new Date();
        candlistStub.setCompletedDate(now);
        candlist.setCompletedDate(now);

        candlist.setNcands(rawCands.size());
        candlistStub.setNcands(rawCands.size());

        candlist.setProcessingId(procId);
        candlistStub.setProcessingId(procId);

        candlist.setPsrxmlId(psrxmlId);
        candlistStub.setPsrxmlId(psrxmlId);

        candlist.setCoordinate(cand0.getCoordinate());
        candlistStub.setCoordinate(cand0.getCoordinate());

        candlistId = candlistStub.getId();
        Logger.getLogger(CandidateManager.class.getName()).log(Level.INFO, "Creating database stub candidates for candlist " + StringConvertable.ID.toString(candlist.getId()) + "");

        final RawCandidateStub[] stubs = new RawCandidateStub[rawCands.size()];
        for (int i = 0; i < stubs.length; i++) {
            RawCandidate cand = rawCands.get(i);

            // make the candidate stub.
            stubs[i] = new RawCandidateStub();
            dbMan.add(stubs[i], session);
            stubs[i].setCandidateListId(candlist.getId());

            // set the ids on the candidate.
            cand.setCandidateListId(candlist.getId());
            cand.setId(stubs[i].getId());

            // make the basic candidate for the candlist.
            RawCandidateBasic candBasic = new RawCandidateBasic();
            candBasic.setId(cand.getId());
            candBasic.setCandidateListId(candlistId);
            candBasic.setReconSnr(cand.getReconstructedSnr());
            candBasic.setSpecSnr(cand.getSpectralSnr());
            ArrayList<String> keys = (ArrayList<String>) cand.getKeys().clone();
            Collections.reverse(keys);
            for (String key : keys) {
                RawCandidateSection section = cand.getSections().get(key);
                if (Float.isNaN(candBasic.getTopoPeriod())) {
                    candBasic.setTopoPeriod(section.getBestTopoPeriod());
                }
                if (Float.isNaN(candBasic.getBaryPeriod())) {
                    candBasic.setBaryPeriod(section.getBestBaryPeriod());
                }
                if (Float.isNaN(candBasic.getAccel())) {
                    candBasic.setAccel(section.getBestAccn());
                }
                if (Float.isNaN(candBasic.getJerk())) {
                    candBasic.setJerk(section.getBestJerk());
                }
                if (Float.isNaN(candBasic.getDm())) {
                    candBasic.setDm(section.getBestDm());
                }
                if (Float.isNaN(candBasic.getFoldSnr())) {
                    candBasic.setFoldSnr(section.getBestSnr());
                }
            }

            candlist.addRawCandidateBasic(candBasic);

        }


        Logger.getLogger(CandidateManager.class.getName()).log(Level.INFO, "Saving raw candidates for " + StringConvertable.ID.toString(candlist.getId()) + " to disk");

        File candDir = new File(this.getCandidateListDirectoryPath(candlistStub));
        candDir.mkdirs();

        GZIPOutputStream out = null;





        try {
            File rawFile = new File(this.getCandidateListPath(candlistStub));
            out = new GZIPOutputStream(new FileOutputStream(rawFile));
            XMLWriter.write(out, candlist);
            out.close();

            for (int i = 0; i < stubs.length; i++) {
                rawFile = new File(this.getRawCandidateFilePath(stubs[i], candlistStub));
                out = new GZIPOutputStream(new FileOutputStream(rawFile));
                XMLWriter.write(out, rawCands.get(i));
                out.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(CandidateManager.class.getName()).log(Level.SEVERE, "Failed to write raw candidates to disk!", ex);
        }


        Logger.getLogger(CandidateManager.class.getName()).log(Level.INFO, "Saving raw candidates for " + StringConvertable.ID.toString(candlist.getId()) + " to database");
        try {
            dbMan.save(session);
        } catch (BookKeeprException ex) {
            Logger.getLogger(CandidateManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        Logger.getLogger(CandidateManager.class.getName()).log(Level.INFO, "Successfully processed " + candlist.getRawCandidateBasicList().size() + " raw candidates for candlist " + StringConvertable.ID.toString(candlist.getId()) + " into database");
    }
}
