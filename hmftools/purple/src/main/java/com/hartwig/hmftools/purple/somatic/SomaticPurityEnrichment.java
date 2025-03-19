package com.hartwig.hmftools.purple.somatic;

import static java.lang.Math.min;

import static com.hartwig.hmftools.common.variant.PurpleVcfTags.PURPLE_AF;
import static com.hartwig.hmftools.common.variant.PurpleVcfTags.PURPLE_BIALLELIC_FLAG;
import static com.hartwig.hmftools.common.variant.PurpleVcfTags.PURPLE_CN;
import static com.hartwig.hmftools.common.variant.PurpleVcfTags.PURPLE_GERMLINE_INFO;
import static com.hartwig.hmftools.common.variant.PurpleVcfTags.PURPLE_MINOR_ALLELE_CN_INFO;
import static com.hartwig.hmftools.common.variant.PurpleVcfTags.PURPLE_VARIANT_CN;
import static com.hartwig.hmftools.purple.config.PurpleConstants.BIALLELIC_PROBABILITY;

import java.util.List;
import java.util.Optional;

import com.hartwig.hmftools.common.genome.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.genome.region.GenomeRegionSelector;
import com.hartwig.hmftools.common.genome.region.GenomeRegionSelectorFactory;
import com.hartwig.hmftools.common.purple.GermlineStatus;
import com.hartwig.hmftools.common.utils.Doubles;
import com.hartwig.hmftools.common.utils.collection.Multimaps;
import com.hartwig.hmftools.purple.purity.PurityAdjuster;
import com.hartwig.hmftools.common.purple.PurpleCopyNumber;
import com.hartwig.hmftools.purple.region.ObservedRegion;

import org.apache.commons.math3.distribution.PoissonDistribution;

import htsjdk.variant.variantcontext.VariantContext;

public class SomaticPurityEnrichment
{
    private final PurityAdjuster mPurityAdjuster;
    private final GenomeRegionSelector<PurpleCopyNumber> mCopyNumberSelector;
    private final GenomeRegionSelector<ObservedRegion> mObservedRegionSelector;

    public SomaticPurityEnrichment(
            final PurityAdjuster purityAdjuster, final List<PurpleCopyNumber> copyNumbers, final List<ObservedRegion> fittedRegions)
    {
        mPurityAdjuster = purityAdjuster;
        mCopyNumberSelector = GenomeRegionSelectorFactory.createImproved(Multimaps.fromRegions(copyNumbers));
        mObservedRegionSelector = GenomeRegionSelectorFactory.createImproved(Multimaps.fromRegions(fittedRegions));
    }

    public void processVariant(final SomaticVariant variant)
    {
        if(!HumanChromosome.contains(variant.chromosome()))
            return;

        Optional<ObservedRegion> observedRegion = mObservedRegionSelector.select(variant);
        GermlineStatus germlineStatus = GermlineStatus.UNKNOWN;

        if(observedRegion.isPresent())
            germlineStatus = observedRegion.get().germlineStatus();

        variant.context().getCommonInfo().putAttribute(PURPLE_GERMLINE_INFO, germlineStatus.toString());

        if(variant.hasTumorAlleleDepth())
        {
            Optional<PurpleCopyNumber> purpleCopyNumber = mCopyNumberSelector.select(variant);
            if(purpleCopyNumber.isPresent())
            {
                applyPurityAdjustment(variant, purpleCopyNumber.get(), germlineStatus == GermlineStatus.HET_DELETION);
            }
        }
    }

    private void applyPurityAdjustment(final SomaticVariant variant, final PurpleCopyNumber purpleCopyNumber, boolean isGermlineHetDeletion)
    {
        double copyNumber = purpleCopyNumber.averageTumorCopyNumber();

        double vaf = mPurityAdjuster.purityAdjustedVAF(
                purpleCopyNumber.chromosome(), Math.max(0.001, copyNumber), variant.alleleFrequency(), isGermlineHetDeletion);

        double variantCopyNumber = Math.max(0, vaf * copyNumber);

        boolean biallelic = isBiallelic(variant, purpleCopyNumber, variantCopyNumber);

        VariantContext variantContext = variant.context();

        variantContext.getCommonInfo().putAttribute(PURPLE_VARIANT_CN, variantCopyNumber);
        variantContext.getCommonInfo().putAttribute(PURPLE_CN, copyNumber);

        variantContext.getCommonInfo().putAttribute(PURPLE_AF, String.format("%.4f", vaf));
        variantContext.getCommonInfo().putAttribute(PURPLE_MINOR_ALLELE_CN_INFO, purpleCopyNumber.minorAlleleCopyNumber());
        variantContext.getCommonInfo().putAttribute(PURPLE_BIALLELIC_FLAG, biallelic);
    }

    private static boolean isBiallelic(final SomaticVariant variant, final PurpleCopyNumber purpleCopyNumber, double variantCopyNumber)
    {
        /* if the minorAlleleCN > 0.5 then a biallelic state should not generally be possible unless either the minorAlleleCN is measured incorrectly
        or the variant is on both alleles. Therefore, we add an extra check for biallelic:

        If minorAlleleCopyNumber > 0.5 then only call as biallelic if:
            Poisson(AlleleReadCount / variantCN * [CN – min(1,minorAlleleCN)],AlleleReadCount)<0.005
        */
        double copyNumber = purpleCopyNumber.averageTumorCopyNumber();

        if(Doubles.greaterThan(copyNumber, 0) && copyNumber - variantCopyNumber >= 0.5)
            return false;

        double minorAlleleCopyNumber = purpleCopyNumber.minorAlleleCopyNumber();
        if(minorAlleleCopyNumber < 0.5)
            return true;

        int alleleReadCount = variant.alleleReadCount();

        double expectedAlleleReadCount = alleleReadCount / variantCopyNumber * (copyNumber - min(1, minorAlleleCopyNumber));
        PoissonDistribution poissonDist = new PoissonDistribution(expectedAlleleReadCount);
        double poissonProb = 1 - poissonDist.cumulativeProbability(alleleReadCount - 1);

        return poissonProb < BIALLELIC_PROBABILITY;
    }
}
