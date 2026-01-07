package com.github.mbto.eatlog.repository;

import com.github.jgonian.ipmath.Ipv4;
import com.github.mbto.eatlog.common.custommodel.InternalAccount;
import com.github.mbto.eatlog.common.dto.auth.YandexAuth;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.jooq.types.UInteger;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import static com.github.mbto.eatlog.common.model.eatlog.tables.Account.ACCOUNT;

@Repository
@RequiredArgsConstructor
public class AccountRepository {
    private final DSLContext eatlogDsl;

    public InternalAccount findByYandexId(YandexAuth yandexAuth, String ip) {
        return eatlogDsl.transactionResult(config -> {
            DSLContext transactionalDsl = DSL.using(config);
            InternalAccount internalAccount = fetchAccountByYandex(
                    transactionalDsl,
                    yandexAuth.getYandexId()
            );
            if(internalAccount == null) {
                return null;
            }
            transactionalDsl.update(ACCOUNT)
                    .set(ACCOUNT.SITE_AVATAR_URL, yandexAuth.getAvatarUrl())
                    .set(ACCOUNT.LASTAUTH_AT, DSL.field("now()", LocalDateTime.class))
                    .set(ACCOUNT.IP, UInteger.valueOf(Ipv4.of(ip).asBigInteger().longValue()))
                    .where(ACCOUNT.ID.eq(internalAccount.getId()))
                    .execute()
            ;
            return internalAccount;
        });
    }

    private InternalAccount fetchAccountByYandex(DSLContext dslContext, String yandexId) {
        return buildSelectAccountFields(dslContext)
                .where(ACCOUNT.SITE_AUTH_YANDEXID.eq(yandexId))
                .fetchOneInto(InternalAccount.class)
                ;
    }

    @NotNull
    private static SelectJoinStep<?> buildSelectAccountFields(DSLContext dslContext) {
        return dslContext.select(
                        ACCOUNT.ID,
                        ACCOUNT.CREATED_AT,
                        ACCOUNT.LASTAUTH_AT,
                        ACCOUNT.NAME,
                        ACCOUNT.SITE_AUTH_YANDEXID,
                        ACCOUNT.SITE_AVATAR_URL,
                        ACCOUNT.LOCALE_SEGMENTS,
                        ACCOUNT.ROLES,
                        ACCOUNT.IP,
                        DSL.coalesce(ACCOUNT.IS_BANNED, false)
                                .as(ACCOUNT.IS_BANNED),
                        ACCOUNT.BANNED_REASON,
                        ACCOUNT.GRADE_EATLOG
                )
                .from(ACCOUNT);
    }
}