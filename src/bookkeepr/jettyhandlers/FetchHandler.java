/*
 * Copyright (C) 2005-2007 Michael Keith, University Of Manchester
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

import bookkeepr.BookKeeprException;

import bookkeepr.xml.XMLAble;
import bookkeepr.xml.XMLReader;
import bookkeepr.xml.XMLWriter;
import bookkeepr.xmlable.BookkeeprHost;
import bookkeepr.xmlable.DatabaseManager;
import bookkeepr.xmlable.IndexIndex;
import bookkeepr.xmlable.Session;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;
import org.xml.sax.SAXException;
import bookkeepr.xmlable.Index;
import bookkeepr.BookKeepr;
import bookkeepr.xml.IdAble;

/**
 *
 * @author kei041
 */
public class FetchHandler extends AbstractHandler {

    private static Pattern regex = Pattern.compile("/");
    DatabaseManager manager;
    BookKeepr bookkeepr;

    public FetchHandler(BookKeepr bookkeepr) {
        this.manager = bookkeepr.getMasterDatabaseManager();
        this.bookkeepr = bookkeepr;
    }

    public void handle(String path, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
        if (request.getMethod().equals("GET")) {
            if (path.startsWith("/id/")) {
                ((Request) request).setHandled(true);
                String[] elems = regex.split(path.substring(1));
                if (elems.length > 1) {
                    String idStr = elems[1];
                    boolean xsl = true;
                    if (idStr.equals("raw") && elems.length == 3) {
                        idStr = elems[2];
                        xsl = false;
                    }
                    long id = Long.parseLong(idStr, 16);
                    IdAble idable = manager.getById(id);
                    if (idable != null && idable instanceof XMLAble) {
                        XMLAble xmlable = ((XMLAble) idable);
                        XMLWriter.write(response.getOutputStream(), xmlable, xsl);
                        response.getOutputStream().close();
                    } else {
                        response.sendError(404, "Requested element not found");
                        return;
                    }
                }
            } else if (path.startsWith("/update/")) {
                ((Request) request).setHandled(true);
                String[] elems = regex.split(path.substring(1));
                if (elems.length == 2) {
                    long id = Long.parseLong(elems[1], 16);
                    IdAble idable = manager.getById(id);
                    if (idable != null && idable instanceof Session) {
                        Session session = ((Session) idable);
                        IndexIndex idx = manager.getFromSession(session);
                        if (idx == null) {
                            response.sendError(404, "Requested session is not avaliable");
                            return;
                        }
                        XMLWriter.write(response.getOutputStream(), idx);
                        response.getOutputStream().close();
                    } else {
                        response.sendError(404, "Requested session not found");
                        return;
                    }
                }
            } else if (path.startsWith("/sync/")) {
                ((Request) request).setHandled(true);
                String[] elems = regex.split(path.substring(1));
                if (elems.length == 2) {
                    int type = Integer.parseInt(elems[1]);
                    type = type & 0xFF; // to make sure that the value will always be valid.

                    IdAble idable = manager.getLatest(manager.getOriginId(), type);
                    if (idable != null && idable instanceof XMLAble) {
                        XMLAble xmlable = ((XMLAble) idable);
                        XMLWriter.write(response.getOutputStream(), xmlable);
                        response.getOutputStream().close();

                    } else {
                        response.sendError(404, "Requested element not found");
                        return;
                    }
                }
                if (elems.length == 3) {
                    int type = Integer.parseInt(elems[2]);
                    type = type & 0xFF; // to make sure that the value will always be valid.

                    int origin = Integer.parseInt(elems[1]);
                    origin = origin & 0xFF; // to make sure that the value will always be valid.

                    IdAble idable = manager.getLatest(origin, type);
                    if (idable != null && idable instanceof XMLAble) {
                        XMLAble xmlable = ((XMLAble) idable);
                        XMLWriter.write(response.getOutputStream(), xmlable);
                        response.getOutputStream().close();
                    } else {
                        response.sendError(404, "Requested element not found");
                        return;
                    }
                }
            } else if (path.equals("/ident/") || path.equals("/ident")) {
                ((Request) request).setHandled(true);

                BookkeeprHost self = bookkeepr.getHost();
                XMLWriter.write(response.getOutputStream(), self);
                response.getOutputStream().close();

            }
        } else if (request.getMethod().equals("POST")) {
            if (path.equals("/insert/") || path.equals("/insert")) {
                ((Request) request).setHandled(true);
                try {
                    XMLAble xmlable = (XMLAble) XMLReader.read(request.getInputStream());
                    if (xmlable instanceof IdAble) {
                        Logger.getLogger(ObservationHandler.class.getName()).log(Level.INFO, "Inserting Item into database");
                        Session session = new Session();
                        this.manager.add((IdAble) xmlable, session);
                        this.manager.save(session);
                        XMLWriter.write(response.getOutputStream(), xmlable);
                        response.getOutputStream().close();
                    } else if (xmlable instanceof Index) {
                        Logger.getLogger(ObservationHandler.class.getName()).log(Level.INFO, "Inserting Index into database");
                        Index idx = (Index) xmlable;
                        Session session = new Session();
                        for (Object o : idx.getIndex()) {
                            if (o instanceof IdAble) {
                                this.manager.add((IdAble) o, session);
                            }
                        }
                        this.manager.save(session);
                    } else if (xmlable instanceof IndexIndex) {
                        Logger.getLogger(ObservationHandler.class.getName()).log(Level.INFO, "Inserting Multiple indexes into database");

                        IndexIndex idxidx = (IndexIndex) xmlable;
                        Session session = new Session();
                        for (Index idx : idxidx.getIndexList()) {
                            for (Object o : idx.getIndex()) {
                                if (o instanceof IdAble) {
                                    this.manager.add((IdAble) o, session);
                                }
                            }
                        }
                        this.manager.save(session);
                    } else {
                        response.sendError(400, "Submitted request was not understood");
                        Logger.getLogger(ObservationHandler.class.getName()).log(Level.INFO, "Recieved malformed request, could not insert non-idable items");


                    }
                } catch (BookKeeprException ex) {
                    Logger.getLogger(FetchHandler.class.getName()).log(Level.WARNING, null, ex);
                    response.sendError(500, "Could not modify external item as owner server was unavaliable");

                } catch (SAXException ex) {
                    response.sendError(400, "Submitted request was malformed");
                    Logger.getLogger(ObservationHandler.class.getName()).log(Level.INFO, "Recieved malformed request", ex);
                }

            } else if (path.equals("/delete/") || path.equals("/delete")) {
                ((Request) request).setHandled(true);
                try {
                    XMLAble xmlable = (XMLAble) XMLReader.read(request.getInputStream());
                    if (xmlable instanceof IdAble) {
                        Session session = new Session();
                        this.manager.remove((IdAble) xmlable, session);
                        this.manager.save(session);
                    } else if (xmlable instanceof Index) {
                        Index idx = (Index) xmlable;
                        Session session = new Session();
                        for (Object o : idx.getIndex()) {
                            if (o instanceof IdAble) {
                                this.manager.remove((IdAble) o, session);
                            }
                        }
                        this.manager.save(session);
                    } else if (xmlable instanceof IndexIndex) {
                        IndexIndex idxidx = (IndexIndex) xmlable;
                        Session session = new Session();
                        for (Index idx : idxidx.getIndexList()) {
                            for (Object o : idx.getIndex()) {
                                if (o instanceof IdAble) {
                                    this.manager.remove((IdAble) o, session);
                                }
                            }
                        }
                        this.manager.save(session);
                    } else {
                        response.sendError(400, "Submitted request was not understood");
                        Logger.getLogger(ObservationHandler.class.getName()).log(Level.INFO, "Recieved malformed request, could not remove non-idable items");


                    }
                } catch (BookKeeprException ex) {
                    Logger.getLogger(FetchHandler.class.getName()).log(Level.WARNING, null, ex);
                    response.sendError(500, "Could not modify external item as owner server was unavaliable");

                } catch (SAXException ex) {
                    response.sendError(400, "Submitted request was malformed");
                    Logger.getLogger(ObservationHandler.class.getName()).log(Level.INFO, "Recieved malformed request");
                }

            }
        }
    }
}
