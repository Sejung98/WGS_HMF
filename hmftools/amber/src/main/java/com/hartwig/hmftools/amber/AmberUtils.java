package com.hartwig.hmftools.amber;

import static com.hartwig.hmftools.amber.AmberConfig.AMB_LOGGER;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.io.Files;
import com.hartwig.hmftools.common.amber.AmberBAF;
import com.hartwig.hmftools.common.amber.AmberSite;
import com.hartwig.hmftools.common.amber.ImmutableAmberBAF;
import com.hartwig.hmftools.common.genome.bed.NamedBedFile;
import com.hartwig.hmftools.common.genome.region.GenomeRegion;

public class AmberUtils
{
    public static boolean isValid(final AmberBAF baf)
    {
        return Double.isFinite(baf.tumorBAF()) & Double.isFinite(baf.normalBAF());
    }

    @SuppressWarnings("UnstableApiUsage")
    public static List<GenomeRegion> loadBedFromResource(String resourcePath) throws IOException
    {
        List<GenomeRegion> genomeRegions = new ArrayList<>();
        java.io.InputStream bedStream = AmberUtils.class.getClassLoader().getResourceAsStream(resourcePath);

        if(bedStream == null)
        {
            AMB_LOGGER.error("unable to find resource bed file: {}", resourcePath);
            throw new RuntimeException("unable to find resource bed file: " + resourcePath);
        }

        File tempFile = null;
        // write the resource out to a temp file and read it back
        try
        {
            tempFile = java.io.File.createTempFile(Files.getNameWithoutExtension(resourcePath), "bed");
            Files.write(bedStream.readAllBytes(), tempFile);
            genomeRegions.addAll(NamedBedFile.readBedFile(tempFile.getPath()));
        }
        finally
        {
            if (tempFile != null)
            {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }

        return genomeRegions;
    }

    public static AmberBAF fromTumorBaf(final TumorBAF tumor)
    {
        int tumorAltCount = tumor.TumorEvidence.AltSupport;
        double tumorBaf = tumorAltCount / (double) (tumorAltCount + tumor.TumorEvidence.RefSupport);
        int normalAltCount = tumor.NormalAltSupport;
        double normalBaf = normalAltCount / (double) (normalAltCount + tumor.NormalRefSupport);

        return ImmutableAmberBAF.builder()
                .from(tumor)
                .normalDepth(tumor.NormalReadDepth)
                .tumorDepth(tumor.TumorEvidence.ReadDepth)
                .normalBAF(normalBaf)
                .tumorBAF(tumorBaf)
                .build();
    }

    public static AmberBAF fromBaseDepth(final PositionEvidence baseDepth)
    {
        int normalAltCount = baseDepth.AltSupport;
        double normalBaf = normalAltCount / (double) (normalAltCount + baseDepth.RefSupport);
        return ImmutableAmberBAF.builder()
                .from(baseDepth)
                .normalDepth(baseDepth.ReadDepth)
                .tumorDepth(-1)
                .normalBAF(normalBaf)
                .tumorBAF(-1)
                .build();
    }

    public static AmberSite depthAsSite(final PositionEvidence baseDepth)
    {
        return new AmberSite(baseDepth.chromosome(), baseDepth.position(), baseDepth.ref(), baseDepth.alt(), false);
    }
}
