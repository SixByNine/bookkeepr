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

import bookkeepr.*;
import bookkeepr.xmlable.BackgroundedTask;
import java.io.IOException;
import java.io.PrintWriter;
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
 * @author kei041
 */
public class BackgroundTaskHandler extends AbstractHandler {

    private static Pattern regex = Pattern.compile("/");
    BackgroundTaskRunner bgrunner;

    public BackgroundTaskHandler(BackgroundTaskRunner bgrunner) {
        this.bgrunner = bgrunner;
    }

    public void handle(String path, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
        if (path.startsWith("/tasks")) {
            ((Request) request).setHandled(true);


            String[] elems = regex.split(path.substring(1));

            if (elems.length == 2) {
                int id = Integer.parseInt(elems[1]);
                BackgroundedTask task = bgrunner.getById(id);
                if (task == null) {
                    Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Task " + id + " does not exist!");
                    response.sendError(404, "Malformed url");
                }
                if (request.getMethod().equals("GET")) {
                    response.addHeader("Content-type", "text/xml");
                    if(task.getStatus()==BackgroundedTask.TaskStatus.RUNNING)
                        response.addHeader("Refresh", "1");
                    if(task.getStatus()==BackgroundedTask.TaskStatus.WAITING)
                        response.addHeader("Refresh", "5");

                    PrintWriter out = response.getWriter();
                    
                    out.printf("<?xml version='1.0'?>\n");
                    out.printf("<task>");
                    out.printf("<name>%s</name><status>%s</status><messages>%s</messages>", task.getName(), task.getStatus(), task.getMessages());

                    out.printf("</task>\n");

                }
            } else if (elems.length == 1) {
                
                String host = request.getHeader("HOST");
                if (host.endsWith("/")) {
                    host = host.substring(0, host.length() - 1);
                }
                
                response.addHeader("Content-type", "text/xml");
                PrintWriter out = response.getWriter();
                out.printf("<?xml version='1.0'?>\n");
                out.printf("<task_list>");

                for (BackgroundedTask task : this.bgrunner.getAllTasks()) {
                    out.printf("<task><name>%s</name><status>%s</status><url>http://%s/%s/%d</url></task>\n", task.getName(), task.getStatus(),host,"tasks",task.getId());
                }
                out.printf("</task_list>\n");

            } else {

                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Tried to GET an invalid path " + path);
                response.sendError(400, "Malformed url");
            }



        }
    }
}
