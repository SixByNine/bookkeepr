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

import bookkeepr.xmlable.Log;
import bookkeepr.xmlable.LogItem;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * @author kei041
 */
public class BookKeeprStatusMonitor extends java.util.logging.Handler {

    private final DateFormat logdateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK);
    private PrintStream out;
    private LogRecord latestError;
    private int nWarn = 0;
    private int nErr = 0;
    HashMap<Level, ArrayBlockingQueue<LogRecord>> records = new HashMap<Level, ArrayBlockingQueue<LogRecord>>();
    private static Level[] storedLevels = new Level[]{Level.CONFIG, Level.INFO, Level.WARNING, Level.SEVERE};

    public enum BookKeeprStatus {

        OK, WARNING, ERROR
    };
    private BookKeeprStatus status = BookKeeprStatus.OK;

    public BookKeeprStatusMonitor(PrintStream out) {
        this.out = out;
        for (Level l : storedLevels) {
            records.put(l, new ArrayBlockingQueue<LogRecord>(10));
        }
    }

    public BookKeeprStatusMonitor() {
        this(System.err);
    }

    public BookKeeprStatus getStatus() {
        return status;
    }

    public void clearStatus() {
        this.status = BookKeeprStatus.OK;
        this.nWarn = 0;
        this.nErr = 0;
    }

    public int getNErr() {
        return nErr;
    }

    public void setNErr(int nErr) {
        this.nErr = nErr;
    }

    public int getNWarn() {
        return nWarn;
    }

    public void setNWarn(int nWarn) {
        this.nWarn = nWarn;
    }

    @Override
    public void close() throws SecurityException {
    }

    @Override
    public void flush() {
        System.err.flush();
    }

    @Override
    public void publish(LogRecord record) {
        appendToLog(record);
        if (this.getLevel() == Level.WARNING) {
            nWarn++;
        }
        if (record.getLevel() == Level.WARNING && this.status != BookKeeprStatus.ERROR) {
            this.status = BookKeeprStatus.WARNING;
            this.latestError = record;
        }
        if (record.getLevel() == Level.SEVERE) {
            this.status = BookKeeprStatus.ERROR;
            this.latestError = record;
            nErr++;
        }

        Date date = new Date();

        out.printf("[%s] %s.%s\n%s:\t%s\n", logdateFormat.format(date), record.getSourceClassName(), record.getSourceMethodName(), record.getLevel().toString(), record.getMessage());
        if (record.getLevel() == Level.SEVERE && record.getThrown() != null) {
            out.println("\tCaused by: ("+record.getThrown().getClass()+") " + record.getThrown().getLocalizedMessage());
            record.getThrown().printStackTrace(System.err);
        } else if (record.getThrown() != null) {
            out.println("\tCaused by: ("+record.getThrown().getClass()+") " + record.getThrown().getLocalizedMessage());
        }
    }

    public Log getLog() {
        Comparator<LogRecord> comp = new Comparator<LogRecord>() {

            public int compare(LogRecord o1, LogRecord o2) {
                long v = o2.getMillis() - o1.getMillis();
                if (v < 0) {
                    return -1;
                }
                if (v > 0) {
                    return 1;
                }
                return 0;
            }
        };

        ArrayList<LogRecord> allRecords = new ArrayList<LogRecord>();
        for (Level l : storedLevels) {
            allRecords.addAll(records.get(l));
        }
        Collections.sort(allRecords, comp);

        Log log = new Log();
        for (LogRecord r : allRecords) {
            LogItem item = new LogItem();
            item.setType(r.getLevel().toString());
            item.setMessage(r.getMessage());
            Date date = new Date(r.getMillis());
            item.setDate(logdateFormat.format(date));
            item.setOrigin(r.getSourceClassName() + "." + r.getSourceMethodName());
            log.addLogItem(item);
        }
        Date date = new Date();
        log.setCurrentTime(logdateFormat.format(date));

        return log;
    }

    private synchronized void appendToLog(LogRecord record) {
        ArrayBlockingQueue<LogRecord> list = this.records.get(record.getLevel());
        if (list != null) {
            while (!list.offer(record)) {
                list.poll();
            }
        }
    }
}
