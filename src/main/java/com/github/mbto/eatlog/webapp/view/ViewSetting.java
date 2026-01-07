package com.github.mbto.eatlog.webapp.view;

import com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Setting;
import com.github.mbto.eatlog.service.CacheService;
import com.github.mbto.eatlog.webapp.session.SessionAccount;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;
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
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static com.github.mbto.eatlog.common.Constants.*;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Setting.SETTING;
import static com.github.mbto.eatlog.service.CacheService.objectBySettingKeyCache;
import static com.github.mbto.eatlog.utils.ProjectUtils.convertRolesToNullIfEmpty;
import static com.github.mbto.eatlog.utils.ProjectUtils.pointwiseUpdateQuery;
import static com.github.mbto.eatlog.webapp.DependentUtil.declensionValuedL10N;
import static com.github.mbto.eatlog.webapp.WebUtils.makeValidatorException;
import static com.github.mbto.eatlog.webapp.WebUtils.msgFromBundle;
import static jakarta.faces.application.FacesMessage.SEVERITY_INFO;
import static jakarta.faces.application.FacesMessage.SEVERITY_WARN;

@ViewScoped
@Named
@Slf4j
public class ViewSetting implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Autowired
    private CacheService cacheService;
    @Autowired
    private DSLContext eatlogDsl;
    @Autowired
    private SessionAccount sessionAccount;

    @Getter
    private LazyDataModel<Setting> childsLazyModel;
    @Getter
    @Setter
    private Setting selectedChild;
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
            public List<Setting> load(int offset, int limit,
                                   Map<String, SortMeta> sortBy,
                                   Map<String, FilterMeta> filterBy) {
                if(isDebugEnabled) {
                    log.debug("\nload() offset=" + offset
                            + ", limit=" + limit
                            + ", getRowCount()=" + getRowCount());
                }
                return eatlogDsl.selectFrom(SETTING)
                        .where(sessionAccount.getInternalAccount().getCalcedRoles().isOwner()
                                ? DSL.trueCondition()
                                : DSL.condition(DSL.function("JSON_OVERLAPS", Boolean.class,
                                SETTING.ROLES, DSL.val(sessionAccount.getInternalAccount().getRoles())))
                        )
                        .limit(offset, limit)
                        .fetchInto(Setting.class);
            }
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                if(isDebugEnabled) {
                    log.debug("\ncount() getRowCount()=" + getRowCount());
                }
                return getRowCount();
            }
            @Override
            public String getRowKey(Setting child) {
                if(isDebugEnabled) {
                    log.debug("\ngetRowKey() child=" + child);
                }
                return child.getId() != null ? child.getId().toString() : null;
            }
            @Override
            public Setting getRowData(String rowKeyStr) {
                if(isDebugEnabled) {
                    log.debug("\ngetRowData() rowKeyStr=" + rowKeyStr);
                }
                UInteger rowKey = UInteger.valueOf(rowKeyStr);
                if(selectedChild != null && rowKey.equals(selectedChild.getId()))
                    return selectedChild;
                Setting child = getWrappedData()
                        .stream()
                        .filter(o -> o.getId().equals(rowKey))
                        .findFirst()
                        .orElse(null);
                if(child != null)
                    return selectedChild = child;
                child = eatlogDsl.selectFrom(SETTING)
                        .where(SETTING.ID.eq(rowKey))
                        .fetchOneInto(Setting.class);
                if(child == null)
                    throw new IllegalStateException(msgFromBundle("unable.find.setting", rowKey));
                return selectedChild = child;
            }
        };
    }

    private void recalcChildsCountTotal() {
        if(log.isDebugEnabled())
            log.debug("\nrecalcChildsCountTotal");
        int childsCountTotal = eatlogDsl.fetchCount(SETTING,
                sessionAccount.getInternalAccount().getCalcedRoles().isOwner()
                        ? DSL.trueCondition()
                        : DSL.condition(DSL.function("JSON_OVERLAPS", Boolean.class,
                        SETTING.ROLES, DSL.val(sessionAccount.getInternalAccount().getRoles())))
        );
        childsLazyModel.setRowCount(childsCountTotal);
    }

    public void add() {
        if (log.isDebugEnabled())
            log.debug("\nadd");
        selectedChild = new Setting();
    }

    /**
     * invokes from template
     */
    @SuppressWarnings("unused")
    public void validateKey(FacesContext context, UIComponent component, String value) throws ValidatorException {
        UInteger existedId = eatlogDsl.select(SETTING.ID)
                .from(SETTING)
                .where(SETTING.KEY.eq(value),
                        selectedChild.getId() != null
                                ? SETTING.ID.notEqual(selectedChild.getId())
                                : DSL.trueCondition())
                .limit(1)
                .fetchOneInto(SETTING.ID.getType());
        if(existedId != null) {
            throw makeValidatorException(value,
                    msgFromBundle("setting.already.exists", existedId));
        }
    }

    public void saveOrUpdate() {
        TreeSet<String> rolesSet = convertRolesToNullIfEmpty(selectedChild);
        log.info("Attempting save/update " + selectedChild);
        FacesContext fc = FacesContext.getCurrentInstance();
        PrimeFaces pf = PrimeFaces.current();
        localChangesCounter = 0;
        if (selectedChild.getId() == null) {
            try {
                localChangesCounter = eatlogDsl.insertInto(SETTING)
                        .set(SETTING.KEY, selectedChild.getKey())
                        .set(SETTING.VALUE, selectedChild.getValue())
                        .set(SETTING.ROLES, rolesSet)
                        .execute();
            } catch (Throwable e) {
                log.warn("Failed save " + selectedChild, e);
                fc.addMessage(dialogMsgs, new FacesMessage(SEVERITY_WARN,
                        msgFromBundle("failed.save.setting"), ""));
                return;
            }
            fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                    msgFromBundle("setting.added"),
                    declensionValuedL10N(localChangesCounter, "change")));
        } else {
            try {
                localChangesCounter = pointwiseUpdateQuery(eatlogDsl, SETTING.ID, selectedChild.getId(),
                        Arrays.asList(
                                Pair.of(SETTING.KEY, selectedChild.getKey()),
                                Pair.of(SETTING.VALUE, selectedChild.getValue()),
                                Pair.of(SETTING.ROLES, rolesSet)
                        )
                );
            } catch (Throwable e) {
                log.warn("Failed update " + selectedChild, e);
                fc.addMessage(dialogMsgs, new FacesMessage(SEVERITY_WARN,
                        msgFromBundle("failed.update.setting"), ""));
                return;
            }
            fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                    msgFromBundle("setting.updated"),
                    declensionValuedL10N(localChangesCounter, "change")));
        }
        if(localChangesCounter > 0) {
            cacheService.evict(objectBySettingKeyCache, selectedChild.getKey());
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
            localChangesCounter = eatlogDsl.deleteFrom(SETTING)
                    .where(SETTING.ID.eq(selectedChild.getId()))
                    .execute();
        } catch (Throwable e) {
            log.warn("Failed delete " + selectedChild, e);
            fc.addMessage(dialogMsgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("failed.delete.setting"), ""));
            return;
        }
        fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                msgFromBundle("setting.deleted"),
                declensionValuedL10N(localChangesCounter, "change")));
        if(localChangesCounter > 0) {
            cacheService.evict(objectBySettingKeyCache, selectedChild.getKey());
        }
        selectedChild = null;
        recalcChildsCountTotal();
        pf.ajax().update(editChildsForm_childsTable, msgs);
        pf.executeScript(hideEditChildDialogWidget);
    }
}