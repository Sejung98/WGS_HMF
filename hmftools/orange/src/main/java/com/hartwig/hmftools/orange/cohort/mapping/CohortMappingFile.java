package com.hartwig.hmftools.orange.cohort.mapping;

import static com.hartwig.hmftools.common.utils.file.FileDelimiters.TSV_DELIM;
import static com.hartwig.hmftools.common.utils.file.FileReaderUtils.createFieldsIndexMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.jetbrains.annotations.NotNull;

public final class CohortMappingFile
{
    private static final String DOID_DELIMITER = ";";

    @NotNull
    public static List<CohortMapping> read(@NotNull String tsv) throws IOException
    {
        return fromLines(Files.readAllLines(new File(tsv).toPath()));
    }

    @NotNull
    private static List<CohortMapping> fromLines(@NotNull List<String> lines)
    {
        List<CohortMapping> configRules = Lists.newArrayList();

        Map<String, Integer> fields = createFieldsIndexMap(lines.get(0), TSV_DELIM);

        for(String line : lines.subList(1, lines.size()))
        {
            String[] values = line.split(TSV_DELIM, -1);

            configRules.add(ImmutableCohortMapping.builder()
                    .cancerType(values[fields.get("cancerType")])
                    .preferenceRank(Integer.parseInt(values[fields.get("preferenceRank")]))
                    .rule(MappingRule.valueOf(values[fields.get("rule")]))
                    .include(toDOIDs(values[fields.get("include")]))
                    .exclude(toDOIDs(values[fields.get("exclude")]))
                    .build());
        }

        return configRules;
    }

    @NotNull
    private static Set<String> toDOIDs(@NotNull String doidString)
    {
        Set<String> doids = Sets.newHashSet();
        if(!doidString.trim().isEmpty())
        {
            for(String doid : doidString.split(DOID_DELIMITER))
            {
                doids.add(doid.trim());
            }
        }
        return doids;
    }
}
