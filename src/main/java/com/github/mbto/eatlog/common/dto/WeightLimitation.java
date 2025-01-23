package com.github.mbto.eatlog.common.dto;

import lombok.Getter;
import lombok.Setter;
import org.jooq.types.UInteger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Function;

@Getter
@Setter
public class WeightLimitation {
    private UInteger weight_id;
    private LocalDate weight_date;
    private BigDecimal kilogram;
    private String title;
    /**
     * l.bju
     */
    private BJU lBju = new BJU();
    /**
     * w.kilogram * lBju
     */
    private BJU maxBju = new BJU();
    /**
     * calcs after select:
     * consumedBJUsum - maxBju
     */
    private BJU resultBju = new BJU();

//    public static final Function<WeightLimitation, BJU> getBjuFunc = WeightLimitation::getBju;
    public static final Function<WeightLimitation, BJU> getMaxBjuFunc = WeightLimitation::getMaxBju;
    public static final Function<WeightLimitation, BJU> getResultBjuFunc = WeightLimitation::getResultBju;
}