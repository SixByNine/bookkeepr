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
package bookkeepr.managers.observationdatabase;

import bookkeepr.managers.ObservationManager;
import bookkeepr.managers.TypeIdManager;
import bookkeepr.xml.IdAble;
import bookkeepr.xmlable.Configuration;
import bookkeepr.xmlable.Pointing;
import coordlib.Coordinate;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author kei041
 */
public class SkyViewManager {

    private Color[] baseColors = new Color[]{Color.GREEN, Color.BLUE, Color.RED, Color.CYAN, Color.YELLOW, Color.GRAY};
    private ObservationManager obsMan;

    public SkyViewManager(ObservationManager obsMan) {
        this.obsMan = obsMan;
    }
    public BufferedImage makeImage(List<Pointing> pointings, int height, int width) {
        return makeImage(pointings, height, width,1,1,0,0);
    }
    public BufferedImage makeImage(List<Pointing> pointings, int height, int width, int xmult, int ymult ,int xoff, int yoff) {

        double maxGl = 180;
        double minGl = -180;
        double maxGb = 90;
        double minGb = -90;

//        for (Pointing p : pointings) {
//            double gl = p.getCoordinate().getGl();
//            double gb = p.getCoordinate().getGb();
//            if (gl > 180) {
//                gl -= 360;
//            }
//            if (gl > maxGl) {
//                maxGl = gl;
//            }
//            if (gl < minGl) {
//                minGl = gl;
//            }
//            if (gb > maxGb) {
//                maxGb = gb;
//            }
//            if (gb < minGb) {
//                minGb = gb;
//            }
//        }
//        maxGl += 0.5;
//        minGl -= 0.5;
//        maxGb += 0.5;
//        minGb -= 0.5;



        int[][] to_ptg = new int[height][width];
        int[][] done_ptg = new int[height][width];
        Color[][] colors = new Color[height][width];
        HashMap<Long, Color> configToColor = new HashMap<Long, Color>();
        int nextcol = 0;

        for (IdAble idable : this.obsMan.getDbManager().getAllOfType(TypeIdManager.getTypeFromClass(Configuration.class))) {
            configToColor.put(idable.getId(), this.baseColors[nextcol]);
            nextcol++;
        }

        for (int[] arr : to_ptg) {
            Arrays.fill(arr, 0);
        }
        for (int[] arr : done_ptg) {
            Arrays.fill(arr, 0);
        }


        for (Pointing p : pointings) {
//            if (Math.random() < 0.1) {
//                p.setToBeObserved(false);
//            }
//            double gl = p.getCoordinate().getGl();
//            double gb = p.getCoordinate().getGb();

            for (Coordinate c : p.getCoverage()) {
//                double gl = c.getGl();
//                double gb = c.getGb();
//                if (gl > 180) {
//                    gl -= 360;
//                }

//                double yN = (gb - minGb) / (maxGb - minGb);
//                double xN = (gl - minGl) / (maxGl - minGl);
//                double cos = Math.cos(Math.toRadians(gb));
//                int x = (int) (xN * width * cos + (1 - cos) * width / 2.0);
//                int y = (int) (yN * height);



                double xl = Math.toRadians(c.getGl());
                double xb = Math.toRadians(c.getGb());
                if (xl < 0) {
                    xl += Math.PI * 2;
                }
                int[] xy = aitoff(xl, xb, minGb, maxGb, minGl, maxGl, width*xmult, height*ymult);
                int x = xy[0] - xoff;
                int y = xy[1] + yoff - height*(ymult-1);
                
                if(x < 0 || x >= width || y < 0 || y >= height)continue;

                Color col = configToColor.get(p.getConfigurationId());
                if (col == null) {
                    col = baseColors[nextcol];
                    configToColor.put(p.getConfigurationId(), col);
                    nextcol++;
                    if (nextcol >= baseColors.length) {
                        nextcol = 0;
                    }
                }
                colors[y][x] = col;



                if (p.getToBeObserved()) {
                    to_ptg[y][x]++;
                } else {
                    done_ptg[y][x]++;
                }
            }
        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics g = img.getGraphics();
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, width, height);
        


//        for (int bv = 0; bv <
//                height; bv++) {
//            double b = ((double) bv / (double) height) * (maxGb - minGb) + minGb;
//            double cos = Math.cos(Math.toRadians(b));
//
//            double lvstart = ((1 - cos) * (width - 2) / 2.0);
//
//            double lvend = ((1 + cos) * (width - 2) / 2.0);
//
//            g.setColor(Color.BLACK);
//            if ((Math.abs(b / 30) - (int) Math.abs(b / 30)) * 30 < (180.0 / (double) height)) {
//                g.setColor(Color.DARK_GRAY);
//            }
//
//            g.drawRect((int) lvstart + 1, bv, (int) (lvend - lvstart - 1), 1);
//
//            g.setColor(new Color(1.0f, 1.0f, 1.0f, (float) (1 - (lvstart - (int) lvstart))));
//            g.drawRect((int) lvstart, bv, 1, 1);
//            g.setColor(new Color(1.0f, 1.0f, 1.0f, (float) ((lvstart - (int) lvstart))));
//            g.drawRect((int) lvstart + 1, bv, 1, 1);
//
//
//            g.setColor(new Color(1.0f, 1.0f, 1.0f, (float) (1 - (lvend - (int) lvend))));
//            g.drawRect((int) lvend, bv, 1, 1);
//            g.setColor(new Color(1.0f, 1.0f, 1.0f, (float) ((lvend - (int) lvend))));
//            g.drawRect((int) lvend + 1, bv, 1, 1);
//
//            g.setColor(Color.DARK_GRAY);
//
//            for (double gl = -120; gl <
//                    121; gl +=
//                            60) {
//                double xN = (gl - minGl) / (maxGl - minGl);
//                int x = (int) (xN * width * cos + (1 - cos) * width / 2.0);
//                g.drawRect((int) x, bv, 1, 1);
//            }
//
//        }


        g.setColor(Color.WHITE);
        for (int bv = 0; bv < ymult*height; bv++) {
            double b = ((double) (bv) / (double) (height*ymult)) * (maxGb - minGb) + minGb;
            int[] xy = aitoff(Math.PI, Math.toRadians(b), minGb, maxGb, minGl, maxGl, width*xmult, height*ymult);
            g.drawRect(xy[0] - xoff, xy[1] - yoff, 1, 1);
            xy = aitoff(Math.PI + 0.000001, Math.toRadians(b), minGb, maxGb, minGl, maxGl, width*xmult, height*ymult);
            g.drawRect(xy[0] - xoff, xy[1] - yoff, 1, 1);
        }

        // draw constant b
        g.setColor(Color.GRAY);

        for (double b = -60; b < 90; b += 30) {
            int[] prev_xy = null;
            for (double l = 0; l < Math.PI - 0.001; l += (Math.PI / width)) {
                int[] xy = aitoff(l, Math.toRadians(b), minGb, maxGb, minGl, maxGl, width*xmult, height*ymult);
                if (prev_xy != null) {
                    g.drawLine(prev_xy[0] - xoff, prev_xy[1] - yoff, xy[0] - xoff, xy[1] - yoff);
                }
                prev_xy = xy;
            }
            prev_xy = null;
            for (double l = Math.PI + 0.001; l < Math.PI * 2; l += (Math.PI / width)) {
                int[] xy = aitoff(l, Math.toRadians(b), minGb, maxGb, minGl, maxGl, width*xmult, height*ymult);
                if (prev_xy != null) {
                    g.drawLine(prev_xy[0] - xoff, prev_xy[1] - yoff, xy[0] - xoff, xy[1] - yoff);
                }
                prev_xy = xy;
            }
        }

        for (double l = 30; l < 360; l += 30) {
            int[] prev_xy = null;
            for (double b = -Math.PI / 2.0; b < Math.PI / 2.0; b += (Math.PI / width)) {
                int[] xy = aitoff(Math.toRadians(l), b, minGb, maxGb, minGl, maxGl, width*xmult, height*ymult);
                if (prev_xy != null) {
                    g.drawLine(prev_xy[0] - xoff, prev_xy[1] - yoff, xy[0] - xoff, xy[1] - yoff);
                }
                prev_xy = xy;
            }

        }

        g.drawLine(xmult*width / 2 -xoff, 0, xmult*width / 2 -xoff, ymult*height - 1 -yoff);
//        g.drawLine(0, height / 2, width, height / 2);

        for (int x = 0; x <
                width; x++) {
            for (int y = 0; y <
                    height; y++) {

                if (to_ptg[y][x] == 0) {
                    if (done_ptg[y][x] == 0) {
                    } else {
                        g.setColor(new Color(colors[y][x].getRed(), colors[y][x].getGreen(), colors[y][x].getBlue()));
                        g.drawRect(x, height - y -1, 1, 1);
                    }

                } else {
                    if (done_ptg[y][x] == 0) {
                        g.setColor(new Color(colors[y][x].getRed() / 6 + 48, colors[y][x].getGreen() / 6 + 48, colors[y][x].getBlue() / 6 + 48));
                        g.drawRect(x, height - y -1, 1, 1);
                    } else {
                        g.setColor(new Color(colors[y][x].getRed() / 2 + 64, colors[y][x].getGreen() / 2 + 64, colors[y][x].getBlue() / 2 + 64));
                        g.drawRect(x, height - y -1, 1, 1);
                    }

                }
            }
        }
//        g.setColor(Color.MAGENTA);
//g.drawRect(0, 0, width, height);
        return img;
    }

    private int[] aitoff(double xl, double xb, double minGb, double maxGb, double minGl, double maxGl, int width, int height) {

        int[] xy = new int[2];
        {
            double xx = 0;
            double yy = 0;

            double twopi = 6.283185;
            double pi = 3.141593;
            double test = 0.001;

            double test1 = (twopi / 4.0) - Math.abs(xb);
            double test2 = twopi - xl;
            if (((test1 < test) || (test2 < test)) || (xl < test)) {

                if (test1 > test) {
                    xx = 0.0;
                    yy = xb;
                } else {
                    xx = 0.0;
                    yy = twopi / 4.;
                    yy *= Math.signum(xb);
                }
            } else {
                double cxl = xl;
                if (xl > pi) {
                    cxl = xl - twopi;
                }
                double cosb = Math.cos(xb);
                double tanb = Math.sqrt(1.0 - (cosb * cosb)) / cosb;
                if (xb < 0.0) {
                    tanb = -tanb;
                }
                double cosl2 = Math.cos(cxl / 2.);
                double sinl2 = Math.sqrt(1.0 - (cosl2 * cosl2));
                if (cxl < 0.0) {
                    sinl2 = -sinl2;
                }
                double z = Math.acos(cosb * cosl2);
                if (cxl < 0.0) {
                    z = -z;
                }
                double cot = tanb / sinl2;
                double sina = 1. / Math.sqrt(1.0 + (cot * cot));
                double cosa = cot * sina;
                xx = -((2. * z) * sina);
                yy = z * cosa;
            }

            xx = Math.toDegrees(xx);
            yy = Math.toDegrees(yy);
            if (xx > 180) {
                xx -= 360;
            }
            double yN = (yy - minGb) / (maxGb - minGb);
            double xN = (xx - minGl) / (maxGl - minGl);

            int x = (int) (xN * width);
            int y = (int) (yN * height);
            if (x >= width) {
                x = width - 1;
            }
            if (y >= height) {
                y = height - 1;
            }
            xy[0] = x;
            xy[1] = y;
        }
        return xy;
    }
}
