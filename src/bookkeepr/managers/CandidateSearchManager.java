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

import bookkeepr.xmlable.ClassifiedCandidate;
import bookkeepr.xmlable.ClassifiedCandidateIndex;
import bookkeepr.xmlable.ClassifiedCandidateSelectRequest;
import java.util.ArrayList;

/**
 *
 * @author kei041
 */
public class CandidateSearchManager {

    CandidateManager candMan;

    public CandidateSearchManager(CandidateManager candMan) {
        this.candMan = candMan;
    }

     
    
    
    public ClassifiedCandidateIndex searchCandidates(ClassifiedCandidateSelectRequest req) {

        ArrayList<ClassifiedCandidate> list = candMan.getClassifiedCandidatesNear(req.getTarget(), req.getTargetSeperation());
        ArrayList<ClassifiedCandidate> newlist = null;
        System.out.println("TESTX "+list.size());
        if(req.getCandClassInt() >= 0){
            System.out.println("TEST1 "+req.getCandClassInt());
            newlist = new ArrayList<ClassifiedCandidate>();
            for(ClassifiedCandidate c : list){
                if(c.getCandClassInt()==req.getCandClassInt()){
                    newlist.add(c);
                }
            }
            list = newlist;
            newlist=null;
        }
        
        
        if(req.getConfStatus() != null){
            newlist = new ArrayList<ClassifiedCandidate>();
            for(ClassifiedCandidate c : list){
                if(c.getConfStatus().contains(req.getConfStatus())){
                    newlist.add(c);
                }
            }
            list = newlist;
            newlist=null;
        }
        
                
        if(req.getObsStatus() != null){
            newlist = new ArrayList<ClassifiedCandidate>();
            for(ClassifiedCandidate c : list){
                if(c.getObsStatus().contains(req.getObsStatus())){
                    newlist.add(c);
                }
            }
            list = newlist;
            newlist=null;
        }
        

        ClassifiedCandidateIndex idx = new ClassifiedCandidateIndex();
        for(ClassifiedCandidate c : list){
            idx.addClassifiedCandidate(c);
        }


        return idx;
    }
}
