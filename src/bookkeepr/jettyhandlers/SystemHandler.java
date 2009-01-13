package bookkeepr.jettyhandlers;

import bookkeepr.BookKeepr;
import bookkeepr.xml.XMLWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;

/**
 *
 * @author Mike Keith
 */
public class SystemHandler extends AbstractHandler {

    private static Pattern regex = Pattern.compile("/");
    private BookKeepr bookkeepr;

    public SystemHandler(BookKeepr bookkeepr) {

        this.bookkeepr = bookkeepr;
    }

    public void handle(String path, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
        Logger.getLogger(SystemHandler.class.getName()).log(Level.FINE, "Recieved " + request.getMethod() + " request for " + path);
//        Enumeration<String> en = request.getHeaderNames();
//        while(en.hasMoreElements()) {
//            String s = en.nextElement();
//            Logger.getLogger(SystemHandler.class.getName()).log(Level.FINE, "HDR -- " + s + "\t\t"+request.getHeader(s));
//
//        }

        if (request.getMethod().equals("GET")) {
            if (path.startsWith("/sys/")) {
                ((Request) request).setHandled(true);
                String[] elems = regex.split(path.substring(1));
                if (elems.length == 2) {
                    if (elems[1].equals("down")) {
                        Thread t = new Thread() {

                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException ex) {
                                }
                                bookkeepr.stop();
                            }
                        };
                        Logger.getLogger(SystemHandler.class.getName()).log(Level.INFO, "Server sent remote Shutdown command");
                        response.setStatus(303);
                        response.addHeader("Location", "/log/");
                        t.start();

                    } else if (elems[1].equals("testlog")) {
                        Logger.getLogger(SystemHandler.class.getName()).log(Level.FINE, "Creating Test Log Messages");
                        Logger.getLogger(SystemHandler.class.getName()).log(Level.INFO, "Creating Test Log Messages");
                        Logger.getLogger(SystemHandler.class.getName()).log(Level.WARNING, "Creating Test Log Messages");
                        Logger.getLogger(SystemHandler.class.getName()).log(Level.SEVERE, "Creating Test Log Messages");
                        response.setStatus(303);
                        response.addHeader("Location", "/log/");

                    } else if (elems[1].equals("hashcheck")) {
                        File root = new File(bookkeepr.getConfig().getRootPath());
                        
                        String[] list = root.list();
                        Arrays.sort(list);
                        String[] cmd = new String[list.length + 1];
                        System.arraycopy(list, 0, cmd, 1, list.length);
                        cmd[0] = "md5sum";
                        Process p = Runtime.getRuntime().exec(cmd, new String[0], root);
                        response.setContentType("text/plain");
                        PrintStream out = new PrintStream(response.getOutputStream());
                        out.printf("%02d\t%s\n", bookkeepr.getConfig().getOriginId(), bookkeepr.getConfig().getExternalUrl());
                        this.outputToInput(p.getInputStream(), out);
                        response.getOutputStream().close();
                    }
                }
            }
            if (path.startsWith("/log")) {
                ((Request) request).setHandled(true);
                String[] elems = regex.split(path.substring(1));
                if (elems.length == 2 && elems[1].equals("clear")) {
                    bookkeepr.getStatusMon().clearStatus();
                    response.setStatus(303);
                    response.addHeader("Location", "/log/");
                } else {
                    response.setHeader("Refresh", "10");
                    XMLWriter.write(response.getOutputStream(), bookkeepr.getStatusMon().getLog());
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
}
