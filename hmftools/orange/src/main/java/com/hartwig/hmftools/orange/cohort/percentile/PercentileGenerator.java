package com.hartwig.hmftools.orange.cohort.percentile;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.hartwig.hmftools.orange.cohort.datamodel.Observation;
import com.hartwig.hmftools.orange.cohort.mapping.CohortConstants;
import com.hartwig.hmftools.orange.cohort.mapping.CohortMapper;

import org.jetbrains.annotations.NotNull;

public class PercentileGenerator
{
    @NotNull
    private final CohortMapper cohortMapper;

    public PercentileGenerator(@NotNull final CohortMapper cohortMapper)
    {
        this.cohortMapper = cohortMapper;
    }

    @NotNull
    public List<CohortPercentiles> run(@NotNull List<Observation> observations)
    {
        Multimap<String, Double> valuesPerCancerType = ArrayListMultimap.create();
        for(Observation observation : observations)
        {
            String cancerType = cohortMapper.cancerTypeForSample(observation.sample());
            if(cancerType != null)
            {
                valuesPerCancerType.put(cancerType, observation.value());
            }
        }

        List<CohortPercentiles> percentiles = Lists.newArrayList();
        if(!valuesPerCancerType.isEmpty())
        {
            percentiles.add(toPercentiles(CohortConstants.COHORT_PAN_CANCER, valuesPerCancerType.values()));
            for(Map.Entry<String, Collection<Double>> entry : valuesPerCancerType.asMap().entrySet())
            {
                percentiles.add(toPercentiles(entry.getKey(), entry.getValue()));
            }
        }

        return percentiles;
    }

    @NotNull
    private static CohortPercentiles toPercentiles(@NotNull String cancerType, @NotNull Collection<Double> values)
    {
        List<Double> sorted = Lists.newArrayList(values);
        sorted.sort(Comparator.naturalOrder());

        List<Double> percentiles = Lists.newArrayList();
        double baseIndex = (double) sorted.size() / PercentileConstants.BUCKET_COUNT;

        percentiles.add(sorted.get(0));
        for(int i = 1; i < PercentileConstants.BUCKET_COUNT - 1; i++)
        {
            int index = (int) Math.min(sorted.size() - 1, Math.round(((i + 0.49) * baseIndex)));
            percentiles.add(sorted.get(index));
        }
        percentiles.add(sorted.get(sorted.size() - 1));

        return ImmutableCohortPercentiles.builder().cancerType(cancerType).cohortSize(sorted.size()).values(percentiles).build();
    }
}
