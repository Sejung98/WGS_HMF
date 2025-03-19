package com.hartwig.hmftools.patientdb.clinical.datamodel;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class DrugData {

    @Nullable
    public abstract String name();

    @Nullable
    public abstract LocalDate startDate();

    @Nullable
    public abstract LocalDate endDate();

    @Nullable
    public abstract String bestResponse();

    @NotNull
    public abstract List<CuratedDrug> curatedDrugs();

    @NotNull
    @Value.Derived
    public List<CuratedDrug> filteredCuratedDrugs() {
        return curatedDrugs().stream().filter(drug -> !drug.type().toLowerCase().equals("remove")).distinct().collect(Collectors.toList());
    }
}
