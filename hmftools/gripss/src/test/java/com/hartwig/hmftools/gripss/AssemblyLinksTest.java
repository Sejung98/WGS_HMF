package com.hartwig.hmftools.gripss;

import static com.hartwig.hmftools.common.test.GeneTestUtils.CHR_1;
import static com.hartwig.hmftools.common.test.GeneTestUtils.CHR_2;
import static com.hartwig.hmftools.common.utils.sv.SvCommonUtils.NEG_ORIENT;
import static com.hartwig.hmftools.common.utils.sv.SvCommonUtils.POS_ORIENT;
import static com.hartwig.hmftools.gripss.GripssTestUtils.buildLinkAttributes;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.gripss.common.SvData;
import com.hartwig.hmftools.gripss.links.AssemblyLinks;
import com.hartwig.hmftools.gripss.links.Link;
import com.hartwig.hmftools.gripss.links.LinkStore;

import org.junit.Test;

public class AssemblyLinksTest
{
    private final GripssTestApp mGripss;

    public AssemblyLinksTest()
    {
        mGripss = new GripssTestApp();
    }

    @Test
    public void testAssemblyLinks()
    {
        // basic link
        SvData var1 = mGripss.createDel(CHR_1, 1000, 2000, null, buildLinkAttributes("asm1", "325"));
        SvData var2 = mGripss.createDel(CHR_1, 2100, 3000, buildLinkAttributes("asm1", "325"), null);

        LinkStore assemblyLinks = AssemblyLinks.buildAssembledLinks(Lists.newArrayList(var1, var2));
        assertNull(assemblyLinks.getBreakendLinks(var1.breakendStart()));
        assertNotNull(assemblyLinks.getBreakendLinks(var1.breakendEnd()));
        assertNotNull(assemblyLinks.getBreakendLinks(var2.breakendStart()));
        assertNull(assemblyLinks.getBreakendLinks(var2.breakendEnd()));

        assemblyLinks.clear();

        // ignore same SV, eg a DUP - though these should not be given the same assembly data in practice
        var1 = mGripss.createDup(CHR_1, 1000, 2000, buildLinkAttributes("asm1", "325"), buildLinkAttributes("asm1", "325"));

        assemblyLinks = AssemblyLinks.buildAssembledLinks(Lists.newArrayList(var1));
        assertTrue(assemblyLinks.getBreakendLinksMap().isEmpty());

        assemblyLinks.clear();

        // ignore non-facing breakends or those on different chromosome
        var1 = mGripss.createInv(CHR_1, 1000, 2000, NEG_ORIENT, null, buildLinkAttributes("asm1", "325"));
        var2 = mGripss.createInv(CHR_2, 3000, 4000, POS_ORIENT, buildLinkAttributes("asm1", "325"), null);

        assemblyLinks = AssemblyLinks.buildAssembledLinks(Lists.newArrayList(var1, var2));
        assertTrue(assemblyLinks.getBreakendLinksMap().isEmpty());

        // same chromosome is fine
        var1 = mGripss.createInv(CHR_2, 1000, 2000, NEG_ORIENT, null, buildLinkAttributes("asm1", "325"));
        var2 = mGripss.createInv(CHR_2, 3000, 4000, POS_ORIENT, buildLinkAttributes("asm1", "325"), null);

        assemblyLinks = AssemblyLinks.buildAssembledLinks(Lists.newArrayList(var1, var2));
        assertNotNull(assemblyLinks.getBreakendLinks(var1.breakendEnd()));
        assertNotNull(assemblyLinks.getBreakendLinks(var2.breakendStart()));

        // ignore incorrect orientations and facing away
        var1 = mGripss.createInv(CHR_1, 1000, 2000, POS_ORIENT, null, buildLinkAttributes("asm1", "325"));
        var2 = mGripss.createInv(CHR_1, 3000, 4000, NEG_ORIENT, buildLinkAttributes("asm1", "325"), null);

        assemblyLinks = AssemblyLinks.buildAssembledLinks(Lists.newArrayList(var1, var2));
        assertTrue(assemblyLinks.getBreakendLinksMap().isEmpty());

        var1 = mGripss.createInv(CHR_1, 1000, 2000, POS_ORIENT, null, buildLinkAttributes("asm1", "325"));
        var2 = mGripss.createInv(CHR_1, 3000, 4000, POS_ORIENT, buildLinkAttributes("asm1", "325"), null);

        assemblyLinks = AssemblyLinks.buildAssembledLinks(Lists.newArrayList(var1, var2));
        assertTrue(assemblyLinks.getBreakendLinksMap().isEmpty());

        // a breakend cannot assembly to the same other breakend, but can to another breakend
        SvData var3 = mGripss.createInv(
                CHR_2, 1000, 2000, NEG_ORIENT, null,
                buildLinkAttributes("asm12_01,asm12_02,asm13-01,asm13-02", "1,2,3,4"));

        SvData var4 = mGripss.createInv(
                CHR_2, 2100, 4200, POS_ORIENT,
                buildLinkAttributes("asm12_01,asm12_02", "1,2"),
                buildLinkAttributes("asm23-01,asm23-02,asm23-03", "5,6,7"));

        SvData var5 = mGripss.createDel(
                CHR_2, 2200, 4100,
                buildLinkAttributes("asm13-01,asm13-02", "3,4"),
                buildLinkAttributes("asm23-01,asm23-02,asm23-03", "5,6,7"));

        assemblyLinks = AssemblyLinks.buildAssembledLinks(Lists.newArrayList(var3, var4, var5));

        List<Link> links = assemblyLinks.getBreakendLinks(var3.breakendEnd());
        assertEquals(2, links.size());
        assertTrue(links.stream().anyMatch(x -> x.breakendEnd() == var4.breakendStart()));
        assertTrue(links.stream().anyMatch(x -> x.breakendEnd() == var5.breakendStart()));

        links = assemblyLinks.getBreakendLinks(var4.breakendEnd());
        assertEquals(1, links.size());
        assertTrue(links.stream().anyMatch(x -> x.breakendEnd() == var5.breakendEnd()));

        links = assemblyLinks.getBreakendLinks(var5.breakendEnd());
        assertEquals(1, links.size());
        assertTrue(links.stream().anyMatch(x -> x.breakendEnd() == var4.breakendEnd()));
    }
}
