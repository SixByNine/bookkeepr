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


import bookkeepr.xml.IdAble;
import bookkeepr.xmlable.DatabaseManager;
import bookkeepr.xmlable.Session;

/**
 *
 * @author kei041
 */
public class SessionManager implements ChangeListener {

    long[] latestIds = new long[256];

    public SessionManager(DatabaseManager dbman) {
        for (int i = 0; i < latestIds.length; i++) {
            IdAble latestSession = dbman.getLatest(i, TypeIdManager.getTypeFromClass(Session.class));

            if (latestSession == null) {
                latestIds[i] = dbman.makeId(0, i, TypeIdManager.getTypeFromClass(Session.class)) - 1;
            } else {
                latestIds[i] = latestSession.getId();
            }
        }

        dbman.addListener(TypeIdManager.getTypeFromClass(Session.class), this);
    }

    public void itemUpdated(DatabaseManager dbMan, IdAble item, boolean remoteChange, boolean modified) {
        if (!modified) {
            Session session = (Session) item;
            int origin = dbMan.getOrigin(item);
            if (session.getId() > latestIds[origin]) {
                latestIds[origin] = session.getId();
            }
        }
    }

    public long getNextId(int server) {
        return latestIds[server] + 1;
    }
}
