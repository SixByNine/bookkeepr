package bookkeepr.managers;

import bookkeepr.xmlable.ArchivedStorage;
import bookkeepr.xmlable.ArchivedStorageIndex;
import bookkeepr.xmlable.ArchivedStorageWrite;
import bookkeepr.xmlable.ArchivedStorageWriteIndex;
import bookkeepr.xmlable.Backend;
import bookkeepr.xmlable.BackendIndex;
import bookkeepr.xmlable.CandidateListIndex;
import bookkeepr.xmlable.CandidateListStub;
import bookkeepr.xmlable.ClassifiedCandidate;
import bookkeepr.xmlable.ClassifiedCandidateIndex;
import bookkeepr.xmlable.Configuration;
import bookkeepr.xmlable.ConfigurationIndex;
import bookkeepr.xmlable.DeletedId;
import bookkeepr.xmlable.DeletedIdIndex;
import bookkeepr.xmlable.Index;
import bookkeepr.xmlable.Observation;
import bookkeepr.xmlable.ObservationIndex;
import bookkeepr.xmlable.Pointing;
import bookkeepr.xmlable.PointingIndex;
import bookkeepr.xmlable.Processing;
import bookkeepr.xmlable.ProcessingIndex;
import bookkeepr.xmlable.Psrxml;
import bookkeepr.xmlable.PsrxmlIndex;
import bookkeepr.xmlable.RawCandidateIndex;
import bookkeepr.xmlable.RawCandidateStub;
import bookkeepr.xmlable.Receiver;
import bookkeepr.xmlable.ReceiverIndex;
import bookkeepr.xmlable.Session;
import bookkeepr.xmlable.SessionIndex;
import bookkeepr.xmlable.Telescope;
import bookkeepr.xmlable.TelescopeIndex;
import bookkeepr.xmlable.ViewedCandidates;
import bookkeepr.xmlable.ViewedCandidatesIndex;

/**
 *
 * @author Mike Keith
 */
public class TypeIdManager {

    /**
     * Types 00 -> 0F reserved for system types
     * 00    : null type, when type is not known
     * 01    : session
     * 02-0F : not used
     * 
     * Types 10 -> 1F reserved for observation DB 
     * 10    : pointing
     * 11    : observation
     * 12    : configuration
     * 13-1E : not used
     * 1F    : psrxml
     */
    public static int getTypeFromClass(Class c) {


        if (c == Session.class) {
            return 0x01;
        }
        if (c == DeletedId.class) {
            return 0x0F;
        }

        if (c == Pointing.class) {
            return 0x10;
        }
        if (c == Observation.class) {
            return 0x11;
        }
        if (c == Configuration.class) {
            return 0x12;
        }
        if (c == Telescope.class) {
            return 0x13;
        }
        if (c == Receiver.class) {
            return 0x14;
        }
        if (c == Backend.class) {
            return 0x15;
        }
        if (c == ArchivedStorage.class) {
            return 0x16;
        }
        if (c == ArchivedStorageWrite.class) {
            return 0x17;
        }
        if (c == Psrxml.class) {
            return 0x1E;
        }

        if (c == Processing.class) {
            return 0x20;
        }

        if (c == RawCandidateStub.class) {
            return 0x30;
        }
        if (c == CandidateListStub.class) {
            return 0x31;
        }
        if (c == ViewedCandidates.class) {
            return 0x32;
        }
        if (c == ClassifiedCandidate.class) {
            return 0x38;
        }

        return 0x00; //  the 'null' type for unknown elements!

    }

    public static Index getIndexFromClass(Class c) {

        if (c == Session.class) {
            return new SessionIndex();
        }
        if (c == DeletedId.class) {
            return new DeletedIdIndex();
        }
        if (c == Pointing.class) {
            return new PointingIndex();
        }
        if (c == Observation.class) {
            return new ObservationIndex();
        }
        if (c == Configuration.class) {
            return new ConfigurationIndex();
        }
        if (c == Telescope.class) {
            return new TelescopeIndex();
        }
        if (c == Receiver.class) {
            return new ReceiverIndex();
        }
        if (c == Backend.class) {
            return new BackendIndex();
        }
        if (c == ArchivedStorage.class) {
            return new ArchivedStorageIndex();
        }
        if (c == ArchivedStorageWrite.class) {
            return new ArchivedStorageWriteIndex();
        }
        if (c == Psrxml.class) {
            return new PsrxmlIndex();
        }
        if (c == Processing.class) {
            return new ProcessingIndex();
        }
        if (c == RawCandidateStub.class) {
            return new RawCandidateIndex();
        }
        if (c == CandidateListStub.class) {
            return new CandidateListIndex();
        }
        if (c == ViewedCandidates.class) {
            return new ViewedCandidatesIndex();
        }
        if (c == ClassifiedCandidate.class) {
            return new ClassifiedCandidateIndex();
        }
        return null;
    }
}
