package com.hartwig.hmftools.amber;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.hartwig.hmftools.common.genome.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion;

public class RegionOfHomozygosityFinderTest
{
    private static final RegionOfHomozygosityFinder.Zygosity het = RegionOfHomozygosityFinder.Zygosity.HETEROZYGOUS;
    private static final RegionOfHomozygosityFinder.Zygosity hom = RegionOfHomozygosityFinder.Zygosity.HOMOZYGOUS;

    private static RegionOfHomozygosityFinder.LocusZygosity bafSite(int position, RegionOfHomozygosityFinder.Zygosity zygosity)
    {
        return new RegionOfHomozygosityFinder.LocusZygosity(position, zygosity);
    }

    @Test
    public void testFindRegionSample() throws IOException
    {
        int minHomozygousRegionSize = 200;
        int minSnpLociCount = 5;
        int basesWindowSize = 50;
        int maxHetInWindow = 3;
        RegionOfHomozygosityFinder finder = new RegionOfHomozygosityFinder(RefGenomeVersion.V37,
                AmberConstants.DEFAULT_MIN_DEPTH_PERCENTAGE, AmberConstants.DEFAULT_MAX_DEPTH_PERCENTAGE,
                minHomozygousRegionSize, minSnpLociCount, basesWindowSize, maxHetInWindow);

        // very simple one, just a stretch of hom
        var bafSites = new ArrayList<>(Arrays.asList(
                bafSite(10, het),
                bafSite(13, hom),
                bafSite(100, hom),
                bafSite(140, hom),
                bafSite(141, hom),
                bafSite(200, hom),
                bafSite(219, hom),
                bafSite(222, het),
                bafSite(223, het),
                bafSite(225, het)
                ));

        var regions = finder.findRegionsForChromosome(HumanChromosome._1, bafSites);

        assertEquals(regions.size(), 1);
        assertEquals(regions.get(0).Start, 13);
        assertEquals(regions.get(0).End, 219);

        // now two regions
        bafSites.addAll(Arrays.asList(
                bafSite(226, het),
                bafSite(250, hom),
                bafSite(260, hom),
                bafSite(270, hom),
                bafSite(280, hom),
                bafSite(470, hom),
                bafSite(550, het),
                bafSite(570, het),
                bafSite(600, het)
        ));

        regions = finder.findRegionsForChromosome(HumanChromosome._1, bafSites);

        assertEquals(regions.size(), 2);
        assertEquals(regions.get(0).Start, 13);
        assertEquals(regions.get(0).End, 219);
        assertEquals(regions.get(1).Start, 250);
        assertEquals(regions.get(1).End, 470);

    }

    @Test
    // test case where there are 3 hets scattered inside the window
    public void testFindRegionHetsInWindow() throws IOException
    {
        int minHomozygousRegionSize = 200;
        int minSnpLociCount = 5;
        int basesWindowSize = 100;
        int maxHetInWindow = 3;
        RegionOfHomozygosityFinder finder = new RegionOfHomozygosityFinder(RefGenomeVersion.V37,
                AmberConstants.DEFAULT_MIN_DEPTH_PERCENTAGE, AmberConstants.DEFAULT_MAX_DEPTH_PERCENTAGE,
                minHomozygousRegionSize, minSnpLociCount, basesWindowSize, maxHetInWindow);

        // very simple one, just a stretch of hom
        var bafSites = new ArrayList<>(Arrays.asList(
                bafSite(10, het),
                bafSite(13, hom),
                bafSite(100, hom),
                bafSite(110, het),
                bafSite(140, hom),
                bafSite(150, het),
                bafSite(200, hom),
                bafSite(209, het),
                bafSite(219, hom),
                bafSite(222, het),
                bafSite(223, het),
                bafSite(225, het)
        ));

        var regions = finder.findRegionsForChromosome(HumanChromosome._1, bafSites);

        assertEquals(regions.size(), 1);
        assertEquals(regions.get(0).Start, 13);
        assertEquals(regions.get(0).End, 219);

        // now two regions
        bafSites.addAll(Arrays.asList(
                bafSite(226, het),
                bafSite(250, hom),
                bafSite(255, het),
                bafSite(260, hom),
                bafSite(265, het),
                bafSite(270, hom),
                bafSite(275, het),
                bafSite(280, hom),
                bafSite(470, hom),
                bafSite(550, het),
                bafSite(570, het),
                bafSite(600, het)
        ));

        regions = finder.findRegionsForChromosome(HumanChromosome._1, bafSites);

        assertEquals(regions.size(), 2);
        assertEquals(regions.get(0).Start, 13);
        assertEquals(regions.get(0).End, 219);
        assertEquals(regions.get(1).Start, 250);
        assertEquals(regions.get(1).End, 470);

        // next test there are too many het sites
        bafSites = new ArrayList<>(Arrays.asList(
                bafSite(10, het),
                bafSite(13, hom),
                bafSite(100, hom),
                bafSite(110, het),
                bafSite(120, het), // extra het site here to push number over 3 in 100
                bafSite(140, hom),
                bafSite(150, het),
                bafSite(200, hom),
                bafSite(209, het),
                bafSite(219, hom),
                bafSite(222, het),
                bafSite(223, het),
                bafSite(225, het)
        ));

        regions = finder.findRegionsForChromosome(HumanChromosome._1, bafSites);

        assertEquals(regions.size(), 0);
    }
}
