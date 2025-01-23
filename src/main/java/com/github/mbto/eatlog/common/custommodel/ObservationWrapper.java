package com.github.mbto.eatlog.common.custommodel;

import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Account;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.jooq.types.UInteger;

@Getter
@AllArgsConstructor
@ToString
public class ObservationWrapper {
    private Account observedAccount;
    private boolean isObservation;

    /* Delegates methods: */

    public UInteger getId() {
        return observedAccount.getId();
    }

    @Size(max = 32)
    @NotNull
    public String getName() {
        return observedAccount.getName();
    }
}
/*
google
observer         наблюдатель
observed         наблюдаемый
observe          наблюдать
observation      наблюдение
observed target  наблюдаемая цель
observing        наблюдение
observable       наблюдаемый
bing
observing        внимательный
observable       достойный внимания
*/