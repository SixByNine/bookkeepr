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

import bookkeepr.xmlable.BackgroundedTask;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author kei041
 */
public class BackgroundTaskRunner extends Thread {

    int nextId = 0;
    int lastStarted = -1;
    boolean run = true;
    private ArrayBlockingQueue<BackgroundedTask> taskQueue = new ArrayBlockingQueue<BackgroundedTask>(50);
    private ArrayList<BackgroundedTask> startedTasks = new ArrayList<BackgroundedTask>();

    @Override
    public void run() {
        super.run();

        while (run) {
            BackgroundedTask task = null;
            try {
                task = taskQueue.poll(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
            }
            if (task == null) {
                continue;
            }

            startedTasks.add(task);
            lastStarted = task.getId();
            task.run();
        }

    }

    public BackgroundedTask getById(int targetId) {
        if (targetId <= lastStarted) {
            // a started task
            for (BackgroundedTask t : startedTasks) {
                if (t.getId() == targetId) {
                    return t;
                }
            }
        } else {
            // a queued task
            for (BackgroundedTask t : taskQueue) {
                if (t.getId() == targetId) {
                    return t;
                }
            }
        }
        return null;
    }

    public void terminate(){
        this.run = false;
        this.interrupt();
    }
    
    public boolean offer(BackgroundedTask task) {
        task.setId(nextId++);
        return taskQueue.offer(task);
    }
    
    public List<BackgroundedTask> getAllTasks(){
        ArrayList<BackgroundedTask> result = new ArrayList<BackgroundedTask>(taskQueue);
        result.addAll(startedTasks);
        return result;
    }
}
