package com.hartwig.hmftools.common.genome.gc;

public final class GcCalcs
{
    public static double calcGcPercent(final String sequence)
    {
        if(sequence.isEmpty())
            return 0;

        int gcCount = 0;
        for(int i = 0; i < sequence.length(); ++i)
        {
            if(sequence.charAt(i) == 'G' || sequence.charAt(i) == 'C')
                ++gcCount;
        }

        return gcCount / (double)sequence.length();
    }
}
