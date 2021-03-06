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
package bookkeepr;

import bookkeepr.jettyhandlers.ArchivedStorageHandler;
import bookkeepr.jettyhandlers.BackgroundTaskHandler;
import bookkeepr.jettyhandlers.CandidateHandler;
import bookkeepr.jettyhandlers.FetchHandler;
import bookkeepr.jettyhandlers.ObservationHandler;
import bookkeepr.jettyhandlers.SystemHandler;
import bookkeepr.jettyhandlers.WebHandler;
import bookkeepr.managers.ArchiveManager;
import bookkeepr.managers.CandidateManager;
import bookkeepr.managers.ObservationManager;
import bookkeepr.managers.SessionManager;
import bookkeepr.managers.SyncManager;
import bookkeepr.xml.XMLReader;
import bookkeepr.xml.XMLWriter;
import bookkeepr.xmlable.BookkeeprConfig;
import bookkeepr.xmlable.BookkeeprHost;
import bookkeepr.xmlable.DatabaseManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.thread.QueuedThreadPool;
import org.xml.sax.SAXException;

/**
 *
 * The master bookkeepr class.
 * 
 * Inistantiate it, set any parameters, call init() to configure and 
 * then start() to kick things off.
 * 
 * @author Michael Keith
 */
public class BookKeepr {

    private static final int version = 3;
    /*
     * The managers, that look after the data
     */
    DatabaseManager masterDatabaseManager; // this is the core database engine

    SessionManager sessionManager; // monitors the latest sessions from all servers

    SyncManager syncManager; // fetches data from other servers to keep in sync

    ObservationManager obsManager;
    ArchiveManager archiveManager;
    CandidateManager candManager;
    /*
     * The Jetty handlers that deal with incoming requests 
     */
    BackgroundTaskHandler bgHandler; // provides status on background tasks

    SystemHandler sysHandler; // provides interfaces to core system properties

    FetchHandler fetchHandler; // provides data on the system, and elements in the db

    ObservationHandler obsHandler; // provides advanced interfaces to the observation db.

    ArchivedStorageHandler archiveHandler; // provides interfaces for data archives (tapes).

    CandidateHandler candHandler; // provides advanced interfaces to the candidate db.

    WebHandler webHandler;
    BookKeeprStatusMonitor statusMon; // monitors the health of the server.

    /*
     * Background task runner...
     * This is a thread that pulls jobs of a queue to do tasks in serial even
     * if they are submitted in paralell. Helps to prevent database overloading
     * Tasks that are run with the bgrunner also won't interfere with each other.
     */
    BackgroundTaskRunner bgrunner = new BackgroundTaskRunner();
    Server server; // the Jetty Server

    BookkeeprConfig config; // our configuration file

    File configFile; // the configuration file File object.

    private Queue<HttpClient> httpClients = new ArrayBlockingQueue<HttpClient>(20);

    /**
     * This loads the configuration file and sets the intial settings. 
     */
    public BookKeepr(File configFile) {

        try {
            this.configFile = configFile;
            if (!configFile.exists()) {
                config = new BookkeeprConfig();
                config.setOriginId(0);
                saveConfig();
            }
            config = (BookkeeprConfig) XMLReader.read(new FileInputStream(configFile));
            if (config.getOriginId() < 0 || config.getOriginId() > 255) {
                config.setOriginId(0);

            }
            if (config.getOriginId() == 0) {
                Logger.getLogger(BookKeepr.class.getName()).log(Level.INFO, "Client mode active, creation or modification disabled");
            }


            statusMon = new BookKeeprStatusMonitor();
            Logger logger = Logger.getLogger("bookkeepr");
            logger.setLevel(Level.ALL);
            logger.setUseParentHandlers(false);
            logger.addHandler(statusMon);
        } catch (SAXException ex) {
            Logger.getLogger(BookKeepr.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BookKeepr.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (int i = 0; i < 20; i++) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpClientParams.setRedirecting(httpclient.getParams(), false);
            HttpConnectionParams.setConnectionTimeout(httpclient.getParams(), 10000);
            if (config.getProxyUrl() != null) {
                final HttpHost proxy =
                        new HttpHost(config.getProxyUrl(), config.getProxyPort(), "http");
                httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            }
            httpClients.add(httpclient);
        }

    }

    /**
     * Initialises the various managers and such.
     * Loads the database from the disk if it exists.
     * Configures, but does not start, the jetty server.
     * Once this is run, you should call start() to start the server.
     */
    public void init() {
        try {
            test();

            UpgradeChecker.pre_load(config.getRootPath(), this);

            saveConfig();


            File dbFile = new File(config.getRootPath() + File.separator + "_index.xml");
            if (dbFile.exists()) {
                Logger.getLogger(BookKeepr.class.getName()).log(Level.INFO, "Loading existing database");
                masterDatabaseManager = (DatabaseManager) XMLReader.read(new FileInputStream(dbFile));
                masterDatabaseManager.setOriginId(config.getOriginId());
                masterDatabaseManager.setRootPath(config.getRootPath());
            } else {
                Logger.getLogger(BookKeepr.class.getName()).log(Level.INFO, "Creating new database");
                masterDatabaseManager = new DatabaseManager();
                masterDatabaseManager.setOriginId(config.getOriginId());
                masterDatabaseManager.setRootPath(config.getRootPath());
                try {
                    masterDatabaseManager.save();
                } catch (BookKeeprException ex) {
                    Logger.getLogger(BookKeepr.class.getName()).log(Level.WARNING, null, ex);
                }

            }
            masterDatabaseManager.setBookkeepr(this);
            masterDatabaseManager.setExternalUrl(config.getExternalUrl());

            sessionManager = new SessionManager(masterDatabaseManager);
            syncManager = new SyncManager(masterDatabaseManager, config, sessionManager, bgrunner);
            obsManager = new ObservationManager(masterDatabaseManager);
            archiveManager = new ArchiveManager(masterDatabaseManager);
            candManager = new CandidateManager("./cands/", masterDatabaseManager, bgrunner);

            sysHandler = new SystemHandler(this);
            obsHandler = new ObservationHandler(this, masterDatabaseManager, obsManager);
            candHandler = new CandidateHandler(candManager, masterDatabaseManager, this);
            archiveHandler = new ArchivedStorageHandler(this, archiveManager);
            webHandler = new WebHandler(this, "./web/");
            masterDatabaseManager.restore();



        } catch (SAXException ex) {
            Logger.getLogger(BookKeepr.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BookKeepr.class.getName()).log(Level.SEVERE, null, ex);
        }

        UpgradeChecker.post_load(config.getRootPath(), this);

        bgHandler = new BackgroundTaskHandler(bgrunner);
        fetchHandler = new FetchHandler(this);

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[]{sysHandler, fetchHandler, bgHandler, obsHandler, archiveHandler, candHandler, webHandler});




        server = new Server(config.getPort());

        final QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(100);
        threadPool.setMinThreads(10);
        server.setThreadPool(threadPool);
        server.setHandler(handlers);

    }

    public void saveConfig() {
        try {
            XMLWriter.write(new FileOutputStream(configFile), config); // update the config with new defaults

        } catch (FileNotFoundException ex) {
            Logger.getLogger(BookKeepr.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public BookkeeprHost getHost() {
        BookkeeprHost host = new BookkeeprHost();
        host.setUrl(this.config.getExternalUrl());
        host.setOriginId(this.config.getOriginId());
        if (this.masterDatabaseManager != null) {
            host.setMaxOriginId(this.masterDatabaseManager.getMaxOriginId());
        }
        host.setStatus(statusMon.getStatus().toString());
        host.setErrors(statusMon.getNErr());
        host.setWarnings(statusMon.getNWarn());
        host.setVersion(version);

        return host;
    }

    public DatabaseManager getMasterDatabaseManager() {
        return masterDatabaseManager;
    }

    public BookKeeprStatusMonitor getStatusMon() {
        return statusMon;
    }

    /**
     * Call init() first.
     * 
     * Starts the bookkeepr system.
     * 
     * Starts the jetty server, the background task runner and the 
     * database synchronisation thread.
     * 
     */
    public void start() {
        try {

            bgrunner.start();
            syncManager.run();
            server.start();
        //test();
        } catch (BindException ex) {
            System.err.println("\n\n\nSomething (probably another Bookkeepr) is already using the bookkeepr port.");
            System.exit(1);
        } catch (Exception ex) {
            Logger.getLogger(BookKeepr.class.getName()).log(Level.SEVERE, null, ex);
        }


        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                super.run();
                System.err.println("Shutting down the BookKeepr");
                BookKeepr.this.stop();
            }
        });
    }

    public void stop() {
        saveConfig();

        try {
            server.stop();
            syncManager.terminate();
            bgrunner.terminate();
        } catch (Exception ex) {
            Logger.getLogger(BookKeepr.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.bgrunner.terminate();
    }

    public BookkeeprConfig getConfig() {
        return config;
    }

    public void setConfig(BookkeeprConfig config) {
        this.config = config;
    }

    public static void main(String[] args) {


        String file = "bookkeepr.cfg.xml";
        if (args.length > 0) {
            file = args[0];
        }
        BookKeepr bk = new BookKeepr(new File(file));

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--addhost")) {
                BookkeeprHost host = new BookkeeprHost();
                host.setOriginId(-1);
                host.setUrl(args[++i]);
                bk.getConfig().addBookkeeprHost(host);
            }
        }

        bk.init();
        bk.start();

    }

    public BackgroundTaskRunner getBackgroundTaskRunner() {
        return bgrunner;
    }

    ArchiveManager getArchiveManager() {
        return archiveManager;
    }

    CandidateManager getCandManager() {
        return candManager;
    }

    ObservationManager getObsManager() {
        return obsManager;
    }

    SessionManager getSessionManager() {
        return sessionManager;
    }

    SyncManager getSyncManager() {
        return syncManager;
    }

    public HttpClient checkoutHttpClient() {

        HttpClient ret = null;
        while (ret == null) {
            synchronized (httpClients) {
                ret = httpClients.poll();
            }
            if (ret == null) {
                Logger.getLogger(BookKeepr.class.getName()).log(Level.WARNING, "Run out of http handlers in BookKeepr!");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                }
            }
        }
        return ret;
    }

    public void returnHttpClient(HttpClient client) {
        this.httpClients.offer(client);
    }

    private void test() {
    }
}
