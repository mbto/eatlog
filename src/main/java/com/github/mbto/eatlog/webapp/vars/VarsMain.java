package com.github.mbto.eatlog.webapp.vars;

import com.github.mbto.eatlog.common.dto.BJU;
import com.github.mbto.eatlog.common.dto.ConsumedProduct;
import com.github.mbto.eatlog.common.dto.WeightLimitation;
import lombok.Getter;
import lombok.Setter;
import org.jooq.types.UInteger;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Getter
@Setter
public class VarsMain {
    private List<ConsumedProduct> consumedProducts;
    private List<WeightLimitation> weightLimitations;
    private BJU consumedBJUsum;
    private BigDecimal weight;

    private ConsumedProduct selectedConsumedProduct;
    private UInteger selectedStoreId;
    private UInteger selectedProductId;
    private Short newConsumedGram;
//    private BigDecimal newWeight;

    public boolean isNew() {
        return CollectionUtils.isEmpty(consumedProducts)
                && CollectionUtils.isEmpty(weightLimitations);
    }

    @SuppressWarnings("unchecked")
    public void recalcWeightLimitations() {
        if(consumedBJUsum == null || CollectionUtils.isEmpty(weightLimitations))
            return;
        for (WeightLimitation wl : weightLimitations) {
            for (List<?> gsFunc : BJU.gsFuncs) {
                Function<BJU, BigDecimal> getterBju = (Function<BJU, BigDecimal>) gsFunc.get(0);
                Function<BJU, BigDecimal> getterBjuToKkal = (Function<BJU, BigDecimal>) gsFunc.get(1);
                BiConsumer<BJU, BigDecimal> setterBju = (BiConsumer<BJU, BigDecimal>) gsFunc.get(2);
                BiConsumer<BJU, BigDecimal> setterBjuToKkal = (BiConsumer<BJU, BigDecimal>) gsFunc.get(3);
                BigDecimal subtrahend = getterBju.apply(WeightLimitation.getMaxBjuFunc.apply(wl));
                if(subtrahend != null) {
                    BigDecimal diff = getterBju.apply(consumedBJUsum)
                            .subtract(subtrahend);
                    setterBju.accept(WeightLimitation.getResultBjuFunc.apply(wl), diff);
                }
                subtrahend = getterBjuToKkal.apply(WeightLimitation.getMaxBjuFunc.apply(wl));
                if(subtrahend != null) {
                    BigDecimal diff = getterBjuToKkal.apply(consumedBJUsum)
                            .subtract(subtrahend);
                    setterBjuToKkal.accept(WeightLimitation.getResultBjuFunc.apply(wl), diff);
                }
            }
        }
    }
}