package com.eerussianguy.blazemap.api.pipeline;

import com.eerussianguy.blazemap.api.BlazeRegistry;

public abstract class MasterDatum {

    public abstract BlazeRegistry.Key<DataType<MasterDatum>> getID();

    @Override
    public final boolean equals(Object other) {
        if(this == other) return true;
        if(other == null) return false;
        if(other.getClass() != this.getClass()) return false;

        MasterDatum md = (MasterDatum) other;
        if(!md.getID().equals(this.getID())) return false;

        return equalsMD(md);
    }

    public abstract boolean equalsMD(MasterDatum md);
}