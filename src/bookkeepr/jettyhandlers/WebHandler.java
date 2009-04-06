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
import bookkeepr.managers.TypeIdManager;
import bookkeepr.xml.BookKeeprHandler;
import bookkeepr.xml.IdAble;
import bookkeepr.xml.StringConvertable;
import bookkeepr.xml.XMLAble;
import bookkeepr.xml.XMLWriter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;

/**
 *
 * @author kei041
 */
public class WebHandler extends AbstractHandler {

    private static Pattern regex = Pattern.compile("/");
    private static Pattern badchar = Pattern.compile("\\.\\.");
    private BookKeepr bookkeepr;
    private String localroot;

    public WebHandler(BookKeepr bookkeepr, String localroot) {
        this.bookkeepr = bookkeepr;
        this.localroot = localroot;

    }

    public void handle(String path, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
        if (path.equals("/")) {
            response.sendRedirect("/web/");
        }

        HttpClient httpclient = null;
        if (path.startsWith("/web/xmlify")) {
            ((Request) request).setHandled(true);
            if (request.getMethod().equals("POST")) {
                try {
                    String remotePath = path.substring(11);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
                    XMLAble xmlable = null;
                    try {
                        xmlable = httpForm2XmlAble(reader.readLine());
                    } catch (BookKeeprException ex) {
                        response.sendError(400, "Server could not form xml from the form data you submitted. " + ex.getMessage());
                        return;
                    }
                    if (xmlable == null) {
                        response.sendError(500, "Server could not form xml from the form data you submitted. The server created a null value!");
                        return;

                    }
//                    XMLWriter.write(System.out, xmlable);
//                    if(true)return;


                    HttpPost httppost = new HttpPost(bookkeepr.getConfig().getExternalUrl() + remotePath);
                    httppost.getParams().setBooleanParameter("http.protocol.strict-transfer-encoding", false);

                    ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
                    XMLWriter.write(out, xmlable);
                    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
                    httppost.setEntity(new InputStreamEntity(in, -1));
                    Logger.getLogger(WebHandler.class.getName()).log(Level.INFO, "Xmlifier posting to " + bookkeepr.getConfig().getExternalUrl() + remotePath);

                    httpclient = bookkeepr.checkoutHttpClient();
                    HttpResponse httpresp = httpclient.execute(httppost);
                    for (Header head : httpresp.getAllHeaders()) {
                        if (head.getName().equalsIgnoreCase("transfer-encoding")) {
                            continue;
                        }
                        response.setHeader(head.getName(), head.getValue());
                    }
                    response.setStatus(httpresp.getStatusLine().getStatusCode());


                    httpresp.getEntity().writeTo(response.getOutputStream());

                } catch (HttpException ex) {
                    Logger.getLogger(WebHandler.class.getName()).log(Level.WARNING, "HttpException " + ex.getMessage(), ex);
                    response.sendError(500, ex.getMessage());

                } catch (URISyntaxException ex) {
                    Logger.getLogger(WebHandler.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
                    response.sendError(400, "Invalid target URI");
                } finally {
                    if (httpclient != null) {
                        bookkeepr.returnHttpClient(httpclient);
                    }
                }



            }
            return;
        }

        if (request.getMethod().equals("GET")) {
            if (path.startsWith("/web")) {
                ((Request) request).setHandled(true);

                if (badchar.matcher(path).matches()) {
                    response.sendError(400, "User Error");
                    return;
                }
                String localpath = path.substring(4);
                Logger.getLogger(WebHandler.class.getName()).log(Level.FINE, "Transmitting " + localroot + localpath);
                File targetFile = new File(localroot + localpath);
                if (targetFile.isDirectory()) {
                    if (path.endsWith("/")) {
                        targetFile = new File(localroot + localpath + "index.html");
                    } else {
                        response.sendRedirect(path + "/");
                        return;
                    }
                }
                if (targetFile.exists()) {
                    if (targetFile.getName().endsWith(".html") || targetFile.getName().endsWith(".xsl")) {
                        BufferedReader in = new BufferedReader(new FileReader(targetFile));
                        PrintStream out = null;
                        String hdr = request.getHeader("Accept-Encoding");
                        if (hdr != null && hdr.contains("gzip")) {
                            // if the host supports gzip encoding, gzip the output for quick transfer speed.
                            out = new PrintStream(new GZIPOutputStream(response.getOutputStream()));
                            response.setHeader("Content-Encoding", "gzip");
                        } else {
                            out = new PrintStream(response.getOutputStream());
                        }
                        String line = in.readLine();
                        while (line != null) {
                            if (line.trim().startsWith("%%%")) {
                                BufferedReader wrapper = new BufferedReader(new FileReader(localroot + "/wrap/" + line.trim().substring(3) + ".html"));
                                String line2 = wrapper.readLine();
                                while (line2 != null) {
                                    out.println(line2);
                                    line2 = wrapper.readLine();
                                }
                                wrapper.close();
                            } else if (line.trim().startsWith("***chooser")) {
                                String[] elems = line.trim().split("\\s");
                                try {
                                    int type = TypeIdManager.getTypeFromClass(Class.forName("bookkeepr.xmlable." + elems[1]));
                                    List<IdAble> items = this.bookkeepr.getMasterDatabaseManager().getAllOfType(type);
                                    out.printf("<select name='%sId'>\n", elems[1]);
                                    for (IdAble item : items) {
                                        out.printf("<option value='%x'>%s</option>", item.getId(), item.toString());
                                    }
                                    out.println("</select>");

                                } catch (Exception e) {
                                    Logger.getLogger(WebHandler.class.getName()).log(Level.WARNING, "Could not make a type ID for " + line.trim());
                                }
                            } else {


                                out.println(line);
                            }
                            line = in.readLine();
                        }
                        in.close();
                        out.close();
                    } else {
                        outputToInput(new FileInputStream(targetFile), response.getOutputStream());
                    }



                } else {
                    response.sendError(HttpStatus.SC_NOT_FOUND);
                }
            }
        }
    }

    private void outputToInput(InputStream in, OutputStream out) throws FileNotFoundException, IOException, IOException {

        byte[] b = new byte[1024];
        while (true) {
            int count = in.read(b);
            if (count < 0) {
                break;
            }
            out.write(b, 0, count);
        }
        in.close();
        out.close();
    }

    private XMLAble httpForm2XmlAble(String line) throws BookKeeprException {
        try {
            Map<String, String> attr = splitHttpForm(line);
            String clname = attr.get("class");
            if (clname == null) {
                Logger.getLogger(WebHandler.class.getName()).log(Level.WARNING, "Not enough elements provided");
                throw new BookKeeprException("No class specified in submitted form");
            }
            Class targetClass = Class.forName("bookkeepr.xmlable." + clname);
            XMLAble obj = (XMLAble) targetClass.newInstance();

            for (String key : obj.getXmlParameters().keySet()) {
                String val = attr.get(key);
                if (val != null) {
                    StringConvertable conv = obj.getXmlParameters().get(key);
                    String mname = "set" + key;
                    Method method = obj.getClass().getMethod(mname, conv.getTargetClass());
                    Object o = null;
                    try {
                        o = conv.fromString(val);
                    } catch (Exception e) {
                        String msg = "Badly formatted data " + val + " for type " + conv.getTargetClass();
                        Logger.getLogger(WebHandler.class.getName()).log(Level.WARNING, msg);
                        throw new BookKeeprException(msg);
                    }
                    method.invoke(obj, o);
                }
            }

            for (String subclass : obj.getXmlSubObjects()) {
                HashMap<String, XMLAble> subMap = new HashMap<String, XMLAble>();
                for (String key : attr.keySet()) {
                    if (key.startsWith(subclass)) {
                        String[] elems = key.split("\\.");
                        if (elems.length != 3) {
                            Logger.getLogger(WebHandler.class.getName()).log(Level.WARNING, "Not enough elements provided");
                            throw new BookKeeprException("Not enough elements provided");
                        }
                        String val = attr.get(key);

                        XMLAble item = subMap.get(elems[1]);
                        if (item == null) {
                            targetClass = Class.forName("bookkeepr.xmlable." + elems[0]);
                            item = (XMLAble) targetClass.newInstance();
                            subMap.put(elems[1], item);
                        }
                        StringConvertable conv = item.getXmlParameters().get(elems[2]);

                        String mname = "set" + BookKeeprHandler.mangleName(elems[2]);
                        Method method = item.getClass().getMethod(mname, conv.getTargetClass());
                        Object o = null;
                        try {
                            o = conv.fromString(val);
                        } catch (Exception e) {
                            String msg = "Badly formatted data " + val + " for type " + conv.getTargetClass();
                            Logger.getLogger(WebHandler.class.getName()).log(Level.WARNING, msg);
                            throw new BookKeeprException(msg);
                        }
                        method.invoke(item, o);
                    }
                }
                for (String key : subMap.keySet()) {
                    String mname = "add" + BookKeeprHandler.aliases(subclass);
                    Method method = obj.getClass().getMethod(mname, Class.forName("bookkeepr.xmlable." + BookKeeprHandler.aliases(subclass)));
                    method.invoke(obj, subMap.get(key));
                }
            }


            return obj;
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(WebHandler.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            throw new BookKeeprException("Server Error: " + ex.getMessage());

        } catch (InvocationTargetException ex) {
            Logger.getLogger(WebHandler.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            throw new BookKeeprException("Server Error: " + ex.getMessage());

        } catch (NoSuchMethodException ex) {
            Logger.getLogger(WebHandler.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            throw new BookKeeprException("Server Error: " + ex.getMessage());

        } catch (SecurityException ex) {
            Logger.getLogger(WebHandler.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            throw new BookKeeprException("Server Error: " + ex.getMessage());

        } catch (ClassNotFoundException ex) {
            Logger.getLogger(WebHandler.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
            throw new BookKeeprException("Server Error: " + ex.getMessage());
        } catch (InstantiationException ex) {
            Logger.getLogger(WebHandler.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
            throw new BookKeeprException("Server Error: " + ex.getMessage());
        } catch (IllegalAccessException ex) {
            Logger.getLogger(WebHandler.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
            throw new BookKeeprException("Server Error: " + ex.getMessage());
        }

    }

    private Map<String, String> splitHttpForm(String line) {

        String[] parts = line.split("&");
        Map<String, String> res = new HashMap<String, String>();
        for (String s : parts) {
            String[] kv = s.split("=");
            if (kv.length < 2) {
                continue;
            }
            try {

                res.put(kv[0], URLDecoder.decode(kv[1], "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(WebHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return res;
    }
}
