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
package bookkeepr.xmlable;

import bookkeepr.BookKeepr;
import bookkeepr.BookKeeprException;

import bookkeepr.xmlable.DeletedId;
import bookkeepr.managers.ChangeListener;
import bookkeepr.managers.TypeIdManager;
import bookkeepr.xml.IdAble;
import bookkeepr.xml.StringConvertable;
import bookkeepr.xml.XMLAble;
import bookkeepr.xml.XMLReader;
import bookkeepr.xml.XMLWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.swing.text.DateFormatter;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.SAXException;

/**
 *
 * @author kei041
 */
public class DatabaseManager implements XMLAble {

    private HashMap<String, Index> indicies = new HashMap<String, Index>();
    private File rootPath = new File("./bookkeepr.db/");
    private String externalUrl;
    private long[] nextId = new long[256];
    private long[][] latestIds = new long[256][256];
    private ArrayList[] listeners = new ArrayList[256];
    private int originId = 0x00;
    private int maxOriginId = 0;
    private BookKeepr bookkeepr;

    public DatabaseManager() {

        Arrays.fill(nextId, 0x1L);
        for (int i = 0; i < latestIds.length; i++) {
            Arrays.fill(latestIds[i], 0x0L);
        }
        for (int i = 0; i < listeners.length; i++) {
            listeners[i] = new ArrayList();
        }
    }

    public int getMaxOriginId() {
        return maxOriginId;
    }

    public void setMaxOriginId(int maxOriginId) {
        this.maxOriginId = maxOriginId;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public long[] getNextId() {
        return nextId;
    }

    public void setNextId(long[] nextId) {
        this.nextId = nextId;
    }

    public int getOriginId() {
        return originId;
    }

    public void setOriginId(int originId) {
        this.originId = originId;
    }

    public File getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = new File(rootPath);
    }

    public BookKeepr getBookkeepr() {
        return bookkeepr;
    }

    public void setBookkeepr(BookKeepr bookkeepr) {
        this.bookkeepr = bookkeepr;
        
    }

    public synchronized void remove(IdAble item, Session session) {
        this.add(new DeletedId(item.getId()), session);
    }

    /**
     * Adds an item to the provided session. Session will be re-set if provided in a 'closed' state.
     * If a blank session (i.e. with id=0) is used, a new relevent Id will be set.
     * 
     * @param item The idable item to add.
     * @param session The session to used. Should be 'open' but it will be opened if closed
     */
    public synchronized void add(IdAble item, Session session) {
        int type;
        // open the session if it is closed.
        if (session.getClosed()) {
            session.setClosed(false);
        }
        if (session.getId() == 0L) {
            if (this.originId == 0) {
                throw new RuntimeException("Bookkeepr with origin id 0 cannot create items!");
            }
            type = TypeIdManager.getTypeFromClass(Session.class);
            session.setId(makeId(nextId[type], originId, type));
            nextId[type]++;
        }

        if (this.getById(item.getId()) != null) {
            // we would be replacing an existing item!
            if (this.getOrigin(session) == this.getOrigin(item)) {
                // the origninator of the session owned the item...
                // therefore we can go ahead
                session.addModifiedItem(item);
                return;
            } else {
                // we (or someone else) are trying to modify an external item.
                // this is forbidden, so we add it to the external items section.
                // This will allow the server to process it when it comes time to save.
                session.addExternalItem(item);
                return;
            }

        }


        if (getOrigin(session) == this.originId && item.getId() == 0) {
            if (this.originId == 0) {
                throw new RuntimeException("Bookkeepr with origin id 0 cannot create items!");
            }
            // if we created this session, then we should be setting ids.
            type = TypeIdManager.getTypeFromClass(item.getClass());
            item.setId(makeId(nextId[type], originId, type));
            nextId[type]++;
        }

        session.addModifiedItem(item);
    }

    /*
     * Ids:
     *  1 2 3 4 5 6 7 8
     * 0CCTTIIIIIIIIIII
     * 
     * 0 = reserved. Always set to 0. (Currently will break if above 15!
     * C = origin
     * T = Type
     * I = Id
     * 
     */
    public long makeId(long nextId, int origin, int type) {
        //                                   0CCTTIIIIIIIIIII                        0CCTTIIIIIIIIIII
        return nextId + (((long) origin) * 0x0010000000000000L) + (((long) type) * 0x0000100000000000L);

    }

    public int getType(IdAble item) {
        long v = item.getId() & 0x000FF00000000000L;
        return (int) (v / 0x0000100000000000L);
    }

    public int getOrigin(IdAble item) {
        long v = item.getId() & 0x0FF0000000000000L;
        return (int) (v / 0x0010000000000000L);
    }

    private String getKey(long index) {
        long key = index / 0x1000L;
        if (index < 0) {
            return "negative";
        }

        //             1 2 3 4 5 6 7 8
        if (index < 0x0000100000000000L) {
            return "00000" + Long.toHexString(key);
        }
        //             1 2 3 4 5 6 7 8
        if (index < 0x0001000000000000L) {
            return "0000" + Long.toHexString(key);
        }
        if (index < 0x0010000000000000L) {
            return "000" + Long.toHexString(key);
        }
        if (index < 0x0100000000000000L) {
            return "00" + Long.toHexString(key);
        }
        if (index < 0x1000000000000000L) {
            return "0" + Long.toHexString(key);
        }
        return Long.toHexString(key);
    }

    public synchronized void restore() {
        Logger.getLogger(DatabaseManager.class.getName()).log(Level.INFO, "Restoring from " + rootPath.getAbsolutePath());


        long[] count = new long[256];
        Arrays.fill(count, 0);

        File[] dirList = rootPath.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(".xml.gz");
            }
        });

        Arrays.sort(dirList, new Comparator<File>() {

            public int compare(File o1, File o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
            }
        });

        for (File f : dirList) {
            try {
                Index<IdAble> idx = (Index<IdAble>) XMLReader.read(new GZIPInputStream(new FileInputStream(f)));
                List<IdAble> list = idx.getIndex();
                if (idx.getIndex().size() < 1) {
                    continue;// it's an empty list!

                }
                String key = getKey(list.get(0).getId());
                IdAble last = list.get(list.size() - 1);
                int type = getType(last);
                int origin = getOrigin(last);
                if (origin > this.maxOriginId) {
                    this.maxOriginId = origin;
                }
                if (last.getId() > latestIds[origin][type]) {
                    latestIds[origin][type] = last.getId();
                }
                this.indicies.put(key, idx);

                count[this.getType(list.get(0))] += idx.getSize();

                // notify listeners.
                for (IdAble item : idx.getIndex()) {

                    for (Object listener : listeners[this.getType(item)]) {
                        ((ChangeListener) listener).itemUpdated(this, item, this.getOrigin(item) != this.originId, false);
                    }
                }
            } catch (SAXException ex) {
                Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
            }


        }

        for (int type = 0; type < 256; type++) {
            long latest = latestIds[this.getOriginId()][type] & 0x00000FFFFFFFFFFFL;

            if (latest >= this.nextId[type]) {
                this.nextId[type] = latest + 1;
                Logger.getLogger(DatabaseManager.class.getName()).log(Level.WARNING, "Latest index for type " + Integer.toHexString(type) + " is later than our next ID... Updating next ID to avoid database conflicts (latest is: " + Long.toHexString(latest) + ")");
            }
        }

        Logger.getLogger(DatabaseManager.class.getName()).log(Level.INFO, "Loaded " + this.indicies.size() + " indexes");
        for (int t = 0; t < count.length; t++) {
            if (count[t] > 0) {
                Logger.getLogger(DatabaseManager.class.getName()).log(Level.INFO, "Loaded " + count[t] + " items of type " + Integer.toHexString(t));
            }
        }

    }

    public synchronized void save() throws BookKeeprException {
        save((List<Session>) null);
    }

    public synchronized void save(Session session) throws BookKeeprException {
        if (session == null) {
            save();
        } else {
            ArrayList<Session> list = new ArrayList<Session>();
            list.add(session);
            save(list);
        }
    }

    public synchronized void save(List<Session> sessions) throws BookKeeprException {
        if (!rootPath.exists()) {
            rootPath.mkdirs();
        }

        if (sessions == null) {
            Logger.getLogger(DatabaseManager.class.getName()).log(Level.FINE, "Saving null session");
            try {
                XMLWriter.write(new FileOutputStream(rootPath.getPath() + File.separator + "_index.xml"), this);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
                throw new BookKeeprException(ex);
            }
            return;
        }


        ArrayList<IdAble> externals = new ArrayList<IdAble>();
        for (Session session : sessions) {
            externals.addAll(session.getExternalItems());
        }
        if (externals.size() > 0) {
            this.saveExternals(externals);
        }


        ArrayList<String> keys = new ArrayList<String>();



        HashMap<Long, Boolean> isModified = new HashMap<Long, Boolean>();

        for (Session session : sessions) {
            if (session.getModified().size() == 0) {
                // don't bother saving the session if it's empty.
                // It's probably just dealing with externals.
                continue;
            }

            // add the session to the list of things to write.
            session.addModifiedItem(session);

            //@todo: We really want to write files then add, but that might require some more work.
            // Then add all the new items.
            for (IdAble item : session.getModified()) {

                Index index = indicies.get(getKey(item.getId()));
                if (index == null) {
                    if (!(item instanceof DeletedId)) {
                        index = TypeIdManager.getIndexFromClass(item.getClass());
                        String key = getKey(item.getId());
                        if (indicies.get(key) != null) {
                            indicies.remove(key);

                        }
                        indicies.put(key, index);
                    }
                }

                if (item instanceof DeletedId) {
                    if (index != null) {
                        // if we are deleting, and the index exists, we just remove the item!
                        IdAble deleted = index.getItem(item.getId());

                        if (deleted != null) {
                            index.remove(item);
                            isModified.put(item.getId(), true);
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-DD");
                            String datestr = format.format(new Date());
                            // save the item to the trash
                            File trashPath = new File(rootPath.getPath() + File.separator + "trash" + File.separator + datestr);
                            if (!trashPath.exists()) {
                                trashPath.mkdirs();
                            }
                            GZIPOutputStream out;
                            try {
                                out = new GZIPOutputStream(new FileOutputStream(trashPath + File.separator + StringConvertable.ID.toString(item.getId()) + ".xml.gz"));
                                XMLWriter.write(out, deleted);
                                out.close();
                            } catch (IOException ex) {
                                Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, "Error saving deleted item!", ex);
                                throw new BookKeeprException(ex);
                            }
                        }
                    }
                } else {
                    // otherwise we will have definitely created the index so we should add it.
                    isModified.put(item.getId(), index.addItemIsExisting(item));
                    int type = getType(item);
                    int origin = getOrigin(item);
                    if (origin > this.maxOriginId) {
                        this.maxOriginId = origin;
                    }
                    if (item.getId() > latestIds[origin][type]) {
                        latestIds[origin][type] = item.getId();
                    }
                }
            }




            for (IdAble item : session.getModified()) {
                long id = item.getId();
                String key = getKey(id);
                if (!keys.contains(key)) {
                    keys.add(key);
                }
            }
        }
        HashMap<String, File> files = new HashMap<String, File>();
        for (String key : keys) {
            OutputStream out = null;
            File tmpfile = null;
            try {
                tmpfile = File.createTempFile("db" + key, "wrk.gz", rootPath);
                tmpfile.deleteOnExit();
                out = new GZIPOutputStream(new FileOutputStream(tmpfile));
                XMLWriter.write(out, indicies.get(key));
                files.put(rootPath.getPath() + File.separator + key + ".xml.gz", tmpfile);
            } catch (IOException ex) {
                Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
                throw new BookKeeprException(ex);
            } finally {
                try {
                    out.close();
                } catch (IOException ex) {
                    Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
                    throw new BookKeeprException(ex);
                }
            }
        }
        for (String fname : files.keySet()) {
            files.get(fname).renameTo(new File(fname));
        }







        try {
            XMLWriter.write(new FileOutputStream(rootPath.getPath() + File.separator + "_index.xml"), this);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DatabaseManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (Session session : sessions) {

            // notify listeners.
            for (IdAble item : session.getModified()) {



                for (Object listener : listeners[this.getType(item)]) {
//                    Logger.getLogger(DatabaseManager.class.getName()).log(Level.FINE, "Notifying " + ((ChangeListener) listener) + " about " + item);
                    if (item instanceof DeletedId) {
                        ((ChangeListener) listener).itemUpdated(this, item, this.getOrigin(item) != this.originId, true);
                    } else {
                        ((ChangeListener) listener).itemUpdated(this, item, this.getOrigin(item) != this.originId, isModified.get(item.getId()));
                    }
                }

            }

            // then close the session
            session.setClosed(true);
        }
    }

    private synchronized void saveExternals(List<IdAble> items) throws RemoteDatabaseNotContactableException {
        List[] itemsPerServer = new ArrayList[256];
        for (int i = 0; i <
                itemsPerServer.length; i++) {
            itemsPerServer[i] = new ArrayList();
        }

        for (IdAble item : items) {
            itemsPerServer[this.getOrigin(item)].add(item);
        }

        for (int i = 0; i <
                itemsPerServer.length; i++) {

            if (!itemsPerServer[i].isEmpty()) {
                // we need to get in touch with the server...
                BookkeeprHost host = null;
                for (BookkeeprHost testHost : bookkeepr.getConfig().getBookkeeprHostList()) {
                    if (testHost.getOriginId() == i) {
                        host = testHost;
                        break;
                    }
                }
                if (host != null) {
                    try {
                        // found the host
                        Logger.getLogger(DatabaseManager.class.getName()).log(Level.FINE, "Need to modify " + itemsPerServer[i].size() + " items in external server " + i);
                        Index[] arr = new Index[256];

                        Arrays.fill(arr, null);
                        for (Object o : itemsPerServer[i]) {
                            IdAble item = (IdAble) o;
                            int type = getType(item);
                            if (arr[type] == null) {
                                arr[type] = TypeIdManager.getIndexFromClass(item.getClass());
                            }
                            arr[type].addItem(item);
                        }
                        IndexIndex result = new IndexIndex();
                        for (Index idx : arr) {
                            if (idx == null) {
                                continue;
                            } else {
                                result.addIndex(idx);
                            }
                        }
                        HttpPost httppost = new HttpPost(host.getUrl() + "/insert/");
                        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

                        XMLWriter.write(out, result);
                        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

                        httppost.setEntity(new InputStreamEntity(in,
                                -1));
                        Logger.getLogger(DatabaseManager.class.getName()).log(Level.INFO, "Database posting to " + host.getUrl() + "/insert/ to modify items");
                        HttpClient httpclient = bookkeepr.checkoutHttpClient();
                        HttpResponse httpresp = httpclient.execute(httppost);
                        int statuscode = httpresp.getStatusLine().getStatusCode();


                        bookkeepr.returnHttpClient(httpclient);
                        httpclient=null;
                        httpresp=null;



                        if (statuscode == 200) {
                            // ok, the remote database is updated... force a sync now.
                            } else {
                            Logger.getLogger(DatabaseManager.class.getName()).log(Level.WARNING, "Could not modify external elements. Could not contact master server");
                            throw new RemoteDatabaseNotContactableException("Got a status code '" + statuscode + "' from the external server, was expecting a '200 OK'");
                        }
                        break;
                    } catch (HttpException ex) {
                        Logger.getLogger(DatabaseManager.class.getName()).log(Level.WARNING, "Could not modify external elements. Could not contact master server", ex);
                        throw new RemoteDatabaseNotContactableException("External server was not comunicatable");
                    } catch (IOException ex) {
                        Logger.getLogger(DatabaseManager.class.getName()).log(Level.WARNING, "Could not modify external elements. Could not contact master server", ex);
                        throw new RemoteDatabaseNotContactableException("External server was not comunicatable");
                    } catch (URISyntaxException ex) {
                        Logger.getLogger(DatabaseManager.class.getName()).log(Level.WARNING, "Could not modify external elements. Could not contact master server", ex);
                        throw new RemoteDatabaseNotContactableException("External server was not configured correctly!");
                    }
                } else {
                    // At some point we should try some other servers to see if they have heard of the server we want.

                    Logger.getLogger(DatabaseManager.class.getName()).log(Level.WARNING, "Could not modify external elements. I don't know who the master server is");
                    throw new RemoteDatabaseNotContactableException("External server was not avaliable");

                }
            }

        }
    }

    public IdAble getById(
            long id) {
        String key = getKey(id);
        Index idx = indicies.get(key);
        if (idx == null) {
            return null;
        }

        return idx.getItem(id);
    }

    public List<IdAble> getAllOfType(int type) {
        ArrayList<IdAble> res = new ArrayList<IdAble>();
        for (int orig = 0; orig <=
                this.maxOriginId; orig++) {
            for (long id = makeId(0, orig, type); id <=
                    latestIds[orig][type]; id++) {
                IdAble item = this.getById(id);
                if (item != null) {
                    res.add(item);
                }

            }
        }
        return res;
    }

    public IdAble getLatest(
            int origin, int type) {
        return getById(latestIds[origin][type]);
    }

    public IndexIndex getFromSession(
            Session session) {
        if (session.getClosed()) {
            Index[] arr = new Index[256];
            Arrays.fill(arr, null);
            for (long id : session.getModifiedKey()) {
                IdAble item = this.getById(id);
                if (item == null) {
                    Logger.getLogger(DatabaseManager.class.getName()).log(Level.INFO, "Item " + Long.toHexString(id) + " Not found in correct container, assuming deleted.");
                    item = new DeletedId(id);
                }
                int type = getType(item);
                if (arr[type] == null) {
                    arr[type] = TypeIdManager.getIndexFromClass(item.getClass());
                }

                arr[type].addItem(item);
            }

            IndexIndex result = new IndexIndex();
            for (Index idx : arr) {
                if (idx == null) {
                    continue;
                } else {
                    result.addIndex(idx);
                }

            }

            return result;
        } else {
            return null;
        }

    }

    public void addListener(int type, ChangeListener listener) {
        listeners[type].add(listener);
    }

    public String getClassName() {
        return this.getClass().getSimpleName();
    }

    public HashMap<String, StringConvertable> getXmlParameters() {
        return xmlParameters;
    }

    public List<String> getXmlSubObjects() {
        return xmlSubObjects;
    }
    private static HashMap<String, StringConvertable> xmlParameters = new HashMap<String, StringConvertable>();
    private static ArrayList<String> xmlSubObjects = new ArrayList<String>();
    

    static {

        xmlParameters.put("RootPath", StringConvertable.STRING);
        xmlParameters.put("NextId", StringConvertable.IDARRAY);
        xmlParameters.put("OriginId", StringConvertable.INT);
    }

    public class RemoteDatabaseNotContactableException extends BookKeeprException {

        public RemoteDatabaseNotContactableException() {
        }

        public RemoteDatabaseNotContactableException(String message) {
            super(message);
        }

        public RemoteDatabaseNotContactableException(String message, Throwable cause) {
            super(message, cause);
        }

        public RemoteDatabaseNotContactableException(Throwable cause) {
            super(cause);
        }
    }
}
