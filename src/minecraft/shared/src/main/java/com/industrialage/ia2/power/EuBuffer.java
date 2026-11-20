package com.industrialage.ia2.power;

/** Read-only access to the EU held in a device's live internal buffer. */
public interface EuBuffer {
    /** Returns the current contents and actual capacity, including installed upgrades. */
    Reading bufferedEu();

    /** The stored amount and capacity in EU. A zero capacity means no buffer. */
    record Reading(double stored, double capacity) {}
}
