package com.hartwig.hmftools.amber;

import com.hartwig.hmftools.common.amber.BaseDepthData;
import com.hartwig.hmftools.common.genome.position.GenomePosition;

public class TumorContamination implements GenomePosition
{
    public final String Chromosome;
    public final int Position;
    public final BaseDepthData Normal;
    public final BaseDepthData Tumor;

    public TumorContamination(final String chromosome, final int position, final BaseDepthData normal, final BaseDepthData tumor)
    {
        Chromosome = chromosome;
        Position = position;
        Normal = normal;
        Tumor = tumor;
    }

    @Override
    public String chromosome() { return Chromosome; }
    public int position() { return Position; }

}
