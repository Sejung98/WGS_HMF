package com.hartwig.hmftools.orange.algo.pave;

import com.google.common.io.Resources;
import com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache;
import com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion;

import org.jetbrains.annotations.NotNull;

public final class TestEnsemblDataCacheFactory
{
    private static final String ENSEMBL_DATA_CACHE_PATH = Resources.getResource("ensembl").getPath();

    @NotNull
    public static EnsemblDataCache createDummyCache()
    {
        EnsemblDataCache cache = new EnsemblDataCache(ENSEMBL_DATA_CACHE_PATH, RefGenomeVersion.V37);
        return cache;
    }

    @NotNull
    public static EnsemblDataCache loadTestCache()
    {
        EnsemblDataCache cache = new EnsemblDataCache(ENSEMBL_DATA_CACHE_PATH, RefGenomeVersion.V37);
        cache.setRequireNonEnsemblTranscripts();
        cache.load(false);
        return cache;
    }
}
