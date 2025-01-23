package com.github.mbto.eatlog.common.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class BJU {
    private BigDecimal b;
    private BigDecimal j;
    private BigDecimal u;

    @EqualsAndHashCode.Exclude
    private BigDecimal b_to_kkal;
    @EqualsAndHashCode.Exclude
    private BigDecimal j_to_kkal;
    @EqualsAndHashCode.Exclude
    private BigDecimal u_to_kkal;
    @EqualsAndHashCode.Exclude
    private BigDecimal kkal;

    public static final Function<BJU, BigDecimal> getBFunc = BJU::getB;
    public static final Function<BJU, BigDecimal> getJFunc = BJU::getJ;
    public static final Function<BJU, BigDecimal> getUFunc = BJU::getU;
    public static final Function<BJU, BigDecimal> getB_to_kkalFunc = BJU::getB_to_kkal;
    public static final Function<BJU, BigDecimal> getJ_to_kkalFunc = BJU::getJ_to_kkal;
    public static final Function<BJU, BigDecimal> getU_to_kkalFunc = BJU::getU_to_kkal;
    public static final BiConsumer<BJU, BigDecimal> setBFunc = BJU::setB;
    public static final BiConsumer<BJU, BigDecimal> setJFunc = BJU::setJ;
    public static final BiConsumer<BJU, BigDecimal> setUFunc = BJU::setU;
    public static final BiConsumer<BJU, BigDecimal> setB_to_kkalFunc = BJU::setB_to_kkal;
    public static final BiConsumer<BJU, BigDecimal> setJ_to_kkalFunc = BJU::setJ_to_kkal;
    public static final BiConsumer<BJU, BigDecimal> setU_to_kkalFunc = BJU::setU_to_kkal;
    public static final Function<BJU, BigDecimal> getKkalFunc = BJU::getKkal;
    public static final BiConsumer<BJU, BigDecimal> setKkalFunc = BJU::setKkal;
    public static final List<List<?>> gsFuncs = Stream.of(
            List.of(getBFunc, getB_to_kkalFunc, setBFunc, setB_to_kkalFunc),
            List.of(getJFunc, getJ_to_kkalFunc, setJFunc, setJ_to_kkalFunc),
            List.of(getUFunc, getU_to_kkalFunc, setUFunc, setU_to_kkalFunc)
    ).collect(Collectors.toUnmodifiableList());
}