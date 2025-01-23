package com.github.mbto.eatlog.common.dto;

import lombok.Getter;
import lombok.Setter;
import org.jooq.types.UInteger;

import java.time.LocalDate;

@Getter
@Setter
public class ConsumedProduct {
    private UInteger store_id;
    private UInteger product_id;
    private UInteger consumed_id;
    private LocalDate consumed_date;
    private String title;
    /**
     * p.bju
     */
    private BJU pBju = new BJU();
    private Short portion_gram;
    private Short consumed_gram;
    /**
     * c.gram * pBju
     */
    private BJU consumedBju = new BJU();
}