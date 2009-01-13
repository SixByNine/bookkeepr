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
import bookkeepr.xmlable.Pointing;
import bookkeepr.xmlable.PointingSelectRequest;
import bookkeepr.xmlable.Psrxml;
import bookkeepr.xmlable.Session;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kei041
 */
public class PsrXMLManager {

    private ObservationManager obsmanager;

    public PsrXMLManager(ObservationManager obsmanager) {
        this.obsmanager = obsmanager;
    }

    public void addPsrXML(Psrxml header) throws PointingNotFoundException, BookKeeprException {
        Session session = new Session();
        addPsrXML(header, session);
        obsmanager.getDbManager().save(session);
    }

    public void addPsrXML(Psrxml header, Session session) throws PointingNotFoundException, BookKeeprException {

        String gridID = header.getSourceNameCentreBeam();
        if (gridID == null) {
            gridID = header.getSourceName();
        }
//        PointingSelectRequest req = new PointingSelectRequest();
//        req.setTarget(header.getStartCoordinate());
//        req.setTargetSeperation(10.0f);
//        req.setGridId(gridID);

        Pointing ptg = obsmanager.getPointingFromGridId(gridID);
//        List<Pointing> ptgs = obsmanager.getPointings(req).getIndex();
        if (ptg != null) {
            // we found an exact match... this is good!

            header.setPointingId(ptg.getId());

            Psrxml repl = this.obsmanager.getPsrxmlFromUtcBeam(header.getUtc(), header.getReceiverBeam());
            // if we already have a psrxml from this beam and utc, replace the old one!
            if (repl != null) {
                header.setId(repl.getId());
                Logger.getLogger(PsrXMLManager.class.getName()).log(Level.FINE, "Replacing PsrXML " + Long.toHexString(header.getId()));
            }
            header.setCatReference("BookKeepr::" + obsmanager.getDbManager().getExternalUrl());

            if (ptg.getToBeObserved()) {
                // if the pointing is to be observed, then mark it as observed.
                Pointing newptg = null;
                try {
                    newptg = (Pointing) ptg.clone();
                } catch (CloneNotSupportedException ex) {
                    Logger.getLogger(PsrXMLManager.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException(ex);
                }

                newptg.setToBeObserved(false);
                obsmanager.getDbManager().add(newptg, session);
            }
            obsmanager.getDbManager().add(header, session);

            Logger.getLogger(PsrXMLManager.class.getName()).log(Level.FINE, "Added PsrXML file " + Long.toHexString(header.getId()) + " to database");
        } else {
            // there is a problem... we have too many or too few matches
            throw new PointingNotFoundException("No pointings mached GridId " + gridID + ". This means that the PsrXML file cannot be added to the database.");
        }
    }

    public class PointingNotFoundException extends BookKeeprException {

        public PointingNotFoundException() {
        }

        public PointingNotFoundException(String message) {
            super(message);
        }

        public PointingNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }

        public PointingNotFoundException(Throwable cause) {
            super(cause);
        }
    }
}
