package com.hartwig.hmftools.sage.phase;

import static java.lang.Math.max;
import static java.lang.Math.min;

import static com.hartwig.hmftools.common.region.BaseRegion.positionWithin;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.hartwig.hmftools.common.region.BaseRegion;
import com.hartwig.hmftools.sage.evidence.ReadContextCounter;

import org.apache.commons.compress.utils.Lists;

public class PhasedVariantGroup
{
    public final int Id;
    public final List<ReadContextCounter> PositiveReadCounters; // supported by the reads
    public final List<ReadContextCounter> NegativeReadCounters; // not supported by the reads

    public int ReadCount; // from uniquely supporting reads
    public double AllocatedReadCount; // allocated from subset groups

    // cache min and max variant positions from the positive read-counters, used for faster group matching
    private int mPosVariantMin;
    private int mPosVariantMax;

    private int mVariantMin;
    private int mVariantMax;
    private final List<Integer> mMergedIds;

    public PhasedVariantGroup(
            final int id, final int minVariantPos, final int maxVariantPos,
            final List<ReadContextCounter> posCounters, final List<ReadContextCounter> negCounters)
    {
        Id = id;
        PositiveReadCounters = posCounters;
        NegativeReadCounters = negCounters;

        mPosVariantMin = minVariantPos;
        mPosVariantMax = maxVariantPos;

        mVariantMin = minVariantPos;
        mVariantMax = maxVariantPos;

        if(!negCounters.isEmpty())
        {
            mVariantMin = min(mVariantMin, minPosition(negCounters, true));
            mVariantMax = max(mVariantMax, maxPosition(negCounters, true));
        }

        ReadCount = 1;
        AllocatedReadCount = 0;
        mMergedIds = Lists.newArrayList();
    }

    public int posVariantMin() { return mPosVariantMin; }
    public int posVariantMax() { return mPosVariantMax; }
    public int variantMin() { return mVariantMin; }
    public int variantMax() { return mVariantMax; }

    public boolean positionsOverlap(final PhasedVariantGroup other)
    {
        return BaseRegion.positionsOverlap(mPosVariantMin, mPosVariantMax, other.posVariantMin(), other.posVariantMax());
    }

    public boolean cullReadCounters(final Set<ReadContextCounter> validCounters)
    {
        // return true if the counters in this group have been culled
        Set<ReadContextCounter> localisedValidCounters = validCounters.stream()
                .filter(x -> positionWithin(x.position(), mVariantMin, mVariantMax)).collect(Collectors.toSet());

        if(localisedValidCounters.isEmpty())
            return true;

        List<ReadContextCounter> invalidPosCounters = PositiveReadCounters.stream().filter(x -> !localisedValidCounters.contains(x)).collect(Collectors.toList());
        List<ReadContextCounter> invalidNegCounters = NegativeReadCounters.stream().filter(x -> !localisedValidCounters.contains(x)).collect(Collectors.toList());

        if(invalidPosCounters.isEmpty() && invalidNegCounters.isEmpty())
            return false;

        if(invalidPosCounters.size() == PositiveReadCounters.size())
            return true;

        invalidPosCounters.forEach(x -> PositiveReadCounters.remove(x));
        invalidNegCounters.forEach(x -> NegativeReadCounters.remove(x));

        mPosVariantMin = minPosition(PositiveReadCounters, true);
        mPosVariantMax = maxPosition(PositiveReadCounters, true);

        mVariantMin = mPosVariantMin;
        mVariantMax = mPosVariantMax;

        if(!NegativeReadCounters.isEmpty())
        {
            mVariantMin = min(mVariantMin, minPosition(NegativeReadCounters, true));
            mVariantMax = max(mVariantMax, maxPosition(NegativeReadCounters, true));
        }

        return true;
    }

    public boolean isValid() { return !PositiveReadCounters.isEmpty() && PositiveReadCounters.size() + NegativeReadCounters.size() >= 2; }

    public boolean exactMatch(
            final int minVariantPos, final int maxVariantPos,
            final List<ReadContextCounter> posCounters, final List<ReadContextCounter> negCounters)
    {
        // both positives and negatives need to match exactly
        if(minVariantPos != mPosVariantMin || maxVariantPos != mPosVariantMax)
            return false;

        if(PositiveReadCounters.size() != posCounters.size() || NegativeReadCounters.size() != negCounters.size())
            return false;

        if(PositiveReadCounters.stream().anyMatch(x -> !posCounters.contains(x)))
            return false;

        if(NegativeReadCounters.stream().anyMatch(x -> !negCounters.contains(x)))
            return false;

        return true;
    }

    public boolean positivesMatch(final PhasedVariantGroup other)
    {
        return positivesMatch(other.posVariantMin(), other.posVariantMax(), other.PositiveReadCounters);
    }

    public boolean positivesMatch(final int minVariantPos, final int maxVariantPos, final List<ReadContextCounter> posCounters)
    {
        // positives need to match exactly, negatives don't
        if(minVariantPos != mPosVariantMin || maxVariantPos != mPosVariantMax)
            return false;

        if(PositiveReadCounters.size() != posCounters.size())
            return false;

        if(PositiveReadCounters.stream().anyMatch(x -> !posCounters.contains(x)))
            return false;

        return true;
    }

    public boolean isSubsetOf(final PhasedVariantGroup other)
    {
        // returns true if this group's positives are a subset of another without any contradictory negatives
        if(other.PositiveReadCounters.size() < PositiveReadCounters.size())
            return false;

        if(!PositiveReadCounters.stream().allMatch(x -> other.PositiveReadCounters.contains(x)))
            return false;

        if(hasAnyOverlap(other.PositiveReadCounters, NegativeReadCounters) || hasAnyOverlap(PositiveReadCounters, other.NegativeReadCounters))
            return false;

        return true;
    }

    public boolean populateCommon(final PhasedVariantGroup other, final List<ReadContextCounter> posCounters, final List<ReadContextCounter> negCounters)
    {
        // cannot have contradictory negatives
        if(hasAnyOverlap(other.PositiveReadCounters, NegativeReadCounters) || hasAnyOverlap(PositiveReadCounters, other.NegativeReadCounters))
            return false;

        PositiveReadCounters.stream().filter(x -> other.PositiveReadCounters.contains(x)).forEach(x -> posCounters.add(x));
        NegativeReadCounters.stream().filter(x -> other.NegativeReadCounters.contains(x)).forEach(x -> negCounters.add(x));
        return !posCounters.isEmpty();
    }

    public boolean hasCommonSubset(
            final PhasedVariantGroup other, final List<ReadContextCounter> posCounters, final List<ReadContextCounter> negCounters)
    {
        if(hasAnyOverlap(other.PositiveReadCounters, NegativeReadCounters) || hasAnyOverlap(PositiveReadCounters, other.NegativeReadCounters))
            return false;

        return posCounters.stream().allMatch(x -> PositiveReadCounters.contains(x))
                && negCounters.stream().allMatch(x -> NegativeReadCounters.contains(x));
    }

    private static boolean hasAnyOverlap(final List<ReadContextCounter> counters1, final List<ReadContextCounter> counters2)
    {
        return counters1.stream().anyMatch(x -> counters2.contains(x)) || counters2.stream().anyMatch(x -> counters1.contains(x));
    }

    public void merge(final PhasedVariantGroup other)
    {
        // simple merge without expanding the +ves
        if(other.PositiveReadCounters.size() == PositiveReadCounters.size())
            ReadCount += other.ReadCount;
        else
            AllocatedReadCount += other.ReadCount;

        AllocatedReadCount += other.AllocatedReadCount;

        mergeNegatives(other.NegativeReadCounters);
        mMergedIds.add(other.Id);
    }

    public void merge(final PhasedVariantGroup other, double allocFraction)
    {
        // merge and allocate as directed, expanding +ves as required
        AllocatedReadCount += (other.ReadCount + other.AllocatedReadCount) * allocFraction;

        // keep merged positive RCs in positional order
        for(ReadContextCounter readCounter : other.PositiveReadCounters)
        {
            boolean matched = false;
            int index = 0;

            while(index < PositiveReadCounters.size())
            {
                ReadContextCounter counter = PositiveReadCounters.get(index);
                if(counter == readCounter)
                {
                    matched = true;
                    break;
                }

                if(counter.position() <= readCounter.position())
                {
                    ++index;
                }
                else
                {
                    // new read counter needs to be inserted at this earlier position
                    break;
                }
            }

            if(!matched)
                PositiveReadCounters.add(index, readCounter);
        }

        mPosVariantMin = minPosition(PositiveReadCounters, true);
        mPosVariantMax = maxPosition(PositiveReadCounters, true);

        mergeNegatives(other.NegativeReadCounters);

        mMergedIds.add(other.Id);
    }

    public void mergeNegatives(final List<ReadContextCounter> negCounters)
    {
        negCounters.stream().filter(x -> !NegativeReadCounters.contains(x)).forEach(x -> NegativeReadCounters.add(x));

        if(!NegativeReadCounters.isEmpty())
        {
            mVariantMin = min(mPosVariantMin, minPosition(NegativeReadCounters, false));
            mVariantMax = max(mPosVariantMax, maxPosition(NegativeReadCounters, false));
        }
    }

    public static int minPosition(final List<ReadContextCounter> readCounters, boolean ordered)
    {
        if(ordered)
            return readCounters.get(0).position();
        else
            return readCounters.stream().mapToInt(x -> x.position()).min().orElse(0);
    }

    public static int maxPosition(final List<ReadContextCounter> readCounters, boolean ordered)
    {
        if(ordered)
            return readCounters.get(readCounters.size() - 1).position();
        else
            return readCounters.stream().mapToInt(x -> x.position()).max().orElse(0);
    }

    public String mergedGroupIds()
    {
        StringJoiner sj = new StringJoiner(";");
        mMergedIds.forEach(x -> sj.add(String.valueOf(x)));
        return sj.toString();
    }

    public static class PhasedGroupComparator implements Comparator<PhasedVariantGroup>
    {
        public int compare(final PhasedVariantGroup first, final PhasedVariantGroup second)
        {
            return first.posVariantMin() - second.posVariantMin();
        }
    }

    public String toString()
    {
        return String.format("%d: range(%d - %d) pos(%d) neg(%d) rc(%d) alloc(%.1f) fullRange(%s - %s)",
            Id, mPosVariantMin, mPosVariantMax, PositiveReadCounters.size(), NegativeReadCounters.size(), ReadCount, AllocatedReadCount,
                mVariantMin, mVariantMax);
    }
}
