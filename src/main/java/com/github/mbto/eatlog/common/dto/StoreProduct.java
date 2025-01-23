package com.github.mbto.eatlog.common.dto;

import lombok.Getter;
import lombok.Setter;
import org.jooq.types.UInteger;

@Getter
@Setter
public class StoreProduct {
    private UInteger store_id;
    private UInteger product_id;
    private String title;
    private Integer consumed_count;
    private Short portion_gram;
    /**
     * p.bju
     */
    private BJU pBju = new BJU();

    /**
     * calcs after select:
     */
    private int stepFactor = 1;

    public void addToConsumedCount(int value) {
        if(consumed_count != null)
            consumed_count += value;
    }

    public void subFromConsumedCount(int value) {
        if(consumed_count != null)
            consumed_count -= value;
    }

    public void calcStepFactor() {
        int value = Math.floorDiv(portion_gram, 2);
        if(value < 1) {
            return;
        }
        if(value * 2 != portion_gram) {
            stepFactor = portion_gram.intValue();
            return;
        }
        int value2 = Math.floorDiv(portion_gram, 4);
        if(value2 < 1 || value2 * 4 != portion_gram) {
            stepFactor = value;
            return;
        }
        stepFactor = value2;
    }
}