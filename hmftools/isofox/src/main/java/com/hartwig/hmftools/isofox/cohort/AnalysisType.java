package com.hartwig.hmftools.isofox.cohort;

import static com.hartwig.hmftools.common.rna.AltSpliceJunctionFile.ALT_SJ_FILE_ID;
import static com.hartwig.hmftools.common.rna.CanonicalSpliceJunctionFile.CANONICAL_SJ_FILE_ID;
import static com.hartwig.hmftools.common.rna.GeneExpressionFile.GENE_EXPRESSION_FILE_ID;
import static com.hartwig.hmftools.common.rna.GeneExpressionFile.TRANSCRIPT_EXPRESSION_FILE_ID;
import static com.hartwig.hmftools.common.rna.GeneFusionFile.PASS_FUSION_FILE_ID;
import static com.hartwig.hmftools.common.rna.GeneFusionFile.UNFILTERED_FUSION_FILE_ID;
import static com.hartwig.hmftools.common.rna.RnaStatistics.SUMMARY_FILE_ID;
import static com.hartwig.hmftools.isofox.results.ResultsWriter.SPLICE_SITE_FILE;
import static com.hartwig.hmftools.isofox.unmapped.UmrFinder.UNMAPPED_READS_FILE_ID;

public enum AnalysisType
{
    SUMMARY, // reads in the BAM reading summary files and makes a single cohort file

    // novel junctions:
    ALT_SPLICE_JUNCTION, // combine and analyse alternate splice junctions for a cohort
    CANONICAL_SPLICE_JUNCTION, // combine and analyse canonical splice junctions for a cohort
    ALT_SPLICE_JUNCTION_MATRIX, // generates a for a cohort's alt-SJs
    SPLICE_VARIANT_MATCHING, // match alternate splice junctions with (candidate-splicing) somatic variants
    RECURRENT_SPLICE_VARIANTS, // find recurrent splice variants, input is a somatic table file
    SPLICE_SITE_PERCENTILES, // produce cohort percentage-spiced-in data from per-sample splice data files
    UNMAPPED_READS, // combine and analyse unmapped (soft-clipped) reads for a cohort

    // fusions:
    FUSION, // process fusions for a cohort - filter passing fusions, form a cohort file, compare with external fusions
    PASSING_FUSION, // produce per-sample filtered fusions
    RETAINED_INTRON, // produce cohort file for retained introns

    // expression
    EXPRESSION_DISTRIBUTION, // produce pan-cancer and per-cancer median and percentile expression data
    GENE_EXPRESSION_COMPARE, // compare gene expression across 2 cohorts of samples
    GENE_EXPRESSION_MATRIX, // generates a matrix for gene expression data
    PANEL_TPM_NORMALISATION, // normalises panel TPMs vs WGS
    TRANSCRIPT_EXPRESSION_MATRIX, // as above but for transcript expression
    EXTERNAL_EXPRESSION_COMPARE; // combine expression data from Isofox and another source

    public static String getIsofoxFileId(final AnalysisType type)
    {
        switch(type)
        {
            case GENE_EXPRESSION_COMPARE:
                return GENE_EXPRESSION_FILE_ID;

            case SUMMARY:
                return SUMMARY_FILE_ID;

            case ALT_SPLICE_JUNCTION:
            case SPLICE_VARIANT_MATCHING:
            case ALT_SPLICE_JUNCTION_MATRIX:
                return ALT_SJ_FILE_ID;

            case CANONICAL_SPLICE_JUNCTION:
                return CANONICAL_SJ_FILE_ID;

            case UNMAPPED_READS:
                return UNMAPPED_READS_FILE_ID;

            case SPLICE_SITE_PERCENTILES:
                return SPLICE_SITE_FILE;

            case FUSION:
                return UNFILTERED_FUSION_FILE_ID;

            case PASSING_FUSION:
                return PASS_FUSION_FILE_ID;

            case GENE_EXPRESSION_MATRIX:
            case PANEL_TPM_NORMALISATION:
                return GENE_EXPRESSION_FILE_ID;

            case TRANSCRIPT_EXPRESSION_MATRIX:
                return TRANSCRIPT_EXPRESSION_FILE_ID;

            default:
                return "";
        }
    }
}
