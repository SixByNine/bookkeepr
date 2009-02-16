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
package bookkeepr.jettyhandlers;

import bookkeepr.BookKeepr;
import bookkeepr.BookKeeprException;
import bookkeepr.managers.ArchiveManager;
import bookkeepr.managers.TypeIdManager;
import bookkeepr.xml.IdAble;
import bookkeepr.xml.StringConvertable;
import bookkeepr.xml.XMLAble;
import bookkeepr.xml.XMLReader;
import bookkeepr.xml.XMLWriter;
import bookkeepr.xmlable.ArchivedStorage;
import bookkeepr.xmlable.ArchivedStorageIndex;
import bookkeepr.xmlable.ArchivedStorageWrite;
import bookkeepr.xmlable.DatabaseManager;
import bookkeepr.xmlable.Psrxml;
import bookkeepr.xmlable.PsrxmlIndex;
import bookkeepr.xmlable.Session;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author kei041
 */
public class ArchivedStorageHandler extends AbstractHandler {

    private static Pattern regex = Pattern.compile("/");
    private DatabaseManager dbMan;
    private BookKeepr bookkeepr;
    private ArchiveManager archiveMan;

    public ArchivedStorageHandler(BookKeepr bookkeepr, ArchiveManager archiveMan) {
        this.dbMan = bookkeepr.getMasterDatabaseManager();
        this.bookkeepr = bookkeepr;
        this.archiveMan = archiveMan;
    }

    public void handle(String path, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
        if (request.getMethod().equals("GET")) {
            if (path.startsWith("/storage/")) {
                ((Request) request).setHandled(true);


                String[] elems = regex.split(path.substring(1));
                if (elems.length > 1) {
                    String idStr = elems[1];
                    if (idStr.equals("label") && elems.length > 2) {
                        String label = elems[2];
                        ArchivedStorage storage = archiveMan.getStorageByLabel(label);
                        if (storage != null) {
                            Logger.getLogger(ArchivedStorageHandler.class.getName()).log(Level.INFO, "Returning Storage with label " + label);
                            XMLWriter.write(response.getOutputStream(), storage);
                            response.getOutputStream().close();
                        } else {
                            Logger.getLogger(ArchivedStorageHandler.class.getName()).log(Level.INFO, "Did not find Storage with label " + label);

                            response.sendError(404, "Requested media label not found");
                            return;
                        }
                    } else { // we are requesting an index of a archive or psrxml

                        long id = StringConvertable.ID.fromString(idStr);
                        IdAble idAble = dbMan.getById(id);
                        if (idAble instanceof Psrxml) {
                            ArchivedStorageIndex idx = archiveMan.getStorageForPsrxmlId(id);
                            if (idx != null) {
                                XMLWriter.write(response.getOutputStream(), idx);
                                response.getOutputStream().close();
                            } else {
                                Logger.getLogger(ArchivedStorageHandler.class.getName()).log(Level.INFO, "Bad request for storage from psrxmlid");
                                response.sendError(404, "Nothing known archived for psrxml id " + idStr);
                                return;
                            }
                        } else if (idAble instanceof ArchivedStorage) {
                            PsrxmlIndex idx = archiveMan.getPsrxmlForStorageId(id);
                            if (idx != null) {
                                XMLWriter.write(response.getOutputStream(), idx);
                                response.getOutputStream().close();
                            } else {
                                Logger.getLogger(ArchivedStorageHandler.class.getName()).log(Level.INFO, "Bad request for psrxml from storageid");
                                response.sendError(404, "Nothing known archived for archive id " + idStr);
                                return;
                            }
                        } else {
                            Logger.getLogger(ArchivedStorageHandler.class.getName()).log(Level.INFO, "Bad request for storage");

                            response.sendError(400, "Requested id not valid");
                            return;
                        }

                    }
                } else { // list all tapes!

                    List list = dbMan.getAllOfType(TypeIdManager.getTypeFromClass(ArchivedStorage.class));
                    ArchivedStorageIndex idx = new ArchivedStorageIndex();
                    for (Object o : list) {
                        idx.addArchivedStorage((ArchivedStorage) o);
                    }

                    XMLWriter.write(response.getOutputStream(), idx);
                    response.getOutputStream().close();
                    return;
                }
            }

        } else if (request.getMethod().equals("POST")) {
            if (path.equals("/storage/") || path.equals("/storage")) {
                try {
                    ((Request) request).setHandled(true);
                    XMLAble xmlable = (XMLAble) XMLReader.read(request.getInputStream());
                    if (xmlable instanceof ArchivedStorage) {
                        ArchivedStorage storage = (ArchivedStorage) xmlable;
                        if (this.archiveMan.getStorageByLabel(storage.getMediaLabel()) != null) {
                            response.sendError(400, "Tape label " + storage.getMediaLabel() + " already exists!");
                            Logger.getLogger(ArchivedStorageHandler.class.getName()).log(Level.INFO, "Tape label " + storage.getMediaLabel() + " already exists!");

                            return;
                        }
                        Logger.getLogger(ObservationHandler.class.getName()).log(Level.INFO, "Inserting Item into database");
                        Session session = new Session();
                        this.dbMan.add(storage, session);
                        this.dbMan.save(session);
                        XMLWriter.write(response.getOutputStream(), xmlable);
                        response.getOutputStream().close();
                    } else if (xmlable instanceof ArchivedStorageWrite) {
                        ArchivedStorageWrite write = (ArchivedStorageWrite) xmlable;
                        ArchivedStorageWrite existing = this.archiveMan.checkUnique(write);

                        if (existing != null) {
                            write.setId(existing.getId()); // replace the existing entry with the new one.

                            if (write.equals(existing)) {
                                XMLWriter.write(response.getOutputStream(), existing);
                                response.getOutputStream().close();
                                Logger.getLogger(ObservationHandler.class.getName()).log(Level.INFO, "Not replacing identical storage write.");

                                return;
                            }
                        }

                        Logger.getLogger(ObservationHandler.class.getName()).log(Level.INFO, "Inserting Item into database");
                        Session session = new Session();
                        this.dbMan.add(write, session);
                        this.dbMan.save(session);
                        XMLWriter.write(response.getOutputStream(), xmlable);
                        response.getOutputStream().close();
                    }
                } catch (BookKeeprException ex) {
                    Logger.getLogger(ArchivedStorageHandler.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
                    response.sendError(500, "BookKeepr error writing to database...");
                } catch (SAXException ex) {
                    Logger.getLogger(ArchivedStorageHandler.class.getName()).log(Level.INFO, ex.getMessage(), ex);
                }
            } else if (path.startsWith("/storage/relocate")) {
                try {
                    ((Request) request).setHandled(true);
                    String[] elems = regex.split(path.substring(1));


                    String label = elems[2];
                    ArchivedStorage storage = archiveMan.getStorageByLabel(label);
                    if (storage != null) {
                        Logger.getLogger(ArchivedStorageHandler.class.getName()).log(Level.INFO, "Relocating Storage with label " + label);
                        XMLAble xmlable = (XMLAble) XMLReader.read(request.getInputStream());
                        ArchivedStorage newloc = (ArchivedStorage) xmlable;
                        newloc.setId(storage.getId());
                        newloc.setMediaLabel(label);
                        newloc.setMediaType(storage.getMediaType());
                        Session session = new Session();
                        
                        this.dbMan.add(newloc, session);
                        this.dbMan.save(session);

                        XMLWriter.write(response.getOutputStream(), newloc);
                        response.getOutputStream().close();
                    } else {
                        Logger.getLogger(ArchivedStorageHandler.class.getName()).log(Level.INFO, "Did not find Storage with label " + label);

                        response.sendError(404, "Requested media label not found");
                        return;
                    }
                } catch (BookKeeprException ex) {
                    Logger.getLogger(ArchivedStorageHandler.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
                    response.sendError(500, "BookKeepr error writing to database...");
                } catch (SAXException ex) {
                    Logger.getLogger(ArchivedStorageHandler.class.getName()).log(Level.INFO, ex.getMessage(), ex);
                }
            }
        }



    }
}