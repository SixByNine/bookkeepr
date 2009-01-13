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
package bookkeepr.managers;

import bookkeepr.BackgroundTaskRunner;
import bookkeepr.BookKeeprException;
import bookkeepr.xml.IdAble;
import bookkeepr.xmlable.BackgroundedTask;

import bookkeepr.xml.XMLReader;
import bookkeepr.xmlable.BookkeeprConfig;
import bookkeepr.xmlable.BookkeeprHost;
import bookkeepr.xmlable.DatabaseManager;
import bookkeepr.xmlable.Index;
import bookkeepr.xmlable.IndexIndex;
import bookkeepr.xmlable.Session;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.SAXException;

/**
 *
 * @author kei041
 */
public class SyncManager {

    private HttpClient httpclient;
    private DatabaseManager obsdb;
    private BookkeeprConfig config;
    private SessionManager sessionManager;
    private BackgroundTaskRunner bgrunner;
    private boolean run = true;

    public SyncManager(DatabaseManager obsdb, BookkeeprConfig config, SessionManager sessionManager, BackgroundTaskRunner bgrunner) {
        this.obsdb = obsdb;
        this.config = config;
        this.sessionManager = sessionManager;
        this.bgrunner = bgrunner;
        httpclient = new DefaultHttpClient();
        if (config.getProxyUrl() != null) {
            final HttpHost proxy =
                    new HttpHost(config.getProxyUrl(), config.getProxyPort(), "http");
            httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
    }

    private void delaySyncTask() {
        new Thread() {

            @Override
            public void run() {
                try {
                    Thread.sleep(config.getSyncTime() * 1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SyncManager.class.getName()).log(Level.INFO, "Sync manager prodded, forcing sync now.");
                }
                insertSyncTask();
            }
        }.start();
    }

    private void insertSyncTask() {
        if (run) {
            BackgroundedTask task = new BackgroundedTask("Synchronise database");
            task.setTarget(new Runnable() {

                public void run() {
                    try {
                        SyncManager.this.sync();
                        delaySyncTask();
                    } catch (Exception e) {
                        Logger.getLogger(SyncManager.class.getName()).log(Level.SEVERE, "Error syncing database!", e);
                        delaySyncTask();
                        throw new RuntimeException(e);
                    }
                }
            });

            if (!bgrunner.offer(task)) {
                delaySyncTask();
            }

        }
    }

    public void run() {
        this.run = true;
        insertSyncTask();
    }

    public void terminate() {
        this.run = false;
    }

    public void sync() {

        for (BookkeeprHost host : config.getBookkeeprHostList()) {

            Logger.getLogger(SyncManager.class.getName()).log(Level.INFO, "Attempting to sync with " + host.getUrl());
            try {


                HttpGet httpget = new HttpGet(host.getUrl() + "/ident/");
                HttpResponse response = httpclient.execute(httpget);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    InputStream in = response.getEntity().getContent();
                    BookkeeprHost rhost = (BookkeeprHost) XMLReader.read(in);
                    in.close();

                    Logger.getLogger(SyncManager.class.getName()).log(Level.FINE, "Managed to connect to " + host.getUrl() + "/ident/ ");
                    host.setOriginId(rhost.getOriginId());

                    host.setMaxOriginId(rhost.getMaxOriginId());
                } else {
                    Logger.getLogger(SyncManager.class.getName()).log(Level.WARNING, "Host " + host.getUrl() + " could not be identified");
                    continue;
                }

            } catch (SAXException ex) {
                Logger.getLogger(SyncManager.class.getName()).log(Level.SEVERE, null, ex);
                continue;
            } catch (IOException ex) {
                Logger.getLogger(SyncManager.class.getName()).log(Level.INFO, "Host " + host.getUrl() + " was not avaiable for syncing");
                continue;
            } catch (HttpException ex) {
                Logger.getLogger(SyncManager.class.getName()).log(Level.INFO, "Host " + host.getUrl() + " was not avaiable for syncing");
                continue;
            } catch (URISyntaxException ex) {
                Logger.getLogger(SyncManager.class.getName()).log(Level.SEVERE, null, ex);
                continue;
            }

            ArrayList<Session> sessions = new ArrayList<Session>();

            for (int originId = 0; originId <= host.getMaxOriginId(); originId++) {


                long maxRequestId;
                try {

                    String url = host.getUrl() + "/sync/" + originId + "/" + TypeIdManager.getTypeFromClass(Session.class);
                    HttpGet httpget = new HttpGet(url);
                    HttpResponse response = httpclient.execute(httpget);
                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

                        Logger.getLogger(SyncManager.class.getName()).log(Level.FINE, "Managed to connect to " + host.getUrl() + "/ident/ ");

                    } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                        Logger.getLogger(SyncManager.class.getName()).log(Level.INFO, "Up-to-date with host");
                        continue;
                    } else {
                        Logger.getLogger(SyncManager.class.getName()).log(Level.INFO, "Got an unexpected " + response.getStatusLine().getStatusCode() + " response from " + host.getUrl());
                        continue;
                    }

                    InputStream in = response.getEntity().getContent();
                    Session topSession = (Session) XMLReader.read(in);
                    in.close();

                    maxRequestId = topSession.getId();
                } catch (ClassCastException ex) {
                    Logger.getLogger(SyncManager.class.getName()).log(Level.WARNING, "Server " + host.getUrl() + " returned something that was not as session when asked for the latest session.", ex);
                    continue;
                } catch (HttpException ex) {
                    Logger.getLogger(SyncManager.class.getName()).log(Level.INFO, "Host " + host.getUrl() + " could not be contacted");
                    continue;
                } catch (URISyntaxException ex) {
                    Logger.getLogger(SyncManager.class.getName()).log(Level.WARNING, "Bad url " + host.getUrl() + "/sync/" + originId + "/" + TypeIdManager.getTypeFromClass(Session.class), ex);
                    continue;
                } catch (SAXException ex) {
                    Logger.getLogger(SyncManager.class.getName()).log(Level.WARNING, "Malformed XML file received from server " + host.getOriginId(), ex);
                    continue;
                } catch (IOException ex) {
                    Logger.getLogger(SyncManager.class.getName()).log(Level.INFO, "Host " + host.getUrl() + " was not avaiable for syncing");
                    continue;
                }

                long startId = sessionManager.getNextId(originId);
                Logger.getLogger(SyncManager.class.getName()).log(Level.INFO, "Expecting next session: " + Long.toHexString(startId));
                for (long requestId = startId; requestId <= maxRequestId; requestId++) {
                    if (originId == 0) {
                        Logger.getLogger(SyncManager.class.getName()).log(Level.SEVERE, "Server " + host.getUrl() + " claims that there are items created by server 0, which is impossible.");
                        break;
                    }
                    if (originId == obsdb.getOriginId()) {
                        Logger.getLogger(SyncManager.class.getName()).log(Level.SEVERE, "There are more up-to-date versions of data we created than in our database!");
                        break;
                    }
                    Logger.getLogger(SyncManager.class.getName()).log(Level.INFO, "Updating to session " + Long.toHexString(requestId));
                    try {
                        String url = host.getUrl() + "/update/" + Long.toHexString(requestId);

                        HttpGet httpget = new HttpGet(url);
                        HttpResponse response = httpclient.execute(httpget);
                        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        } else {
                            Logger.getLogger(SyncManager.class.getName()).log(Level.INFO, "Got an unexpected " + response.getStatusLine().getStatusCode() + " response from " + host.getUrl());
                            continue;
                        }

                        InputStream in = response.getEntity().getContent();
                        IndexIndex idxidx = (IndexIndex) XMLReader.read(in);
                        in.close();





                        Session session = new Session();
                        session.setId(requestId);

                        for (Index idx : idxidx.getIndexList()) {
                            for (Object idable : idx.getIndex()) {
                                obsdb.add((IdAble) idable, session);
                            }
                        }
                        sessions.add(session);
                    //obsdb.save(session);
                    } catch (HttpException ex) {
                        Logger.getLogger(SyncManager.class.getName()).log(Level.INFO, "HTTP exception connecting to host " + host.getUrl());
                        continue;
                    } catch (URISyntaxException ex) {
                        Logger.getLogger(SyncManager.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
                        break;
                    } catch (SAXException ex) {
                        Logger.getLogger(SyncManager.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
                        break;
                    } catch (IOException ex) {
                        Logger.getLogger(SyncManager.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
                        break;
                    }
                }
            }
            if (!sessions.isEmpty()) {
                try {
                    Logger.getLogger(SyncManager.class.getName()).log(Level.INFO, "Saving updates to database");
                    obsdb.save(sessions);
                    Logger.getLogger(SyncManager.class.getName()).log(Level.INFO, "Database Synchronised with " + host.getUrl());
                } catch (BookKeeprException ex) {
                    Logger.getLogger(SyncManager.class.getName()).log(Level.SEVERE, "Somehow tried to modify external elements from a database sync. This should never happen!", ex);
                }
            }
        }
    }
}
