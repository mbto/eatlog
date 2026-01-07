package com.github.mbto.eatlog.webapp.view;

import com.github.mbto.eatlog.common.custommodel.ObservationWrapper;
import com.github.mbto.eatlog.common.dto.BJU;
import com.github.mbto.eatlog.common.dto.ConsumedProduct;
import com.github.mbto.eatlog.common.dto.StoreProduct;
import com.github.mbto.eatlog.common.dto.WeightLimitation;
import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Account;
import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Store;
import com.github.mbto.eatlog.utils.QueriesCache;
import com.github.mbto.eatlog.webapp.request.RequestParamsHolder;
import com.github.mbto.eatlog.webapp.vars.VarsMain;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.compare.ComparableUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.types.UInteger;
import org.primefaces.PrimeFaces;
import org.primefaces.component.datalist.DataList;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.mbto.eatlog.common.Constants.DDMMYYYY_PATTERN;
import static com.github.mbto.eatlog.common.Constants.YYYYMMDD_PATTERN;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Consumed.CONSUMED;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Limitation.LIMITATION;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Product.PRODUCT;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Store.STORE;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Weight.WEIGHT;
import static com.github.mbto.eatlog.utils.ProjectUtils.declensionValuedL10N;
import static com.github.mbto.eatlog.utils.ProjectUtils.pointwiseUpdateQuery;
import static com.github.mbto.eatlog.webapp.WebUtils.*;
import static jakarta.faces.application.FacesMessage.SEVERITY_INFO;
import static jakarta.faces.application.FacesMessage.SEVERITY_WARN;

@ViewScoped
@Named
@Slf4j
public class ViewMain implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Autowired
    private DSLContext eatlogDsl;
    @Autowired
    private RequestParamsHolder requestParamsHolder;

    @Getter
    private Map<UInteger, Store> storeById;
    @Getter
    private String cartesianLinerModel;
//    @Getter
//    private String datesRangeString;
    @Getter
    private String datesRangeCartesianLinerModel;
    @Getter
    private LazyDataModel<LocalDate> availableDatesLazyModel;

    /**
     * сортировка naturalOrder: т.к. порядок storeId задаётся
     * при формировании поля Map<UInteger, Store> storeById
     */
    @Getter
    private final Map<UInteger, Map<UInteger, StoreProduct>>
            storeProductMapByStoreId = new TreeMap<>(Comparator.naturalOrder());

    @Getter
    @Setter
    private LocalDate newAvailableDate;
    private Set<LocalDate> newAvailableDates;
    private boolean newAvailableDateUpdated;

    private final Map<LocalDate, VarsMain> varsByDate = new TreeMap<>(Comparator.naturalOrder());

    public void fetch() {
        if (log.isDebugEnabled())
            log.debug("\nfetch");
        storeById = eatlogDsl.select(STORE.ID, STORE.TITLE)
                .from(STORE)
                .where(STORE.ACCOUNT_ID.eq(getObservedAccount().getId()))
                .fetchMap(STORE.ID, Store.class);
        cartesianLinerModel = recreateCartesianLinerModel(/*null*/);
        recreateDatesLazyModel();
        recalcAvailableDatesCountTotal();
    }

    private String recreateCartesianLinerModel(/*List<LocalDate> availableDates*/) {
        if(log.isDebugEnabled())
            log.debug("\nrecreateCartesianLinerModel");
//        if(availableDates != null && availableDates.size() < 2) {
//            return null;
//        }
        Field<String> formattedDateField = DSL.field("date_format({0}, {1})",
                SQLDataType.VARCHAR,
                WEIGHT.DATE, DSL.inline("%d.%m.%Y"));
//        Condition datesCondition = availableDates == null ? DSL.trueCondition()
//                : WEIGHT.DATE.in(availableDates);
        Map<String, BigDecimal> kilogramByDateString = eatlogDsl
                .select(formattedDateField, WEIGHT.KILOGRAM)
                .from(WEIGHT)
                .where(WEIGHT.ACCOUNT_ID.eq(getObservedAccount().getId())/*,
                       datesCondition*/)
                .orderBy(WEIGHT.DATE)
                .fetchMap(formattedDateField, WEIGHT.KILOGRAM);
        if(kilogramByDateString.size() < 2) {
            return null;
        }
        return generateLineChartModelWithStringLabels(kilogramByDateString,
                getObservedAccount().getName(),
                buildRangeStringFromSortedMapKeys(kilogramByDateString, "record"));
    }

    private void recalcDatesRangeData(List<LocalDate> availableDates) {
        if(log.isDebugEnabled())
            log.debug("\nrecalcDatesRangeData");
        if(availableDates == null || availableDates.size() < 2) {
            datesRangeCartesianLinerModel = null;
            return;
        }
/* <p:panelGrid columns="1" style="margin:0 auto;" styleClass="ui-panelgrid-blank">
<h:outputText id="datesRangeStringId" value="#{viewMain.datesRangeString}"
rendered="#{viewMain.availableDatesLazyModel.rowIndex == 0 and viewMain.datesRangeString != null}"/>
</p:panelGrid> */
//        datesRangeString = buildRangeStringFromUnsortedList(availableDates, "calc");
//        datesRangeCartesianLinerModel = recreateCartesianLinerModel(availableDates);
        Map<LocalDate, BigDecimal> weightByDate = availableDates.stream()
                .filter(date -> varsByDate.get(date).getWeight() != null)
                .collect(Collectors.toMap(Function.identity(),
                        date -> varsByDate.get(date).getWeight(),
                        (weight1, weight2) -> {
                            throw new IllegalStateException("Dublicate keys founded with weight1=" + weight1 + ", weight2=" + weight2);
                        }, TreeMap::new));
        datesRangeCartesianLinerModel = generateLineChartModelWithLocalDateLabels(weightByDate,
                getObservedAccount().getName(),
                buildRangeStringFromSortedMapKeys(weightByDate, "record"));
    }

    private void recreateDatesLazyModel() {
        if (log.isDebugEnabled())
            log.debug("\nrecreateDatesLazyModel");
        availableDatesLazyModel = new LazyDataModel<>() {
            private final boolean isDebugEnabled = log.isDebugEnabled();
            @Override
            public List<LocalDate> load(int offset, int limit,
                                        Map<String, SortMeta> sortBy,
                                        Map<String, FilterMeta> filterBy) {
                if(isDebugEnabled) {
                    log.debug("\nload() offset=" + offset
                            + ", limit=" + limit
                            + ", getRowCount()=" + getRowCount()
                            + ", newAvailableDateUpdated=" + newAvailableDateUpdated
                    );
                }
                if(newAvailableDateUpdated) {
                    newAvailableDateUpdated = false;
                    return getWrappedData();
                }
                List<LocalDate> availableDates
                        = eatlogDsl.selectFrom(buildSelectAvailableDates(null, newAvailableDates))
                        .orderBy(DSL.one().desc())
                        .limit(offset, limit)
                        .fetchInto(LocalDate.class);
                fetchVars(null, availableDates);
                recalcDatesRangeData(availableDates);
                return availableDates;
            }
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                if(isDebugEnabled) {
                    log.debug("\ncount() getRowCount()=" + getRowCount());
                }
                return getRowCount();
            }
        };
    }

    private void recalcAvailableDatesCountTotal() {
        if(log.isDebugEnabled())
            log.debug("\nrecalcAvailableDatesCountTotal");
        int availableDatesCountTotal = eatlogDsl.fetchCount(buildSelectAvailableDates(null, null));
        availableDatesLazyModel.setRowCount(availableDatesCountTotal);
    }

    private SelectOrderByStep<Record1<LocalDate>> buildSelectAvailableDates(LocalDate availableDate,
                                                                            Set<LocalDate> additionalsDates) {
        final var wStep = DSL.select(WEIGHT.DATE)
                .from(WEIGHT)
                .where(WEIGHT.ACCOUNT_ID.eq(getObservedAccount().getId()),
                        availableDate != null
                                ? WEIGHT.DATE.eq(availableDate)
                                : DSL.trueCondition());
        final var cpsStep = DSL.selectDistinct(CONSUMED.DATE)
                .from(CONSUMED)
                .join(PRODUCT).on(CONSUMED.PRODUCT_ID.eq(PRODUCT.ID))
                .join(STORE).on(PRODUCT.STORE_ID.eq(STORE.ID))
                .where(STORE.ACCOUNT_ID.eq(getObservedAccount().getId()),
                        availableDate != null
                                ? CONSUMED.DATE.eq(availableDate)
                                : DSL.trueCondition());
        if(CollectionUtils.isEmpty(additionalsDates)) {
            return wStep.union(cpsStep);
        }
//        union SELECT * FROM ( VALUES ROW(DATE '2023-06-17'), ROW(DATE '2023-06-18')) AS t (col1)
        final String customSql = additionalsDates.stream()
                .map(date -> "ROW(DATE '" + date.format(YYYYMMDD_PATTERN) + "')")
                .collect(Collectors.joining(",", "( VALUES ", ") AS t (col1)"));
        final var additionalDatesStep = DSL.select(DSL.field("col1", LocalDate.class))
                .from(DSL.table(customSql));
        return wStep.union(cpsStep)
                    .union(additionalDatesStep);
    }

    private void fetchVars(LocalDate availableDate, List<LocalDate> availableDates) {
        if (log.isDebugEnabled())
            log.debug("\nfetchVars availableDate=" + availableDate + ", availableDates=" + availableDates);
        QueryPart availableDatesCondition = !availableDates.isEmpty()
                ? DSL.list(availableDates.stream()
                        .map(DSL::val)
                        .collect(Collectors.toList()))
                : DSL.val((LocalDate) null);
        Map<LocalDate, List<ConsumedProduct>> consumedProductsByDate = eatlogDsl
                .resultQuery(QueriesCache.get("paginated-consumeds-with-products-by-date[s]"),
                        getObservedAccount().getId(),
                        availableDate,
                        availableDatesCondition)
                .fetchGroups(r -> r.get(CONSUMED.DATE, CONSUMED.DATE.getType()),
                             r -> {
                                 ConsumedProduct cp = new ConsumedProduct();
                                 cp.setStore_id(r.get(STORE.ID.as("store_id")));
                                 cp.setProduct_id(r.get(PRODUCT.ID.as("product_id")));
                                 cp.setConsumed_id(r.get(CONSUMED.ID.as("consumed_id")));
                                 cp.setConsumed_date(r.get(CONSUMED.DATE, CONSUMED.DATE.getType()));
                                 cp.setTitle(r.get(PRODUCT.TITLE));
                                 BJU pBju = cp.getPBju();
                                 pBju.setB(r.get(PRODUCT.B));
                                 pBju.setJ(r.get(PRODUCT.J));
                                 pBju.setU(r.get(PRODUCT.U));
                                 pBju.setB_to_kkal(r.get(PRODUCT.B_TO_KKAL));
                                 pBju.setJ_to_kkal(r.get(PRODUCT.J_TO_KKAL));
                                 pBju.setU_to_kkal(r.get(PRODUCT.U_TO_KKAL));
                                 pBju.setKkal(r.get(PRODUCT.KKAL));
                                 cp.setPortion_gram(r.get(PRODUCT.PORTION_GRAM, Short.class));
                                 cp.setConsumed_gram(r.get(CONSUMED.GRAM.as("consumed_gram"), Short.class));
                                 BJU consumedBju = cp.getConsumedBju();
                                 consumedBju.setB(r.get("consumed_b", BigDecimal.class));
                                 consumedBju.setJ(r.get("consumed_j", BigDecimal.class));
                                 consumedBju.setU(r.get("consumed_u", BigDecimal.class));
                                 consumedBju.setB_to_kkal(r.get("consumed_b_to_kkal", BigDecimal.class));
                                 consumedBju.setJ_to_kkal(r.get("consumed_j_to_kkal", BigDecimal.class));
                                 consumedBju.setU_to_kkal(r.get("consumed_u_to_kkal", BigDecimal.class));
                                 consumedBju.setKkal(r.get("consumed_kkal", BigDecimal.class));
                                 return cp;
                             });
        Map<LocalDate, List<WeightLimitation>> weightLimitationByDate = eatlogDsl
                .resultQuery(QueriesCache.get("paginated-weights-with-limitations-by-date[s]"),
                        getObservedAccount().getId(),
                        availableDate,
                        availableDatesCondition)
                .fetchGroups(r -> r.get(WEIGHT.DATE, WEIGHT.DATE.getType()),
                             r -> {
                                 WeightLimitation wl = new WeightLimitation();
                                 wl.setWeight_id(r.get(WEIGHT.ID.as("weight_id")));
                                 wl.setWeight_date(r.get(WEIGHT.DATE, WEIGHT.DATE.getType()));
                                 wl.setKilogram(r.get(WEIGHT.KILOGRAM));
                                 wl.setTitle(r.get(LIMITATION.TITLE));
                                 BJU lBju = wl.getLBju();
                                 lBju.setB(r.get(LIMITATION.B));
                                 lBju.setJ(r.get(LIMITATION.J));
                                 lBju.setU(r.get(LIMITATION.U));
                                 lBju.setB_to_kkal(r.get(LIMITATION.B_TO_KKAL));
                                 lBju.setJ_to_kkal(r.get(LIMITATION.J_TO_KKAL));
                                 lBju.setU_to_kkal(r.get(LIMITATION.U_TO_KKAL));
                                 BJU maxBju = wl.getMaxBju();
                                 maxBju.setB(r.get("b_max", BigDecimal.class));
                                 maxBju.setJ(r.get("j_max", BigDecimal.class));
                                 maxBju.setU(r.get("u_max", BigDecimal.class));
                                 maxBju.setB_to_kkal(r.get("b_max_to_kkal", BigDecimal.class));
                                 maxBju.setJ_to_kkal(r.get("j_max_to_kkal", BigDecimal.class));
                                 maxBju.setU_to_kkal(r.get("u_max_to_kkal", BigDecimal.class));
                                 return wl;
                             });
        if (availableDate == null) {
            updateNextAvailableDate(availableDates);
        }
        Map<LocalDate, BJU> consumedBJUsumByDate = consumedProductsByDate.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> {
                            //noinspection OptionalGetWithoutIsPresent
                            ConsumedProduct cp = entry.getValue()
                                    .stream()
                                    .filter(cwp -> cwp.getConsumed_id() == null) // row 'сумма'
                                    .findFirst()
                                    .get(); // in all records has row 'сумма' with null consumed_id
                            return cp.getConsumedBju();
                        },
                        (bju1, bju2) -> {
                            throw new IllegalStateException("Dublicate keys founded with bju1=" + bju1 + ", bju2=" + bju2);
                        }, HashMap::new));

        Map<LocalDate, BigDecimal> weightByDate = weightLimitationByDate.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        // size > 0 always, because if list exists -> record exists
                        entry -> entry.getValue().get(0).getKilogram(),
                        (kilo1, kilo2) -> {
                            throw new IllegalStateException("Dublicate keys founded with kilo1=" + kilo1 + ", kilo2=" + kilo2);
                        }, HashMap::new));
        if(availableDate == null) {
            for (LocalDate anyDate : availableDates) {
                VarsMain varsMain = getVarsByDate(anyDate);
                varsMain.setConsumedProducts(consumedProductsByDate.get(anyDate));
                varsMain.setWeightLimitations(weightLimitationByDate.get(anyDate));
                varsMain.setConsumedBJUsum(consumedBJUsumByDate.get(anyDate));
                varsMain.setWeight(weightByDate.get(anyDate));
                varsMain.recalcWeightLimitations();
            }
        } else {
            VarsMain varsMain = getVarsByDate(availableDate);
            varsMain.setConsumedProducts(consumedProductsByDate.get(availableDate));
            varsMain.setWeightLimitations(weightLimitationByDate.get(availableDate));
            varsMain.setConsumedBJUsum(consumedBJUsumByDate.get(availableDate));
            varsMain.setWeight(weightByDate.get(availableDate));
            varsMain.recalcWeightLimitations();
        }
    }

    public void createCalc() {
        if (log.isDebugEnabled())
            log.debug("\ncreateCalc");
        FacesContext fc = FacesContext.getCurrentInstance();
        List<LocalDate> cachedDates = availableDatesLazyModel.getWrappedData();
        if(cachedDates == null) {
            cachedDates = new ArrayList<>();
            availableDatesLazyModel.setWrappedData(cachedDates);
        }
        if(newAvailableDate == null) {
            updateNextAvailableDate(cachedDates);
        }
        var selectAvailableDatesStep = buildSelectAvailableDates(newAvailableDate, null);
        if(cachedDates.stream()
                .anyMatch(cd -> cd.equals(newAvailableDate))
                || eatlogDsl.fetchExists(selectAvailableDatesStep)) {
            fc.addMessage("datePickerId", new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("calc.data.exists", newAvailableDate.format(DDMMYYYY_PATTERN)), ""));
            return;
        }
        cachedDates.add(0, newAvailableDate);
        if(newAvailableDates == null) {
            newAvailableDates = new TreeSet<>();
        }
        newAvailableDates.add(newAvailableDate);
        availableDatesLazyModel.setRowCount(availableDatesLazyModel.getRowCount() + 1);
        fc.addMessage("datePickerId", new FacesMessage(SEVERITY_INFO,
                msgFromBundle("calc.data.added", newAvailableDate.format(DDMMYYYY_PATTERN)), ""));
        getVarsByDate(newAvailableDate); // initialize VarsMain
//        recalcDatesRangeData(cachedDates.subList(0, Math.min(cachedDates.size(), Math.max(1, availableDatesLazyModel.getPageSize()))));
        updateNextAvailableDate(cachedDates);
        newAvailableDateUpdated = true;
        PrimeFaces pf = PrimeFaces.current();
        pf.ajax().update("repeatOutputPanelId");
    }

    private void updateNextAvailableDate(List<LocalDate> availableDates) {
        if (log.isDebugEnabled())
            log.debug("\nupdateNextAvailableDate availableDates=" + availableDates);
        newAvailableDate = availableDates
                .stream()
                .max(Comparator.naturalOrder())
                .map(date -> date.plusDays(1))
                .orElse(LocalDate.now());
    }

    public void onConsumedSelect(LocalDate availableDate) {
        VarsMain varsMain = getVarsByDate(availableDate);
        ConsumedProduct selectedConsumedProduct = varsMain.getSelectedConsumedProduct();
        if(selectedConsumedProduct == null) { // column 'сумма'
            varsMain.setSelectedStoreId(null);
            varsMain.setSelectedProductId(null);
            varsMain.setNewConsumedGram(null);
            return;
        }
        varsMain.setSelectedStoreId(selectedConsumedProduct.getStore_id());
        onStoreSelect(availableDate);
        varsMain.setSelectedProductId(selectedConsumedProduct.getProduct_id());
        onProductSelect(availableDate);
    }

    public void onStoreSelect(LocalDate availableDate) {
        VarsMain varsMain = getVarsByDate(availableDate);
        varsMain.setSelectedConsumedProduct(null);
        varsMain.setSelectedProductId(null);
        varsMain.setNewConsumedGram(null);
        rebuldStoreProductMapByStoreId(availableDate);
    }

    private void rebuldStoreProductMapByStoreId(LocalDate availableDate) {
        VarsMain varsMain = getVarsByDate(availableDate);
        UInteger selectedStoreId = varsMain.getSelectedStoreId();
        if(selectedStoreId == null || storeProductMapByStoreId.get(selectedStoreId) != null)
            return;
        Map<UInteger, StoreProduct> storeProductByProductId = eatlogDsl
                .resultQuery(QueriesCache.get("all-products-with-count(consumeds)-by-store.id"),
                        getObservedAccount().getId(),
                        selectedStoreId)
                .fetchMap(r -> r.get(PRODUCT.ID.as("product_id")),
                          r -> {
                              StoreProduct spc = new StoreProduct();
                              spc.setStore_id(r.get(STORE.ID.as("store_id")));
                              spc.setProduct_id(r.get(PRODUCT.ID.as("product_id")));
                              spc.setTitle(r.get(PRODUCT.TITLE));
                              spc.setConsumed_count(r.get("consumed_count", Integer.class));
                              spc.setPortion_gram(r.get(PRODUCT.PORTION_GRAM, Short.class));
                              spc.calcStepFactor();
                              BJU pBju = spc.getPBju();
                              pBju.setB(r.get(PRODUCT.B));
                              pBju.setJ(r.get(PRODUCT.J));
                              pBju.setU(r.get(PRODUCT.U));
                              pBju.setKkal(r.get(PRODUCT.KKAL));
                              return spc;
                          });
        storeProductMapByStoreId.put(selectedStoreId, storeProductByProductId);
    }

    public void onProductSelect(LocalDate availableDate) {
        VarsMain varsMain = getVarsByDate(availableDate);
        varsMain.setNewConsumedGram(calcNewConsumedGram(availableDate));
        UInteger selectedProductId = varsMain.getSelectedProductId();
        List<ConsumedProduct> consumedProduct = varsMain.getConsumedProducts();
        if(consumedProduct != null) {
            ConsumedProduct selectedConsumedProduct = varsMain.getSelectedConsumedProduct();
            if (selectedConsumedProduct == null || !selectedProductId.equals(selectedConsumedProduct.getProduct_id())) {
                selectedConsumedProduct = consumedProduct
                        .stream()
                        .filter(cp -> selectedProductId.equals(cp.getProduct_id()))
                        .findFirst()
                        .orElse(null);
                varsMain.setSelectedConsumedProduct(selectedConsumedProduct);
            }
        }
    }

    private Short calcNewConsumedGram(LocalDate availableDate) {
        VarsMain varsMain = getVarsByDate(availableDate);
        UInteger selectedProductId = varsMain.getSelectedProductId();
        if(selectedProductId == null)
            return null;
        Optional<ConsumedProduct> existedConsumedProduct = findExistedConsumedProduct(availableDate);
        if(existedConsumedProduct.isEmpty()) { // product doesn't exist in consumed table
            UInteger selectedStoreId = varsMain.getSelectedStoreId();
            if(selectedStoreId == null)
                return null;
            return storeProductMapByStoreId.get(selectedStoreId)
                    .get(selectedProductId)
                    .getPortion_gram();
        } else { // product exists in consumed table
            return existedConsumedProduct.get().getConsumed_gram();
        }
    }

    public Optional<ConsumedProduct> findExistedConsumedProduct(LocalDate availableDate) {
        VarsMain varsMain = getVarsByDate(availableDate);
        List<ConsumedProduct> consumedProducts = varsMain.getConsumedProducts();
        if(consumedProducts == null)
            return Optional.empty();
        UInteger selectedProductId = varsMain.getSelectedProductId();
        if(selectedProductId == null)
            return Optional.empty();
        return consumedProducts.stream()
                .filter(cp -> cp.getProduct_id() != null /* row with 'сумма' */
                        && cp.getProduct_id().equals(selectedProductId))
                .findFirst();
    }
    public String buildConsumedCount(LocalDate availableDate) {
        VarsMain varsMain = getVarsByDate(availableDate);
        UInteger selectedStoreId = varsMain.getSelectedStoreId();
        if(selectedStoreId == null)
            return null;
        Map<UInteger, StoreProduct> storeProductByProductId = storeProductMapByStoreId
                .get(selectedStoreId);
        if(storeProductByProductId == null)
            return null;
        int count = storeProductByProductId.values()
                .stream()
                .mapToInt(StoreProduct::getConsumed_count)
                .sum();
        return msgFromBundle("count.consumed.records", declensionValuedL10N(count, "record"));
    }

    public void incOrDecNewConsumedGram(LocalDate availableDate, boolean incOrDec) {
        VarsMain varsMain = getVarsByDate(availableDate);
        UInteger selectedStoreId = varsMain.getSelectedStoreId();
        if(selectedStoreId == null)
            return;
        Map<UInteger, StoreProduct> storeProductByProductId = storeProductMapByStoreId
                .get(selectedStoreId);
        if(storeProductByProductId == null)
            return;
        StoreProduct storeProduct = storeProductByProductId.get(varsMain.getSelectedProductId());
        if(storeProduct == null)
            return;
        short stepFactor = (short) storeProduct.getStepFactor();
        Short currentConsumedGram = varsMain.getNewConsumedGram();
        boolean isMin = currentConsumedGram == (short) 1;
        boolean isMax = currentConsumedGram == (short) 9999;
        short newConsumedGram;
        if(!incOrDec) { // decrement
            if(isMin) {
                newConsumedGram = 1;
            } else if(isMax) {
                short newValue = 0;
                while(newValue <= 9999) {
                    newValue += stepFactor;
                }
                newConsumedGram = (short) (newValue - stepFactor);
            } else {
                if(currentConsumedGram % stepFactor != 0) {
                    short newValue = 0;
                    while(newValue <= currentConsumedGram) {
                        newValue += stepFactor;
                    }
                    newConsumedGram = (short) (newValue - stepFactor);
                } else {
                    newConsumedGram = (short) (currentConsumedGram - stepFactor);
                }
            }
        } else {        // increment
            if(isMin) {
                newConsumedGram = stepFactor;
            } else if(isMax) {
                newConsumedGram = 9999;
            } else {
                if(currentConsumedGram % stepFactor != 0) {
                    short newValue = 0;
                    while(newValue <= currentConsumedGram) {
                        newValue += stepFactor;
                    }
                    newConsumedGram = newValue;
                } else {
                    newConsumedGram = (short) (currentConsumedGram + stepFactor);
                }
            }
        }
        if(newConsumedGram < 1) {
            newConsumedGram = 1;
        } else if(newConsumedGram > 9999) {
            newConsumedGram = 9999;
        }
        varsMain.setNewConsumedGram(newConsumedGram);
    }

    public void insertOrUpdateNewConsumedGram(LocalDate availableDate) {
        VarsMain varsMain = getVarsByDate(availableDate);
        final UInteger selectedStoreId = varsMain.getSelectedStoreId();
        final UInteger selectedProductId = varsMain.getSelectedProductId();
        final Short newConsumedGram = varsMain.getNewConsumedGram();
        int modificationsCount = eatlogDsl.transactionResult(config -> {
            DSLContext transactionalDsl = DSL.using(config);
            UInteger existedConsumedId = findExistedConsumedProduct(availableDate)
                    .map(ConsumedProduct::getConsumed_id)
                    .orElse(null);
            if(existedConsumedId == null) {
                existedConsumedId = transactionalDsl.selectDistinct(CONSUMED.ID)
                        .from(CONSUMED)
                        .join(PRODUCT).on(CONSUMED.PRODUCT_ID.eq(PRODUCT.ID))
                        .where(CONSUMED.DATE.eq(availableDate),
                                PRODUCT.ID.eq(selectedProductId))
                        .fetchOne(CONSUMED.ID);
            }
            if(existedConsumedId == null) {
                int rowsCount = transactionalDsl.insertInto(CONSUMED)
                        .set(CONSUMED.DATE, availableDate)
                        .set(CONSUMED.PRODUCT_ID, selectedProductId)
                        .set(CONSUMED.GRAM, newConsumedGram)
                        .execute();
                storeProductMapByStoreId.get(selectedStoreId)
                        .get(selectedProductId)
                        .addToConsumedCount(rowsCount);
                return rowsCount;
            } else {
                return pointwiseUpdateQuery(transactionalDsl, CONSUMED.ID,
                        existedConsumedId,
                        List.of(Pair.of(CONSUMED.GRAM, newConsumedGram)));
            }
        });
        varsMain.setSelectedConsumedProduct(null);
        varsMain.setSelectedProductId(null);
        varsMain.setNewConsumedGram(null);
        if(modificationsCount > 0) {
            fetchVars(availableDate, availableDatesLazyModel.getWrappedData());
        }
    }

    public void deleteConsumedGram(LocalDate availableDate) {
        VarsMain varsMain = getVarsByDate(availableDate);
        final UInteger selectedStoreId = varsMain.getSelectedStoreId();
        final UInteger selectedProductId = varsMain.getSelectedProductId();
        int modificationsCount = eatlogDsl.transactionResult(config -> {
            DSLContext transactionalDsl = DSL.using(config);
            int rowsCount = transactionalDsl.deleteFrom(CONSUMED)
                    .where(CONSUMED.DATE.eq(availableDate),
                            CONSUMED.PRODUCT_ID.eq(selectedProductId))
                    .execute();
            storeProductMapByStoreId.get(selectedStoreId)
                    .get(selectedProductId)
                    .subFromConsumedCount(rowsCount);
            return rowsCount;
        });
        varsMain.setSelectedConsumedProduct(null);
        varsMain.setSelectedProductId(null);
        varsMain.setNewConsumedGram(null);
        if(modificationsCount > 0) {
            fetchVars(availableDate, availableDatesLazyModel.getWrappedData());
        }
    }

    public void insertOrUpdateNewWeight(LocalDate availableDate, DataList dataList) {
        VarsMain varsMain = getVarsByDate(availableDate);
        List<WeightLimitation> weightLimitation = varsMain.getWeightLimitations();
        final BigDecimal newWeight = varsMain.getWeight();
//        final BigDecimal newWeight = varsMain.getNewWeight();
        int modificationsCount = eatlogDsl.transactionResult(config -> {
            DSLContext transactionalDsl = DSL.using(config);
            UInteger existedWeightId;
            if(weightLimitation != null) {
                // size > 0 always, because if list exists -> record exists
                existedWeightId = weightLimitation.get(0).getWeight_id();
            } else {
                existedWeightId = transactionalDsl.selectDistinct(WEIGHT.ID)
                        .from(WEIGHT)
                        .where(WEIGHT.DATE.eq(availableDate),
                               WEIGHT.ACCOUNT_ID.eq(getObservedAccount().getId()))
                        .fetchOne(WEIGHT.ID);
            }
            if(existedWeightId == null) {
                return transactionalDsl.insertInto(WEIGHT)
                        .set(WEIGHT.DATE, availableDate)
                        .set(WEIGHT.ACCOUNT_ID, getObservedAccount().getId())
                        .set(WEIGHT.KILOGRAM, newWeight)
                        .execute();
            } else {
                return pointwiseUpdateQuery(transactionalDsl, WEIGHT.ID,
                        existedWeightId,
                        List.of(Pair.of(WEIGHT.KILOGRAM, newWeight)));
            }
        });
        if(modificationsCount > 0) {
            cartesianLinerModel = recreateCartesianLinerModel(/*null*/);
            List<LocalDate> cachedDates = availableDatesLazyModel.getWrappedData();
            fetchVars(availableDate, cachedDates);
            recalcDatesRangeData(cachedDates.subList(0, Math.min(cachedDates.size(), Math.max(1, availableDatesLazyModel.getPageSize()))));
            PrimeFaces pf = PrimeFaces.current();
            int datesRangeCartesianOutputPanelIndex = dataList.getPage() * availableDatesLazyModel.getPageSize();
            pf.ajax().update("cartesianOutputPanelId",
                    dataList.getClientId() + ":" + datesRangeCartesianOutputPanelIndex + ":datesRangeCartesianOutputPanelId");
        }
    }

    public VarsMain getVarsByDate(LocalDate availableDate) {
        return varsByDate.computeIfAbsent(availableDate, localDate -> new VarsMain());
    }

    public String buildMsgEmojiDiff(BigDecimal resultBjuValue) {
        return ComparableUtils.is(resultBjuValue)
                .lessThanOrEqualTo(BigDecimal.ZERO) ? "✔" : "❌";
    }

    public String buildMsgLimitDiff(BigDecimal resultBjuValue, BigDecimal resultBjuToKkalValue) {
        if(ComparableUtils.is(resultBjuValue)
                .lessThanOrEqualTo(BigDecimal.ZERO)) {
            return msgFromBundle("calc.no.excess", resultBjuValue.abs(), resultBjuToKkalValue.abs());
        } else {
            return msgFromBundle("calc.has.excess", resultBjuValue, resultBjuToKkalValue);
        }
    }

    public ObservationWrapper getObservationWrapper() {
        return requestParamsHolder.getObservationWrapper();
    }

    public Account getObservedAccount() {
        return getObservationWrapper().getObservedAccount();
    }
}