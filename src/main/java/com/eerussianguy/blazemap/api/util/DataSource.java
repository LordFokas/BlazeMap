package com.eerussianguy.blazemap.api.util;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;

public interface DataSource {
    <T extends MasterDatum> T get(Key<DataType<T>> key);
}
