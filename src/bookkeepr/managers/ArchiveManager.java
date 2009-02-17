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

import bookkeepr.xml.IdAble;
import bookkeepr.xml.StringConvertable;
import bookkeepr.xmlable.ArchivedStorage;
import bookkeepr.xmlable.ArchivedStorageIndex;
import bookkeepr.xmlable.ArchivedStorageWrite;
import bookkeepr.xmlable.ArchivedStorageWriteExtended;
import bookkeepr.xmlable.ArchivedStorageWriteIndex;
import bookkeepr.xmlable.DatabaseManager;
import bookkeepr.xmlable.Psrxml;
import bookkeepr.xmlable.PsrxmlIndex;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author kei041
 */
public class ArchiveManager implements ChangeListener {

    private DatabaseManager dbMan;
    private HashMap<String, ArchivedStorage> label2Storage = new HashMap<String, ArchivedStorage>();
    private HashMap<Long, ArchivedStorageIndex> psrxml2Storage = new HashMap<Long, ArchivedStorageIndex>();
    private HashMap<Long, PsrxmlIndex> storage2Psrxml = new HashMap<Long, PsrxmlIndex>();
    private HashMap<Long, ArchivedStorageWriteIndex> storage2Writes = new HashMap<Long, ArchivedStorageWriteIndex>();
    private HashMap<String, ArchivedStorageWrite> uniqueWrites = new HashMap<String, ArchivedStorageWrite>();
    private ArrayList<ArchivedStorageWrite> orphanedWrites = new ArrayList<ArchivedStorageWrite>();

    public ArchiveManager(DatabaseManager dbman) {
        this.dbMan = dbman;
        this.dbMan.addListener(TypeIdManager.getTypeFromClass(ArchivedStorage.class), this);
        this.dbMan.addListener(TypeIdManager.getTypeFromClass(ArchivedStorageWrite.class), this);
        this.dbMan.addListener(TypeIdManager.getTypeFromClass(Psrxml.class), this);
    }

    public ArchivedStorage getStorageByLabel(String label) {
        return this.label2Storage.get(label);
    }

    public ArchivedStorageIndex getStorageForPsrxmlId(long id) {
        return this.psrxml2Storage.get(id);
    }

    public PsrxmlIndex getPsrxmlForStorageId(long id) {
        return this.storage2Psrxml.get(id);
    }

    public ArchivedStorageWriteIndex getArchivedWriteExtendedIndexForStorageId(long id) {
        //Psrxml xml = this.storage2Psrxml.get(id);

        ArchivedStorageWriteIndex oIdx = this.storage2Writes.get(id);

        ArchivedStorageWriteIndex eIdx = new ArchivedStorageWriteIndex();

        for (ArchivedStorageWrite write : oIdx.getArchivedStorageWriteList()) {
            ArchivedStorageWriteExtended extended = new ArchivedStorageWriteExtended();
            extended.setId(write.getId());
            extended.setDateWritten(write.getDateWritten());
            extended.setFileLabel(write.getFileLabel());
            extended.setPsrxmlId(write.getPsrxmlId());
            extended.setStorageId(write.getStorageId());
            extended.setWriteSize(write.getWriteSize());
            extended.addArchivedStorage((ArchivedStorage) this.dbMan.getById(write.getStorageId()));
            extended.addPsrxml((Psrxml) this.dbMan.getById(write.getPsrxmlId()));
            eIdx.addArchivedStorageWrite(extended);
        }

        return eIdx;
    }

    public ArchivedStorageWrite checkUnique(ArchivedStorageWrite write) {
        String key = write.getStorageId() + write.getFileLabel(); // Nothing can have the same storage id and label!

        return uniqueWrites.get(key);
    }

    public void itemUpdated(DatabaseManager dbMan, IdAble item, boolean remoteChange, boolean isModified) {
        if (item instanceof ArchivedStorage) {
            ArchivedStorage storage = (ArchivedStorage) item;
            this.label2Storage.put(storage.getMediaLabel(), storage);
            synchronized (this.orphanedWrites) {
                Iterator<ArchivedStorageWrite> itr = this.orphanedWrites.iterator();
                while (itr.hasNext()) {
                    ArchivedStorageWrite w = itr.next();
                    if (w.getStorageId() == storage.getId()) {
                        if (makeLink(w)) {
                            itr.remove();
                        }
                    }

                }
            }
        } else if (item instanceof ArchivedStorageWrite) {
            ArchivedStorageWrite write = (ArchivedStorageWrite) item;
            String key = write.getStorageId() + write.getFileLabel();
            if (checkUnique(write) == null) {
                uniqueWrites.put(key, write);
            }
            if (!makeLink(write)) {
                synchronized (this.orphanedWrites) {
                    orphanedWrites.add(write);
                }
            }

            ArchivedStorageWriteIndex idx = this.storage2Writes.get(write.getStorageId());
            if (idx == null) {
                idx = new ArchivedStorageWriteIndex();
                this.storage2Writes.put(write.getStorageId(), idx);
            }
            idx.addArchivedStorageWrite(write);
        } else if (item instanceof Psrxml) {
            Psrxml psrxml = (Psrxml) item;
            synchronized (this.orphanedWrites) {
                Iterator<ArchivedStorageWrite> itr = this.orphanedWrites.iterator();
                while (itr.hasNext()) {
                    ArchivedStorageWrite w = itr.next();
                    if (w.getPsrxmlId() == psrxml.getId()) {
                        if (makeLink(w)) {
                            itr.remove();
                        }
                    }

                }
            }
        }


    }

    private boolean makeLink(ArchivedStorageWrite write) {
        synchronized (this.orphanedWrites) {
            Psrxml psrxml = (Psrxml) dbMan.getById(write.getPsrxmlId());
            if (psrxml == null) {
//                this.orphanedWrites.add(write);
                return false;

            }


            ArchivedStorage storage = (ArchivedStorage) dbMan.getById(write.getStorageId());
            if (storage == null) {
//                this.orphanedWrites.add(write);
                return false;

            }


            ArchivedStorageIndex stList = this.psrxml2Storage.get(psrxml.getId());
            if (stList == null) {
                stList = new ArchivedStorageIndex();
                this.psrxml2Storage.put(psrxml.getId(), stList);
            }

            stList.addArchivedStorage(storage);

            PsrxmlIndex xmlList = this.storage2Psrxml.get(storage.getId());
            if (xmlList == null) {
                xmlList = new PsrxmlIndex();
                this.storage2Psrxml.put(storage.getId(), xmlList);
            }

            xmlList.addPsrxml(psrxml);
            return true;
        }
    }
}
