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
package bookkeepr;

import bookkeepr.managers.TypeIdManager;
import bookkeepr.xml.IdAble;
import bookkeepr.xml.XMLReader;
import bookkeepr.xml.XMLWriter;
import bookkeepr.xmlable.BookkeeprHost;
import bookkeepr.xmlable.CandidateListStub;
import bookkeepr.xmlable.Psrxml;
import bookkeepr.xmlable.Session;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.SAXException;

/**
 *
 * @author kei041
 */
public class UpgradeChecker {

    static int upgradeFrom = -1;

    static void pre_load(String dbLocation, BookKeepr bk) {
        File file = new File(dbLocation + File.separator + "_host.xml");
        if (file.exists()) {
            try {
                BookkeeprHost host = (BookkeeprHost) XMLReader.read(new FileInputStream(file));
                upgradeFrom = host.getVersion();
            } catch (SAXException sAXException) {
            } catch (IOException iOException) {
            }
        }
        if (upgradeFrom < bk.getHost().getVersion()) {
            Logger.getLogger(UpgradeChecker.class.getName()).log(Level.INFO, "***UPGRADING from version " + upgradeFrom + " to " + bk.getHost().getVersion());
        }
        if (upgradeFrom > bk.getHost().getVersion()) {
            Logger.getLogger(UpgradeChecker.class.getName()).log(Level.SEVERE, "ERROR! Cannot downgrade between bookkeepr versions!");
            System.exit(1);

        }
    }

    static void post_load(String dbLocation, BookKeepr bk) {
        if (upgradeFrom < bk.getHost().getVersion()) {
            Logger.getLogger(UpgradeChecker.class.getName()).log(Level.INFO, "***UPGRADING from version " + upgradeFrom + " to " + bk.getHost().getVersion());
        }
        if (upgradeFrom < 2) {
            /*
             * Upgrade from pre version 2 candidate lists
             */
            Logger.getLogger(UpgradeChecker.class.getName()).log(Level.INFO, "Adding date field to candidate lists");
            Logger.getLogger(UpgradeChecker.class.getName()).log(Level.INFO, "Moving candidate lists to new date based directories");
            List<IdAble> idlist = bk.getMasterDatabaseManager().getAllOfType(TypeIdManager.getTypeFromClass(CandidateListStub.class));
            Session s = new Session();
            for (IdAble idable : idlist) {

                if (bk.getMasterDatabaseManager().getOrigin(idable) == bk.getMasterDatabaseManager().getOriginId()) {
                    CandidateListStub clistStub = (CandidateListStub) idable;
                    // Add the date field to the clists
                    Psrxml psrxml = (Psrxml) bk.getMasterDatabaseManager().getById(clistStub.getPsrxmlId());
                    
                    clistStub.setObservedDate(psrxml.getUtc());
                    bk.getMasterDatabaseManager().add(clistStub, s);

                    // move the files to the new path.
                    {
                        String newpath = bk.getCandManager().getCandidateListDirectoryPath(clistStub);
                        String oldpath = bk.getCandManager().getLegacyCandidateListDirectoryPath(clistStub);
                        File newFile = new File(newpath);
                        File oldFile = new File(oldpath);
                        if (oldFile.exists()) {
                            newFile.getParentFile().mkdirs();
                            boolean success = oldFile.renameTo(newFile);

                            if (!success) {
                                Logger.getLogger(UpgradeChecker.class.getName()).log(Level.SEVERE, "ERROR! A failure occured trying to move candidate list dir to new path " + newFile.getAbsolutePath());
                                System.exit(1);
                            }
                        } else {
                            Logger.getLogger(UpgradeChecker.class.getName()).log(Level.WARNING, "Candidate list dir " + oldFile.getName() + " did not exist");
                        }
                    }
                    {
                        String newpath = bk.getCandManager().getCandidateListPath(clistStub);
                        String oldpath = bk.getCandManager().getLegacyCandidateListPath(clistStub);
                        File newFile = new File(newpath);
                        File oldFile = new File(oldpath);
                        if (oldFile.exists()) {
                            newFile.getParentFile().mkdirs();
                            boolean success = oldFile.renameTo(newFile);

                            if (!success) {
                                Logger.getLogger(UpgradeChecker.class.getName()).log(Level.SEVERE, "ERROR! A failure occured trying to move candidate list file to new path " + newFile.getAbsolutePath());
                                System.exit(1);
                            }

                            oldFile.delete();
                            //remove the parent directories if empty.
                            if (oldFile.getParentFile().list().length == 0) {
                                oldFile.getParentFile().delete();
                                if (oldFile.getParentFile().getParentFile().list().length == 0) {
                                    oldFile.getParentFile().getParentFile().delete();
                                }
                            }
                        } else {
                            Logger.getLogger(UpgradeChecker.class.getName()).log(Level.WARNING, "Candidate list file " + oldFile.getName() + " did not exist");
                        }
                    }

                }

            }
            try {
                if(s.getModifiedKeyList().size()!=0)
                    bk.getMasterDatabaseManager().save(s);
            } catch (BookKeeprException ex) {
                Logger.getLogger(UpgradeChecker.class.getName()).log(Level.SEVERE, "Error updating database!", ex);
                System.exit(1);
            }
        }

        File file = new File(dbLocation + File.separator + "_host.xml");
        try {
            XMLWriter.write(new FileOutputStream(file), bk.getHost());
        } catch (IOException iOException) {
            Logger.getLogger(UpgradeChecker.class.getName()).log(Level.WARNING, "Could not write _host.xml file in db dir", iOException);
        }

    }
}
