package com.github.mbto.eatlog.service;

import com.github.mbto.eatlog.common.custommodel.InternalAccount;
import com.github.mbto.eatlog.common.dto.auth.GoogleAuth;
import com.github.mbto.eatlog.common.model.eatlog.tables.records.AccountRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.UpdateSetMoreStep;
import org.jooq.impl.DSL;
import org.jooq.types.UInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.mbto.eatlog.common.model.eatlog.tables.Account.ACCOUNT;
import static com.github.mbto.eatlog.common.model.eatlog.tables.Limitation.LIMITATION;
import static com.github.mbto.eatlog.webapp.enums.RoleEnum.OWNER;
import static com.github.mbto.eatlog.webapp.enums.RoleEnum.rolesContainerCreatorFunc;
import static com.github.mbto.eatlog.webapp.WebUtils.msgFromBundle;

@Service
@Lazy(false)
@Slf4j
public class AccountService {
    @Autowired
    private DSLContext eatlogDsl;
    @Autowired
    private ApplicationHolder applicationHolder;

    public InternalAccount findOrCreateAccount(String sub, String localeSegments, UInteger geonameId) {
        if(applicationHolder.isDevEnvironment()) {
            return findOrCreateAccount(new GoogleAuth(sub), localeSegments, geonameId);
        }
        throw new IllegalStateException("Required dev environment");
    }

    public InternalAccount findOrCreateAccount(GoogleAuth auth,
                                               String localeSegments,
                                               UInteger geonameId) {
        log.info("Try find/create account with auth=" + auth
                + ", localeSegments=" + localeSegments
                + ", geonameId=" + geonameId);
        AtomicBoolean accountCreated = new AtomicBoolean(false);
        InternalAccount internalAccount = eatlogDsl.transactionResult(config -> {
            DSLContext transactionalDsl = DSL.using(config);
            InternalAccount existedAccount = transactionalDsl.selectFrom(ACCOUNT)
                    .where(ACCOUNT.GOOGLE_SUB.eq(auth.getSub()))
                    .fetchOneInto(InternalAccount.class);
            if (existedAccount != null) {
                // use existed account
                UpdateSetMoreStep<AccountRecord> step = transactionalDsl.update(ACCOUNT)
                        .set(ACCOUNT.LASTAUTH_AT, DSL.currentTimestamp().cast(ACCOUNT.LASTAUTH_AT.getType()))
                        .set(ACCOUNT.GEONAME_ID, geonameId)
                        ;
                if(auth.getPicture() != null) {
                    //noinspection ResultOfMethodCallIgnored
                    step.set(ACCOUNT.GOOGLE_PICTURE_URL, auth.getPicture());
                    existedAccount.setGooglePictureUrl(auth.getPicture());
                }
                step.where(ACCOUNT.ID.eq(existedAccount.getId()))
                        .execute();
                return existedAccount;
            }
            // register new account
            AccountRecord accountRecord = transactionalDsl.insertInto(ACCOUNT)
                    .set(ACCOUNT.NAME, StringUtils.isBlank(auth.getName())
                            ? msgFromBundle("new.account") : auth.getName().trim())
                    .set(ACCOUNT.GOOGLE_SUB, auth.getSub())
                    .set(ACCOUNT.GOOGLE_PICTURE_URL, auth.getPicture())
                    .set(ACCOUNT.LOCALE_SEGMENTS, localeSegments)
                    .set(ACCOUNT.ROLES,
                            transactionalDsl.fetchExists(
                                    transactionalDsl.select(DSL.one())
                                            .from(ACCOUNT)
                                            .limit(1)
                            ) ? null // first account will be owner, others without roles
                              : Stream.of(OWNER.getRoleName())
                                    .collect(Collectors.toCollection(rolesContainerCreatorFunc))
                    )
                    .set(ACCOUNT.GEONAME_ID, geonameId)
                    .returning(ACCOUNT.asterisk())
                    .fetchOne();
            accountCreated.set(true);
            //noinspection DataFlowIssue
            transactionalDsl.insertInto(LIMITATION)
                    .set(LIMITATION.ACCOUNT_ID, accountRecord.getId())
                    .set(LIMITATION.TITLE, msgFromBundle("weight.loss"))
                    .set(LIMITATION.B, BigDecimal.valueOf(1.5))
                    .set(LIMITATION.J, BigDecimal.valueOf(0.5))
                    .set(LIMITATION.U, (BigDecimal) null)
                    .execute();
            return accountRecord.into(InternalAccount.class);
        });
        internalAccount.setGeonameId(geonameId);
        internalAccount.calcRoles();
        log.info("Successfully " + (accountCreated.get() ? "created" : "finded")
                + " account=" + internalAccount
                + " by auth=" + auth
                + ", localeSegments=" + localeSegments
                + ", geonameId=" + geonameId);
        return internalAccount;
    }
}