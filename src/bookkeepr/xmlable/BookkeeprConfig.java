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

import bookkeepr.xml.StringConvertable;
import bookkeepr.xml.XMLAble;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author kei041
 */
public class BookkeeprConfig implements XMLAble {

    private String externalUrl = "http://localhost:8080";
    private int port = 8080;
    private String rootPath = "./bookkeepr.db";
    private ArrayList<BookkeeprHost> knownHosts = new ArrayList<BookkeeprHost>();
    private int originId = 0;
    private int syncTime = 600;
    private HashMap<Integer, BookkeeprHost> knownHostMap = new HashMap<Integer, BookkeeprHost>();
    private String proxyUrl = null;
    private int proxyPort = 0;

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    public void addBookkeeprHost(BookkeeprHost host) {
        this.knownHosts.add(host);
        if (host.getOriginId() != 0) {
            this.knownHostMap.remove(host.getOriginId());
            this.knownHostMap.put(host.getOriginId(), host);
        }
    }

    public List<BookkeeprHost> getBookkeeprHostList() {
        return this.knownHosts;
    }

    public int getOriginId() {
        return originId;
    }

    public void setOriginId(int originId) {
        this.originId = originId;
    }

    public int getSyncTime() {
        return syncTime;
    }

    public void setSyncTime(int syncTime) {
        this.syncTime = syncTime;
    }

    public BookkeeprHost getBookkeeprHost(int origin) {
        return this.knownHostMap.get(origin);
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
        xmlParameters.put("ExternalUrl", StringConvertable.STRING);
        xmlParameters.put("Port", StringConvertable.INT);
        xmlParameters.put("OriginId", StringConvertable.INT);
        xmlParameters.put("SyncTime", StringConvertable.INT);
        xmlParameters.put("RootPath", StringConvertable.STRING);
        xmlParameters.put("ProxyPort", StringConvertable.INT);
        xmlParameters.put("ProxyUrl", StringConvertable.STRING);
        xmlSubObjects.add("BookkeeprHost");

    }
}
