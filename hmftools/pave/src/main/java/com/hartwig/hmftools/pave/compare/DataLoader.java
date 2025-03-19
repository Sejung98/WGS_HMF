package com.hartwig.hmftools.pave.compare;

import static com.hartwig.hmftools.common.utils.file.FileReaderUtils.createFieldsIndexMap;
import static com.hartwig.hmftools.common.variant.CodingEffect.NONE;
import static com.hartwig.hmftools.pave.PaveConfig.PV_LOGGER;
import static com.hartwig.hmftools.pave.VariantData.NO_LOCAL_PHASE_SET;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.variant.CodingEffect;
import com.hartwig.hmftools.common.variant.Hotspot;
import com.hartwig.hmftools.common.variant.VariantType;

public final class DataLoader
{
    public static Map<String,List<RefVariantData>> processRefVariantFile(final String filename)
    {
        Map<String,List<RefVariantData>> sampleVariantsMap = Maps.newHashMap();

        if(filename == null)
            return sampleVariantsMap;

        try
        {
            BufferedReader fileReader = new BufferedReader(new FileReader(filename));
            String header = fileReader.readLine();

            final String fileDelim = "\t";
            final Map<String,Integer> fieldsIndexMap = createFieldsIndexMap(header, fileDelim);

            int sampleIndex = fieldsIndexMap.get("sampleId");

            int chrIndex = fieldsIndexMap.get("chromosome");
            int posIndex = fieldsIndexMap.get("position");
            int refIndex = fieldsIndexMap.get("ref");
            int altIndex = fieldsIndexMap.get("alt");
            int typeIndex = fieldsIndexMap.get("type");
            int geneIndex = fieldsIndexMap.get("gene");
            int canonicalEffectIndex = fieldsIndexMap.get("canonicalEffect");
            int canonicalCodingEffectIndex = fieldsIndexMap.get("canonicalCodingEffect");
            int worstCodingEffectIndex = fieldsIndexMap.get("worstCodingEffect");
            int canonicalHgvsCodingImpactIndex = fieldsIndexMap.get("canonicalHgvsCodingImpact");
            int canonicalHgvsProteinImpactIndex = fieldsIndexMap.get("canonicalHgvsProteinImpact");
            int microhomologyIndex = fieldsIndexMap.get("microhomology");
            int repeatSequenceIndex = fieldsIndexMap.get("repeatSequence");
            int repeatCountIndex = fieldsIndexMap.get("repeatCount");
            int localPhaseSetIndex = fieldsIndexMap.get("localPhaseSet");
            int reportedIndex = fieldsIndexMap.get("reported");
            Integer hotspotIndex = fieldsIndexMap.get("hotspot");

            List<RefVariantData> variants = null;

            String line = "";

            while((line = fileReader.readLine()) != null)
            {
                final String[] items = line.split(fileDelim, -1);

                String sampleId = items[sampleIndex];

                variants = sampleVariantsMap.get(sampleId);
                if(variants == null)
                {
                    variants = Lists.newArrayList();
                    sampleVariantsMap.put(sampleId, variants);
                }

                int localPhaseSet = !items[localPhaseSetIndex].equals("NULL") ? Integer.parseInt(items[localPhaseSetIndex]) : NO_LOCAL_PHASE_SET;

                try
                {
                    RefVariantData variant = new RefVariantData(
                            items[chrIndex], Integer.parseInt(items[posIndex]), items[refIndex], items[altIndex],
                            VariantType.valueOf(items[typeIndex]), items[geneIndex], items[canonicalEffectIndex],
                            items[canonicalCodingEffectIndex].isEmpty() ? NONE : CodingEffect.valueOf(items[canonicalCodingEffectIndex]),
                            CodingEffect.valueOf(items[worstCodingEffectIndex]), items[canonicalHgvsCodingImpactIndex],
                            items[canonicalHgvsProteinImpactIndex], items[microhomologyIndex], items[repeatSequenceIndex],
                            Integer.parseInt(items[repeatCountIndex]), localPhaseSet, items[reportedIndex].equals("1"),
                            hotspotIndex != null ? items[hotspotIndex].equals(Hotspot.HOTSPOT.toString()) : false);

                    variants.add(variant);
                }
                catch(Exception e)
                {
                    PV_LOGGER.error("invalid ref variant record({}): {}", line, e.toString());
                }
            }
        }
        catch(IOException e)
        {
            PV_LOGGER.error("failed to read ref variant data file: {}", e.toString());
        }

        return sampleVariantsMap;
    }

}
