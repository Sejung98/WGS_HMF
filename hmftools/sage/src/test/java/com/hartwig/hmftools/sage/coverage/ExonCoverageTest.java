package com.hartwig.hmftools.sage.coverage;

import static org.junit.Assert.assertEquals;

import com.hartwig.hmftools.common.genome.bed.ImmutableNamedBed;
import com.hartwig.hmftools.common.genome.bed.NamedBed;
import com.hartwig.hmftools.common.region.ChrBaseRegion;

import org.junit.Test;

public class ExonCoverageTest
{
    @Test
    public void testAlignmentBefore()
    {
        ExonCoverage victim = exon("Gene", 100, 104);
        victim.processRead(30, 99);
        assertCoverage(victim, 0, 0, 0, 0, 0);
    }

    @Test
    public void testAlignmentOverlapsStart()
    {
        ExonCoverage victim = exon("Gene", 100, 104);
        victim.processRead(90, 100);
        assertCoverage(victim, 1, 0, 0, 0, 0);

        victim.processRead(91, 102);
        assertCoverage(victim, 2, 1, 1, 0, 0);
    }

    @Test
    public void testAlignmentOverlapsEnd()
    {
        ExonCoverage victim = exon("Gene", 100, 104);
        victim.processRead(103, 110);
        assertCoverage(victim, 0, 0, 0, 1, 1);

        victim.processRead(104, 110);
        assertCoverage(victim, 0, 0, 0, 1, 2);
    }

    @Test
    public void testAlignmentOverlapsBoth()
    {
        ExonCoverage victim = exon("Gene", 100, 104);
        victim.processRead(90, 110);
        assertCoverage(victim, 1, 1, 1, 1, 1);

        victim.processRead(90, 110);
        assertCoverage(victim, 2, 2, 2, 2, 2);
    }

    @Test
    public void testAlignmentWithin()
    {
        ExonCoverage victim = exon("Gene", 100, 104);
        victim.processRead(101, 103);
        assertCoverage(victim, 0, 1, 1, 1, 0);

        victim.processRead(101, 105);
        assertCoverage(victim, 0, 2, 2, 2, 1);

        victim.processRead(99, 101);
        assertCoverage(victim, 1, 3, 2, 2, 1);
    }

    @Test
    public void testAlignmentAfter()
    {
        ExonCoverage victim = exon("Gene", 100, 104);
        victim.processRead(106, 200);
        assertCoverage(victim, 0, 0, 0, 0, 0);
    }

    private void assertCoverage(ExonCoverage victim, int... values)
    {
        for(int i = 0; i < values.length; i++)
        {
            int value = values[i];
            assertEquals(value, victim.coverage()[i]);
        }
    }

    private ChrBaseRegion alignment(int start, int end)
    {
        return new ChrBaseRegion("1", start, end);
    }

    private ExonCoverage exon(String gene, int start, int end)
    {
        NamedBed bed = ImmutableNamedBed.builder().chromosome("1").name(gene).start(start).end(end).build();
        return new ExonCoverage(bed, 1);
    }
}
