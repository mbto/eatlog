package com.github.mbto.eatlog.webapp.view;

import com.github.mbto.eatlog.common.custommodel.ObservationWrapper;
import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Account;
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
import org.jooq.types.UInteger;
import org.primefaces.PrimeFaces;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.github.mbto.eatlog.common.Constants.*;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Product.PRODUCT;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Store.STORE;
import static com.github.mbto.eatlog.common.utils.ProjectUtils.declensionValuedL10N;
import static com.github.mbto.eatlog.common.utils.ProjectUtils.pointwiseUpdateQuery;
import static com.github.mbto.eatlog.webapp.WebUtils.msgFromBundle;
import static jakarta.faces.application.FacesMessage.SEVERITY_INFO;
import static jakarta.faces.application.FacesMessage.SEVERITY_WARN;

@ViewScoped
@Named
@Slf4j
public class ViewStore implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Autowired
    private SessionAccount sessionAccount;
    @Autowired
    private DSLContext eatlogDsl;
    @Autowired
    private RequestParamsHolder requestParamsHolder;

    @Getter
    private LazyDataModel<Store> childsLazyModel;
    @Getter
    @Setter
    private Store selectedChild;
    @Getter
    private int childRelations;

    private int localChangesCounter;

    public void fetch() {
        if (log.isDebugEnabled())
            log.debug("\nfetch");
        recreateChildsLazyModel();
        recalcChildsCountTotal();
    }

    private void recreateChildsLazyModel() {
        if (log.isDebugEnabled())
            log.debug("\nrecreateChildsLazyModel");
        childsLazyModel = new LazyDataModel<>() {
            private final boolean isDebugEnabled = log.isDebugEnabled();
            @Override
            public List<Store> load(int offset, int limit,
                                      Map<String, SortMeta> sortBy,
                                      Map<String, FilterMeta> filterBy) {
                if(isDebugEnabled) {
                    log.debug("\nload() offset=" + offset
                            + ", limit=" + limit
                            + ", getRowCount()=" + getRowCount());
                }
                return eatlogDsl.selectFrom(STORE)
                        .where(STORE.ACCOUNT_ID.eq(getObservedAccount().getId()))
                        .orderBy(STORE.TITLE)
                        .limit(offset, limit)
                        .fetchInto(Store.class);
            }
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                if(isDebugEnabled) {
                    log.debug("\ncount() getRowCount()=" + getRowCount());
                }
                return getRowCount();
            }
            @Override
            public String getRowKey(Store child) {
                if(isDebugEnabled) {
                    log.debug("\ngetRowKey() child=" + child);
                }
                return child.getId() != null ? child.getId().toString() : null;
            }
            @Override
            public Store getRowData(String rowKeyStr) {
                if(isDebugEnabled) {
                    log.debug("\ngetRowData() rowKeyStr=" + rowKeyStr);
                }
                UInteger rowKey = UInteger.valueOf(rowKeyStr);
                if(selectedChild != null && rowKey.equals(selectedChild.getId()))
                    return selectedChild;
                Store child = getWrappedData()
                        .stream()
                        .filter(o -> o.getId().equals(rowKey))
                        .findFirst()
                        .orElse(null);
                if(child != null)
                    return selectedChild = child;
                child = eatlogDsl.selectFrom(STORE)
                        .where(STORE.ID.eq(rowKey))
                        .fetchOneInto(Store.class);
                if(child == null)
                    throw new IllegalStateException(msgFromBundle("unable.find.store", rowKey));
                return selectedChild = child;
            }
        };
    }

    private void recalcChildsCountTotal() {
        if(log.isDebugEnabled())
            log.debug("\nrecalcChildsCountTotal");
        int childsCountTotal = eatlogDsl.fetchCount(STORE,
                STORE.ACCOUNT_ID.eq(getObservedAccount().getId()));
        childsLazyModel.setRowCount(childsCountTotal);
    }

    public void onChildSelect() {
        childRelations = eatlogDsl.fetchCount(PRODUCT, PRODUCT.STORE_ID.eq(selectedChild.getId()));
        if(childRelations == 0)
            return;
        FacesContext fc = FacesContext.getCurrentInstance();
        fc.addMessage(dialogMsgs, new FacesMessage(SEVERITY_INFO,
                msgFromBundle("info.delete.store", declensionValuedL10N(childRelations, "product")),
                ""));
    }

    public void add() {
        if (log.isDebugEnabled())
            log.debug("\nadd");
        final UInteger accountId = sessionAccount.getInternalAccount().getId();
        selectedChild = new Store();
        selectedChild.setAccountId(accountId);
    }

    public void saveOrUpdate() {
        log.info("Attempting save/update " + selectedChild);
        FacesContext fc = FacesContext.getCurrentInstance();
        PrimeFaces pf = PrimeFaces.current();
        localChangesCounter = 0;
        if (selectedChild.getId() == null) {
            try {
                localChangesCounter = eatlogDsl.insertInto(STORE)
                        .set(STORE.ACCOUNT_ID, selectedChild.getAccountId())
                        .set(STORE.TITLE, selectedChild.getTitle())
                        .execute();
            } catch (Throwable e) {
                log.warn("Failed save " + selectedChild, e);
                fc.addMessage(dialogMsgs, new FacesMessage(SEVERITY_WARN,
                        msgFromBundle("failed.save.store"), ""));
                return;
            }
            fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                    msgFromBundle("store.added"),
                    declensionValuedL10N(localChangesCounter, "change")));
        } else {
            try {
                //noinspection ArraysAsListWithZeroOrOneArgument
                localChangesCounter = pointwiseUpdateQuery(eatlogDsl, STORE.ID, selectedChild.getId(),
                        Arrays.asList(
                                Pair.of(STORE.TITLE, selectedChild.getTitle())
                        )
                );
            } catch (Throwable e) {
                log.warn("Failed update " + selectedChild, e);
                fc.addMessage(dialogMsgs, new FacesMessage(SEVERITY_WARN,
                        msgFromBundle("failed.update.store"), ""));
                return;
            }
            fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                    msgFromBundle("store.updated"),
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
            localChangesCounter = eatlogDsl.deleteFrom(STORE)
                    .where(STORE.ID.eq(selectedChild.getId()))
                    .execute();
        } catch (Throwable e) {
            log.warn("Failed delete " + selectedChild, e);
            fc.addMessage(dialogMsgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("failed.delete.store"), ""));
            return;
        }
        fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                msgFromBundle("store.deleted"),
                declensionValuedL10N(localChangesCounter, "change")));
        selectedChild = null;
        recalcChildsCountTotal();
        pf.ajax().update(editChildsForm_childsTable, msgs);
        pf.executeScript(hideEditChildDialogWidget);
    }

    public ObservationWrapper getObservationWrapper() {
        return requestParamsHolder.getObservationWrapper();
    }

    public Account getObservedAccount() {
        return getObservationWrapper().getObservedAccount();
    }
}