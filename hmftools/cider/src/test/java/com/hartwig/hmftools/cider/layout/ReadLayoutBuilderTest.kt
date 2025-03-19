package com.hartwig.hmftools.cider.layout

import com.hartwig.hmftools.cider.ReadKey
import com.hartwig.hmftools.cider.TestUtils
import htsjdk.samtools.SAMUtils
import htsjdk.samtools.util.SequenceUtil.A
import htsjdk.samtools.util.SequenceUtil.T
import htsjdk.samtools.util.SequenceUtil.C
import htsjdk.samtools.util.SequenceUtil.G

import kotlin.test.*

class ReadLayoutBuilderTest
{
    companion object
    {
        const val MIN_BASE_QUALITY = 30.toByte()
    }

    @Test
    fun testCompareReads1()
    {
        val seq1 = "CAGGTGCAGCTGGTGGAGTCTGGGGGA"
        val seq2 = "GAGGTGCAGCTGGTAGAGTCTGGGAGA"
        val baseQual1 = ByteArray(seq1.length){ 50 }
        val baseQual2 = baseQual1

        val (matchCount, compareCount) = ReadLayoutBuilder.sequenceMatchCount(seq1, seq2, baseQual1, baseQual2,
            0, 0, 20, MIN_BASE_QUALITY)

        assertEquals(18, matchCount)
    }

    @Test
    fun testCompareReads2()
    {
        val seq1 = "CAGGTGCAGCTGGTGGAGTCTGGGGGA"
        val seq2 = "GTGCAGCTGGTAGAGTCTGGGAGA"
        val baseQual1 = ByteArray(seq1.length){ 50 }
        val baseQual2 = baseQual1

        val (matchCount, compareCount) = ReadLayoutBuilder.sequenceMatchCount(seq1, seq2, baseQual1, baseQual2,
            3, 0, 20, MIN_BASE_QUALITY)

        assertEquals(19, matchCount)
    }

    @Test
    fun testAddToLayout()
    {
        val group = ReadLayout()
        var seq = "CAGGTG"

        // we are aligned at the T
        var readData = TestLayoutRead("read1", ReadKey("read1", true), seq.toByteArray(), ByteArray(seq.length){ 50 }, 4)
        group.addRead(readData, MIN_BASE_QUALITY)

        assertEquals(seq, group.consensusSequenceString())
        assertEquals(4, group.alignedPosition)

        // now test a sequence that extend the start by 3 bases
        seq = "AGCCAGGT"
        readData = TestLayoutRead("read2", ReadKey("read2", true), seq.toByteArray(), ByteArray(seq.length){ 50 }, 7)
        group.addRead(readData, MIN_BASE_QUALITY)

        assertEquals(seq + "G", group.consensusSequenceString())
        assertEquals(7, group.alignedPosition)

        // now test a sequence that extend the end by 3 bases
        seq = "AGGTGCAA"
        readData = TestLayoutRead("read3", ReadKey("read3", true), seq.toByteArray(), ByteArray(seq.length){ 50 }, 3)
        group.addRead(readData, MIN_BASE_QUALITY)

        assertEquals("AGCC" + seq, group.consensusSequenceString())
        assertEquals(7, group.alignedPosition)

        assertEquals(3, group.reads.size)

        // lets see if we got the correct support counts
        assertEquals(mapOf(A to 1), group.highQualSequenceSupport.support[0].generateCountMap())
        assertEquals(mapOf(G to 1), group.highQualSequenceSupport.support[1].generateCountMap())
        assertEquals(mapOf(C to 1), group.highQualSequenceSupport.support[2].generateCountMap())
        assertEquals(mapOf(C to 2), group.highQualSequenceSupport.support[3].generateCountMap())
        assertEquals(mapOf(A to 3), group.highQualSequenceSupport.support[4].generateCountMap())
        assertEquals(mapOf(G to 3), group.highQualSequenceSupport.support[5].generateCountMap())
        assertEquals(mapOf(G to 3), group.highQualSequenceSupport.support[6].generateCountMap())
        assertEquals(mapOf(T to 3), group.highQualSequenceSupport.support[7].generateCountMap())
        assertEquals(mapOf(G to 2), group.highQualSequenceSupport.support[8].generateCountMap())
        assertEquals(mapOf(C to 1), group.highQualSequenceSupport.support[9].generateCountMap())
        assertEquals(mapOf(A to 1), group.highQualSequenceSupport.support[10].generateCountMap())
        assertEquals(mapOf(A to 1), group.highQualSequenceSupport.support[11].generateCountMap())
    }

    @Test
    fun testLayoutMatch()
    {
        val layout = ReadLayout()
        var seq = "CAGGTG"
        var baseQual = SAMUtils.fastqToPhred("FF::FF") // F is 37, : is 25

        // we are aligned at the T
        var readData = TestLayoutRead("read1", ReadKey("read1", true), seq.toByteArray(), baseQual, 4)
        layout.addRead(readData, MIN_BASE_QUALITY)

        assertEquals(seq, layout.consensusSequenceString())
        assertEquals(4, layout.alignedPosition)

        // match a new sequence against the overlay
        seq = "CAGGTG"
        readData = TestLayoutRead("read2", ReadKey("read2", true), seq.toByteArray(), baseQual, 4)

        // this should match
        assertTrue(ReadLayoutBuilder.layoutMatch(layout, readData, MIN_BASE_QUALITY, 6))

        // this fails due to overlap
        assertFalse(ReadLayoutBuilder.layoutMatch(layout, readData, MIN_BASE_QUALITY, 7))

        // match another one which should not match
        seq = "CTGGTG"
        readData = TestLayoutRead("read2", ReadKey("read2", true), seq.toByteArray(), baseQual, 4)
        assertFalse(ReadLayoutBuilder.layoutMatch(layout, readData, MIN_BASE_QUALITY, 6))

        // now try to match a shorter sequence, should match also
        seq = "GGTG"
        baseQual = SAMUtils.fastqToPhred("FFFF") // F is 37, : is 25
        readData = TestLayoutRead("read2", ReadKey("read2", true), seq.toByteArray(), baseQual, 2)
        assertTrue(ReadLayoutBuilder.layoutMatch(layout, readData, MIN_BASE_QUALITY, 4))
    }

    @Test
    fun testLayoutNegativeReadAlignedPosition()
    {
        val layout = ReadLayout()
        var seq = "CAGGTG"
        var baseQual = "FFFFFF" // F is 37, : is 25

        // we are aligned at the A
        var readData = TestUtils.createLayoutRead("read1", seq, baseQual, 1)
        layout.addRead(readData, MIN_BASE_QUALITY)

        // another sequence which does not include A, A is actually 2 positions before the start of sequence
        // the GTG part matches
        seq = "GTGCC"
        baseQual = "FFFFF"
        readData = TestUtils.createLayoutRead("read2", seq, baseQual, -2)
        assertTrue(ReadLayoutBuilder.layoutMatch(layout, readData, MIN_BASE_QUALITY, 3))

        // if I move the aligned position it will not match
        val mismatchRead = TestUtils.createLayoutRead("read2", seq, baseQual, -1)
        assertFalse(ReadLayoutBuilder.layoutMatch(layout, mismatchRead, MIN_BASE_QUALITY, 3))

        // try add to layout
        layout.addRead(readData, MIN_BASE_QUALITY)

        assertEquals("CAGGTGCC", layout.consensusSequenceString())
        assertEquals("11122211", layout.highQualSupportString())
    }

    @Test
    fun testLayoutNegativeLayoutAlignedPosition()
    {
        val layout = ReadLayout()
        var seq = "CAGGTG"
        var baseQual = "FFFFFF" // F is 37, : is 25

        // we are aligned at 2 bases before the start of layout
        val read1 = TestUtils.createLayoutRead("read1", seq, baseQual, -2)
        layout.addRead(read1, MIN_BASE_QUALITY)

        assertEquals(seq, layout.consensusSequenceString())
        assertEquals(-2, layout.alignedPosition)

        // we can still align reads normally
        seq = "TCAGGTGA"
        baseQual = "FFFFFFFF"
        val read2 = TestUtils.createLayoutRead("read2", seq, baseQual, -1)
        assertTrue(ReadLayoutBuilder.layoutMatch(layout, read2, MIN_BASE_QUALITY, 6))
        assertFalse(ReadLayoutBuilder.layoutMatch(layout, read2, MIN_BASE_QUALITY, 7))

        // we can also align reads that do not have negative aligned position
        seq = "ATTCAGGTG"
        baseQual = "FFFFFFFFF"
        val read3 = TestUtils.createLayoutRead("read3", seq, baseQual, 1)
        assertTrue(ReadLayoutBuilder.layoutMatch(layout, read3, MIN_BASE_QUALITY, 6))

        // try adding those to layout
        layout.addRead(read2, MIN_BASE_QUALITY)
        assertEquals("TCAGGTGA", layout.consensusSequenceString())
        assertEquals("12222221", layout.highQualSupportString())

        layout.addRead(read3, MIN_BASE_QUALITY)
        assertEquals("ATTCAGGTGA", layout.consensusSequenceString())
        assertEquals("1123333331", layout.highQualSupportString())
    }

    @Test
    fun testLayoutSupport()
    {
        val group = ReadLayout()
        var seq = "CAGGTG"

        // we are aligned at the T
        var readData = TestLayoutRead("read1", ReadKey("read1", true), seq.toByteArray(), ByteArray(seq.length){ 50 }, 4)
        group.addRead(readData, MIN_BASE_QUALITY)

        assertEquals(seq, group.consensusSequenceString())
        assertEquals(4, group.alignedPosition)

        // match a new sequence against the overlay
        seq = "AGCCAGAT"
        readData = TestLayoutRead("read2", ReadKey("read2", true), seq.toByteArray(), ByteArray(seq.length){ 50 }, 7)

        //val (matchCount, compareCount) = ReadLayoutBuilder.layoutMatchCount(group, readData, false, MIN_BASE_QUALITY)

        //assertEquals(4, matchCount)
        //assertEquals(5, compareCount)
    }

    @Test
    fun testLayoutMerge()
    {
        val layout1 = ReadLayout()
        var seq1 = "CAGGTG"
        val baseQual1 = SAMUtils.fastqToPhred("FF::FF") // F is 37, : is 25

        // we are aligned at the T
        var read1 = TestLayoutRead("read1", ReadKey("read1", true), seq1.toByteArray(), baseQual1, 4)
        layout1.addRead(read1, MIN_BASE_QUALITY)

        val layout2 = ReadLayout()
        var seq2 = "AGGTGAT"
        val baseQual2 = SAMUtils.fastqToPhred("F:FFFF:") // F is 37, : is 25

        // aligned at the first A
        var read2 = TestLayoutRead("read2", ReadKey("read2", true), seq2.toByteArray(), baseQual2, 0)
        layout2.addRead(read2, MIN_BASE_QUALITY)

        // now we have 2 layouts, one is GAGGTG, another is AGGTGAT, we merge them together to create CAGGTGAT
        // and we want to align them at the 2nd G
        val layout3 = ReadLayout.merge(layout1, layout2, -1, 2, MIN_BASE_QUALITY)

        assertEquals("CAGGTGAT", layout3.consensusSequenceString())
        assertEquals("12012210", layout3.highQualSupportString())
        //assertEquals(5, compareCount)
    }
}
