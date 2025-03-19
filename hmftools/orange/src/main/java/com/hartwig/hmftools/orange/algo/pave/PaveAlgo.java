package com.hartwig.hmftools.orange.algo.pave;

import static com.hartwig.hmftools.orange.OrangeApplication.LOGGER;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache;
import com.hartwig.hmftools.common.gene.ExonData;
import com.hartwig.hmftools.common.gene.GeneData;
import com.hartwig.hmftools.common.gene.TranscriptData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PaveAlgo
{
    private static final int MAX_SPLICE_DISTANCE = 8;

    @NotNull
    private final EnsemblDataCache ensemblDataCache;
    private final boolean expectGenesExist;

    public PaveAlgo(@NotNull final EnsemblDataCache ensemblDataCache, final boolean expectGenesExist)
    {
        this.ensemblDataCache = ensemblDataCache;
        this.expectGenesExist = expectGenesExist;
    }

    @Nullable
    public PaveEntry run(@NotNull String gene, @NotNull String transcript, int position)
    {
        TranscriptData transcriptData = findTranscript(gene, transcript);
        if(transcriptData == null)
        {
            return null;
        }

        ExonData affectedExon = findAffectedExon(transcriptData.exons(), position);
        if(affectedExon == null)
        {
            // Non-exonic variant and hence not affecting a codon.
            return createEntry(null, null);
        }

        Integer affectedCodon = findAffectedCodon(transcriptData, position);
        return createEntry(affectedCodon, affectedExon.Rank);
    }

    @Nullable
    private TranscriptData findTranscript(@NotNull String gene, @NotNull String transcriptId)
    {
        GeneData geneData = ensemblDataCache.getGeneDataByName(gene);
        if(geneData == null)
        {
            if(!gene.isEmpty() && expectGenesExist)
            {
                LOGGER.warn("Could not resolve gene against ensembl data cache: '{}'", gene);
            }
            return null;
        }

        TranscriptData transcript = ensemblDataCache.getTranscriptData(geneData.GeneId, transcriptId);
        if(transcript == null)
        {
            LOGGER.warn("Could not resolve transcript '{}' against ensembl data cache for gene '{}'", transcriptId, geneData.GeneName);
        }
        return transcript;
    }

    @Nullable
    @VisibleForTesting
    static ExonData findAffectedExon(@NotNull List<ExonData> exons, int position)
    {
        for(ExonData exon : exons)
        {
            if(liesInExon(exon, position))
            {
                return exon;
            }
        }

        // TODO Properly assign INDELs to exons in case they start too far away from the splice region but affect it nonetheless.

        // In theory an intron could be so small that a variant belongs to multiple exons.
        // In practice this is rare enough to ignore (it does happen on position 223183490 in pseudogene CT75 though).
        for(ExonData exon : exons)
        {
            if(liesInExonSpliceRegion(exon, position))
            {
                return exon;
            }
        }

        return null;
    }

    @Nullable
    @VisibleForTesting
    static Integer findAffectedCodon(@NotNull TranscriptData transcript, int position)
    {
        Integer codingStart = transcript.CodingStart;
        Integer codingEnd = transcript.CodingEnd;
        if(codingStart == null || codingEnd == null)
        {
            // Non-coding transcript
            return null;
        }

        if(position < codingStart || position > codingEnd)
        {
            // UTR variant.
            return null;
        }

        int currentCodingBases = 0;

        for(ExonData exon : codingCorrected(sortByRank(transcript.exons()), codingStart, codingEnd))
        {
            if(liesInExon(exon, position))
            {
                int exonStart = transcript.posStrand() ? exon.Start : exon.End;
                currentCodingBases += Math.abs(position - exonStart);
                return 1 + (int) Math.round((currentCodingBases - currentCodingBases % 3) / 3D);
            }
            else
            {
                currentCodingBases += (1 + exon.End - exon.Start);
            }
        }

        // Intronic variant
        return null;
    }

    @NotNull
    private static List<ExonData> codingCorrected(@NotNull List<ExonData> exons, int codingStart, int codingEnd)
    {
        List<ExonData> codingCorrectedExons = Lists.newArrayList();
        for(ExonData exon : exons)
        {
            boolean isFullyCoding = exon.Start >= codingStart && exon.End <= codingEnd;
            boolean isPartiallyStartUtr = exon.Start < codingStart && exon.End >= codingStart;
            boolean isPartiallyEndUtr = exon.End > codingEnd && exon.Start <= codingEnd;
            if(isFullyCoding || isPartiallyStartUtr || isPartiallyEndUtr)
            {
                codingCorrectedExons.add(new ExonData(exon.TransId,
                        Math.max(exon.Start, codingStart),
                        Math.min(exon.End, codingEnd),
                        exon.Rank,
                        exon.PhaseStart,
                        exon.PhaseEnd));
            }
        }
        return codingCorrectedExons;
    }

    @NotNull
    private static List<ExonData> sortByRank(@NotNull List<ExonData> exons)
    {
        return exons.stream().sorted(Comparator.comparingInt(exon -> exon.Rank)).collect(Collectors.toList());
    }

    private static boolean liesInExon(@NotNull ExonData exon, int position)
    {
        return position >= exon.Start && position <= exon.End;
    }

    private static boolean liesInExonSpliceRegion(@NotNull ExonData exon, int position)
    {
        int spliceRegionStart = exon.Start - MAX_SPLICE_DISTANCE;
        int spliceRegionEnd = exon.End + MAX_SPLICE_DISTANCE;

        boolean liesInSpliceStart = position >= spliceRegionStart && position < exon.Start;
        boolean liesInSpliceEnd = position > exon.End && position <= spliceRegionEnd;

        return liesInSpliceStart || liesInSpliceEnd;
    }

    @NotNull
    private static PaveEntry createEntry(@Nullable Integer affectedCodon, @Nullable Integer affectedExon)
    {
        return ImmutablePaveEntry.builder().affectedCodon(affectedCodon).affectedExon(affectedExon).build();
    }
}
