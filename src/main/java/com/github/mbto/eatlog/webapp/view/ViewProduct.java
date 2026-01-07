package com.github.mbto.eatlog.webapp.view;

import com.github.mbto.eatlog.common.custommodel.ObservationWrapper;
import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Account;
import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Product;
import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Store;
import com.github.mbto.eatlog.webapp.request.RequestParamsHolder;
import com.github.mbto.eatlog.webapp.session.SessionAccount;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.types.UInteger;
import org.primefaces.PrimeFaces;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.github.mbto.eatlog.common.Constants.*;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Consumed.CONSUMED;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Product.PRODUCT;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Store.STORE;
import static com.github.mbto.eatlog.utils.ProjectUtils.declensionValuedL10N;
import static com.github.mbto.eatlog.utils.ProjectUtils.pointwiseUpdateQuery;
import static com.github.mbto.eatlog.webapp.WebUtils.msgFromBundle;
import static jakarta.faces.application.FacesMessage.SEVERITY_INFO;
import static jakarta.faces.application.FacesMessage.SEVERITY_WARN;

@ViewScoped
@Named
@Slf4j
public class ViewProduct implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Autowired
    private SessionAccount sessionAccount;
    @Autowired
    private DSLContext eatlogDsl;
    @Autowired
    private RequestParamsHolder requestParamsHolder;

    @Getter
    private Map<UInteger, Store> parentById;
    @Getter
    @Setter
    private UInteger selectedParentId;
    @Getter
    private LazyDataModel<Product> childsLazyModel;
    @Getter
    @Setter
    private Product selectedChild;
    @Getter
    private int childRelations;
    @Getter
    private int diffWithMyChildsCount;

    private int localChangesCounter;

    public void fetch() {
        if (log.isDebugEnabled())
            log.debug("\nfetch");
        parentById = eatlogDsl.selectFrom(STORE)
                .where(STORE.ACCOUNT_ID.eq(getObservedAccount().getId()))
                .fetchMap(STORE.ID, Store.class);
        recreateChildsLazyModel();
        if(!parentById.isEmpty()) {
            selectedParentId = parentById.entrySet()
                    .stream()
                    .max(Comparator.comparing(entry -> entry.getValue().getUpdatedAt()))
                    .get()
                    .getKey();
            onParentSelected();
        }
    }

    private void recreateChildsLazyModel() {
        if (log.isDebugEnabled())
            log.debug("\nrecreateChildsLazyModel");
        childsLazyModel = new LazyDataModel<>() {
            private final boolean isDebugEnabled = log.isDebugEnabled();

            @Override
            public List<Product> load(int offset, int limit,
                                      Map<String, SortMeta> sortBy,
                                      Map<String, FilterMeta> filterBy) {
                if(isDebugEnabled) {
                    log.debug("\nload() offset=" + offset
                            + ", limit=" + limit
                            + ", getRowCount()=" + getRowCount());
                }
                return eatlogDsl.select(PRODUCT.asterisk())
                        .from(STORE)
                        .join(PRODUCT).on(STORE.ID.eq(PRODUCT.STORE_ID))
                        .where(STORE.ID.eq(selectedParentId))
                        .orderBy(PRODUCT.TITLE)
                        .limit(offset, limit)
                        .fetchInto(Product.class);
            }
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                if(isDebugEnabled) {
                    log.debug("\ncount() getRowCount()=" + getRowCount());
                }
                return getRowCount();
            }
            @Override
            public String getRowKey(Product child) {
                if(isDebugEnabled) {
                    log.debug("\ngetRowKey() child=" + child);
                }
                return child.getId() != null ? child.getId().toString() : null;
            }
            @Override
            public Product getRowData(String rowKeyStr) {
                if(isDebugEnabled) {
                    log.debug("\ngetRowData() rowKeyStr=" + rowKeyStr);
                }
                UInteger rowKey = UInteger.valueOf(rowKeyStr);
                if(selectedChild != null && rowKey.equals(selectedChild.getId()))
                    return selectedChild;
                Product child = getWrappedData()
                        .stream()
                        .filter(o -> o.getId().equals(rowKey))
                        .findFirst()
                        .orElse(null);
                if(child != null)
                    return selectedChild = child;
                child = eatlogDsl.selectFrom(PRODUCT)
                        .where(PRODUCT.ID.eq(rowKey))
                        .fetchOneInto(Product.class);
                if(child == null)
                    throw new IllegalStateException(msgFromBundle("unable.find.product", rowKey));
                return selectedChild = child;
            }
        };
    }

    public void onParentSelected() {
        if (log.isDebugEnabled())
            log.debug("\nonParentSelected");
        selectedChild = null;
        recalcChildsCountTotal();
        recalcDiffWithMyChildsCount();
    }

    private void recalcChildsCountTotal() {
        if(log.isDebugEnabled())
            log.debug("\nrecalcChildsCountTotal");
        int childsCountTotal = eatlogDsl.fetchCount(
                STORE.join(PRODUCT)
                        .on(STORE.ID.eq(PRODUCT.STORE_ID)),
                STORE.ID.eq(selectedParentId));
        childsLazyModel.setRowCount(childsCountTotal);
    }

    public void recalcDiffWithMyChildsCount() {
        if(!getObservationWrapper().isObservation())
            return;
        final Store selectedParent = parentById.get(selectedParentId);
        final UInteger targetAccountId = sessionAccount.getInternalAccount().getId();
        var targetParentIdStep = eatlogDsl.select(STORE.ID)
                .from(STORE)
                .where(STORE.ACCOUNT_ID.eq(targetAccountId),
                        STORE.TITLE.equalIgnoreCase(selectedParent.getTitle()))
                .limit(1);
        diffWithMyChildsCount = eatlogDsl.fetchCount(
                eatlogDsl.select(PRODUCT.TITLE, PRODUCT.PORTION_GRAM,
                                PRODUCT.B, PRODUCT.J, PRODUCT.U)
                        .from(PRODUCT)
                        .where(PRODUCT.STORE_ID.eq(selectedParent.getId()))
                        .except(eatlogDsl
                                .select(PRODUCT.TITLE, PRODUCT.PORTION_GRAM,
                                        PRODUCT.B, PRODUCT.J, PRODUCT.U)
                                .from(PRODUCT)
                                .where(PRODUCT.STORE_ID.eq(targetParentIdStep))
                        )
        );
    }

    public void onChildSelect() {
        childRelations = eatlogDsl.fetchCount(CONSUMED, CONSUMED.PRODUCT_ID.eq(selectedChild.getId()));
        if(childRelations == 0)
            return;
        FacesContext fc = FacesContext.getCurrentInstance();
        fc.addMessage(dialogMsgs, new FacesMessage(SEVERITY_INFO,
                msgFromBundle("info.delete.product", declensionValuedL10N(childRelations, "record")),
                ""));
    }

    public void add() {
        if (log.isDebugEnabled())
            log.debug("\nadd");
        selectedChild = new Product();
        selectedChild.setStoreId(selectedParentId);
    }

    public void saveOrUpdate() {
        log.info("Attempting save/update " + selectedChild);
        FacesContext fc = FacesContext.getCurrentInstance();
        PrimeFaces pf = PrimeFaces.current();
        localChangesCounter = 0;
        if (selectedChild.getId() == null) {
            try {
                localChangesCounter = eatlogDsl.insertInto(PRODUCT)
                        .set(PRODUCT.STORE_ID, selectedChild.getStoreId())
                        .set(PRODUCT.TITLE, selectedChild.getTitle())
                        .set(PRODUCT.PORTION_GRAM, selectedChild.getPortionGram())
                        .set(PRODUCT.B, selectedChild.getB())
                        .set(PRODUCT.J, selectedChild.getJ())
                        .set(PRODUCT.U, selectedChild.getU())
                        .execute();
            } catch (Throwable e) {
                log.warn("Failed save " + selectedChild, e);
                fc.addMessage(dialogMsgs, new FacesMessage(SEVERITY_WARN,
                        msgFromBundle("failed.save.product"), ""));
                return;
            }
            fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                    msgFromBundle("product.added"),
                    declensionValuedL10N(localChangesCounter, "change")));
        } else {
            try {
                localChangesCounter = pointwiseUpdateQuery(eatlogDsl, PRODUCT.ID, selectedChild.getId(),
                        Arrays.asList(
                                Pair.of(PRODUCT.STORE_ID, selectedChild.getStoreId()),
                                Pair.of(PRODUCT.TITLE, selectedChild.getTitle()),
                                Pair.of(PRODUCT.PORTION_GRAM, selectedChild.getPortionGram()),
                                Pair.of(PRODUCT.B, selectedChild.getB()),
                                Pair.of(PRODUCT.J, selectedChild.getJ()),
                                Pair.of(PRODUCT.U, selectedChild.getU())
                        )
                );
            } catch (Throwable e) {
                log.warn("Failed update " + selectedChild, e);
                fc.addMessage(dialogMsgs, new FacesMessage(SEVERITY_WARN,
                        msgFromBundle("failed.update.product"), ""));
                return;
            }
            fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                    msgFromBundle("product.updated"),
                    declensionValuedL10N(localChangesCounter, "change")));
        }
        selectedChild = null;
        recalcChildsCountTotal();
        pf.ajax().update(editChildsForm_childsTable, msgs);
        pf.executeScript(hideEditChildDialogWidget);
    }

    public void delete() {
        log.info("Attempting delete " + selectedChild);
        FacesContext fc = FacesContext.getCurrentInstance();
        PrimeFaces pf = PrimeFaces.current();
        localChangesCounter = 0;
        try {
            localChangesCounter = eatlogDsl.deleteFrom(PRODUCT)
                    .where(PRODUCT.ID.eq(selectedChild.getId()))
                    .execute();
        } catch (Throwable e) {
            log.warn("Failed delete " + selectedChild, e);
            fc.addMessage(dialogMsgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("failed.delete.product"), ""));
            return;
        }
        fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                msgFromBundle("product.deleted"),
                declensionValuedL10N(localChangesCounter, "change")));
        selectedChild = null;
        recalcChildsCountTotal();
        pf.ajax().update(editChildsForm_childsTable, msgs);
        pf.executeScript(hideEditChildDialogWidget);
    }

    public void copyChildsToMyParent() {
        final Store selectedParent = parentById.get(selectedParentId);
        log.info("Attempting copyChildsToMyParent from selectedParent=" + selectedParent);
        FacesContext fc = FacesContext.getCurrentInstance();
        final UInteger targetAccountId = sessionAccount.getInternalAccount().getId();
        localChangesCounter = 0;
        try {
            eatlogDsl.transaction(config -> {
                DSLContext transactionalDsl = DSL.using(config);
                UInteger targetParentId = transactionalDsl.select(STORE.ID)
                        .from(STORE)
                        .where(STORE.ACCOUNT_ID.eq(targetAccountId),
                                STORE.TITLE.equalIgnoreCase(selectedParent.getTitle()))
                        .limit(1)
                        .fetchOne(STORE.ID);
                if (targetParentId == null) {
                    targetParentId = transactionalDsl.insertInto(STORE)
                            .set(STORE.ACCOUNT_ID, targetAccountId)
                            .set(STORE.TITLE, selectedParent.getTitle())
                            .returning(STORE.ID)
                            .fetchOne(STORE.ID);
                    localChangesCounter += 1;
                }
                localChangesCounter += transactionalDsl
                        .insertInto(PRODUCT,
                                PRODUCT.STORE_ID, PRODUCT.TITLE, PRODUCT.PORTION_GRAM,
                                PRODUCT.B, PRODUCT.J, PRODUCT.U)
                        .select(transactionalDsl.select(
                                        DSL.val(targetParentId), PRODUCT.TITLE, PRODUCT.PORTION_GRAM,
                                        PRODUCT.B, PRODUCT.J, PRODUCT.U)
                                .from(PRODUCT)
                                .where(PRODUCT.STORE_ID.eq(selectedParent.getId()))
                                .except(transactionalDsl.select(
                                                DSL.val(targetParentId), PRODUCT.TITLE, PRODUCT.PORTION_GRAM,
                                                PRODUCT.B, PRODUCT.J, PRODUCT.U)
                                        .from(PRODUCT)
                                        .where(PRODUCT.STORE_ID.eq(targetParentId))
                                ).orderBy(PRODUCT.TITLE)
                        ).execute();
            });
        } catch (Throwable e) {
            log.warn("Failed copyChildsToMyParent from selectedParent=" + selectedParent, e);
            fc.addMessage(msgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("failed.copy.products"), ""));
            return;
        }
        fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                msgFromBundle("products.copied"),
                declensionValuedL10N(localChangesCounter, "change")));
        recalcDiffWithMyChildsCount();
    }

    public ObservationWrapper getObservationWrapper() {
        return requestParamsHolder.getObservationWrapper();
    }

    public Account getObservedAccount() {
        return getObservationWrapper().getObservedAccount();
    }
}