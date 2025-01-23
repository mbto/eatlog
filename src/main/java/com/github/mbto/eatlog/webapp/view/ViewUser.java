package com.github.mbto.eatlog.webapp.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mbto.eatlog.common.custommodel.InternalAccount;
import com.github.mbto.eatlog.service.ApplicationHolder;
import com.github.mbto.eatlog.service.CacheService;
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
import org.springframework.util.CollectionUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

import static com.github.mbto.eatlog.common.Constants.*;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Account.ACCOUNT;
import static com.github.mbto.eatlog.common.utils.ProjectUtils.*;
import static com.github.mbto.eatlog.service.CacheService.*;
import static com.github.mbto.eatlog.webapp.WebUtils.msgFromBundle;
import static com.github.mbto.eatlog.webapp.WebUtils.updateSameAccountsInSessionsAndAddMsg;
import static jakarta.faces.application.FacesMessage.SEVERITY_INFO;
import static jakarta.faces.application.FacesMessage.SEVERITY_WARN;
import static org.apache.commons.lang3.StringUtils.isBlank;

@ViewScoped
@Named
@Slf4j
public class ViewUser implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Autowired
    private ApplicationHolder applicationHolder;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private SessionAccount sessionAccount;
    @Autowired
    private DSLContext eatlogDsl;
    @Autowired
    private ObjectMapper objectMapper;

    @Getter
    private LazyDataModel<InternalAccount> childsLazyModel;
    @Getter
    @Setter
    private InternalAccount selectedChild;
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
            public List<InternalAccount> load(int offset, int limit,
                                         Map<String, SortMeta> sortBy,
                                         Map<String, FilterMeta> filterBy) {
                if(isDebugEnabled) {
                    log.debug("\nload() offset=" + offset
                            + ", limit=" + limit
                            + ", getRowCount()=" + getRowCount());
                }
                Map<String, String> queryByFilename = applicationHolder.getQueryByFilename();
                List<InternalAccount> childs = eatlogDsl.resultQuery(
                        queryByFilename.get("all-accounts-with-weights_json"),
                        null,
                            DSL.val(offset),
                            DSL.val(limit)
                        ).fetchInto(InternalAccount.class);
                Set<UInteger> unresolvedGeonameIds = null;
                for (InternalAccount child : childs) {
                    child.calcRoles();
                    child.setupWeightChart(objectMapper);
                    UInteger geonameId = child.getGeonameId();
                    if(geonameId == null)
                        continue;
                    if(!cacheService.itemExists(geoInfoByGeonameIdCache, geonameId)) {
                        if(unresolvedGeonameIds == null)
                            unresolvedGeonameIds = new HashSet<>();
                        unresolvedGeonameIds.add(geonameId);
                    }
                }
                if(!CollectionUtils.isEmpty(unresolvedGeonameIds)) {
                    cacheService.getGeoInfoByGeonameIdMap(unresolvedGeonameIds, true);
                }
                return childs;
            }
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                if(isDebugEnabled) {
                    log.debug("\ncount() getRowCount()=" + getRowCount());
                }
                return getRowCount();
            }
            @Override
            public String getRowKey(InternalAccount child) {
                if(isDebugEnabled) {
                    log.debug("\ngetRowKey() child=" + child);
                }
                return child.getId() != null ? child.getId().toString() : null;
            }
            @Override
            public InternalAccount getRowData(String rowKeyStr) {
                if(isDebugEnabled) {
                    log.debug("\ngetRowData() rowKeyStr=" + rowKeyStr);
                }
                UInteger rowKey = UInteger.valueOf(rowKeyStr);
                if(selectedChild != null && rowKey.equals(selectedChild.getId()))
                    return selectedChild;
                InternalAccount child = getWrappedData()
                        .stream()
                        .filter(o -> o.getId().equals(rowKey))
                        .findFirst()
                        .orElse(null);
                if(child != null)
                    return selectedChild = child;
                String query = applicationHolder.getQueryByFilename().get("all-accounts-with-weights_json");
                child = eatlogDsl.resultQuery(query,
                        DSL.val(rowKey),
                        DSL.val(0),
                        DSL.val(1)
                ).fetchOneInto(InternalAccount.class);
                if(child == null)
                    throw new IllegalStateException(msgFromBundle("unable.find.account", rowKey));
                child.calcRoles();
                child.setupWeightChart(objectMapper);
                return selectedChild = child;
            }
        };
    }

    private void recalcChildsCountTotal() {
        if(log.isDebugEnabled())
            log.debug("\nrecalcChildsCountTotal");
        int childsCountTotal = eatlogDsl.fetchCount(ACCOUNT);
        childsLazyModel.setRowCount(childsCountTotal);
    }

    public void onBannedChanged() {
        if(selectedChild.getIsBanned()) {
            InternalAccount internalAccount = sessionAccount.getInternalAccount();
            String bannerName = internalAccount.getName() + " (" + internalAccount.getId() + ")";
            String bannedReason = selectedChild.getBannedReason();
            boolean emptyReason = isBlank(bannedReason);
            if(emptyReason || !bannedReason.contains(bannerName)) {
                selectedChild.setBannedReason(
                        (!emptyReason ? bannedReason + ", " : "")
                        + LocalDateTime.now().format(DDMMYYYY_HHMMSS_PATTERN)
                        + " by " + bannerName
                );
            }
        }
    }

    public void update() {
        log.info("Attempting update " + selectedChild);
        FacesContext fc = FacesContext.getCurrentInstance();
        PrimeFaces pf = PrimeFaces.current();
        localChangesCounter = 0;
        try {
            localChangesCounter = pointwiseUpdateQuery(eatlogDsl, ACCOUNT.ID, selectedChild.getId(),
                    Arrays.asList(
                            Pair.of(ACCOUNT.NAME, selectedChild.getName()),
                            Pair.of(ACCOUNT.ROLES, convertRolesToNullIfEmpty(selectedChild)),
                            Pair.of(ACCOUNT.IS_BANNED, convertIsBannedToNullIfFalse(selectedChild)),
                            Pair.of(ACCOUNT.BANNED_REASON, selectedChild.getBannedReason())
                    )
            );
        } catch (Throwable e) {
            log.warn("Failed update " + selectedChild, e);
            fc.addMessage(dialogMsgs, new FacesMessage(SEVERITY_WARN,
                    msgFromBundle("failed.update.account"), ""));
            return;
        }
        fc.addMessage(msgs, new FacesMessage(SEVERITY_INFO,
                msgFromBundle("account.updated"),
                declensionValuedL10N(localChangesCounter, "change")));
        if(localChangesCounter > 0) {
            cacheService.evict(observedAccountByIdCache, selectedChild.getId());
            updateSameAccountsInSessionsAndAddMsg(selectedChild,
                    sessionAccount.getInternalAccount().getCalcedRoles(),
                    dialogMsgs,
                    applicationHolder.getSiteVisitorBySessionId());
            cacheService.evict(hoursCache, allocateSiteVisitorsKey); // for resort siteVisitorsOutputPanelId after name changed
        }
        selectedChild = null;
        pf.ajax().update(editChildsForm_childsTable, msgs);
        pf.executeScript(hideEditChildDialogWidget);
    }
}