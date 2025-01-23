/*
 * This file is generated by jOOQ.
 */
package com.github.mbto.eatlog.common.model.eatlog.tables;


import com.github.mbto.eatlog.common.model.eatlog.Eatlog;
import com.github.mbto.eatlog.common.model.eatlog.Keys;
import com.github.mbto.eatlog.common.model.eatlog.tables.records.LimitationRecord;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function9;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row9;
import org.jooq.Schema;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Limitation extends TableImpl<LimitationRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>eatlog.limitation</code>
     */
    public static final Limitation LIMITATION = new Limitation();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<LimitationRecord> getRecordType() {
        return LimitationRecord.class;
    }

    /**
     * The column <code>eatlog.limitation.id</code>.
     */
    public final TableField<LimitationRecord, UInteger> ID = createField(DSL.name("id"), SQLDataType.INTEGERUNSIGNED.nullable(false).identity(true), this, "");

    /**
     * The column <code>eatlog.limitation.account_id</code>.
     */
    public final TableField<LimitationRecord, UInteger> ACCOUNT_ID = createField(DSL.name("account_id"), SQLDataType.INTEGERUNSIGNED.nullable(false), this, "");

    /**
     * The column <code>eatlog.limitation.title</code>.
     */
    public final TableField<LimitationRecord, String> TITLE = createField(DSL.name("title"), SQLDataType.VARCHAR(32).nullable(false), this, "");

    /**
     * The column <code>eatlog.limitation.b</code>.
     */
    public final TableField<LimitationRecord, BigDecimal> B = createField(DSL.name("b"), SQLDataType.DECIMAL(5, 2), this, "");

    /**
     * The column <code>eatlog.limitation.j</code>.
     */
    public final TableField<LimitationRecord, BigDecimal> J = createField(DSL.name("j"), SQLDataType.DECIMAL(5, 2), this, "");

    /**
     * The column <code>eatlog.limitation.u</code>.
     */
    public final TableField<LimitationRecord, BigDecimal> U = createField(DSL.name("u"), SQLDataType.DECIMAL(5, 2), this, "");

    /**
     * The column <code>eatlog.limitation.b_to_kkal</code>.
     */
    public final TableField<LimitationRecord, BigDecimal> B_TO_KKAL = createField(DSL.name("b_to_kkal"), SQLDataType.DECIMAL(6, 2), this, "");

    /**
     * The column <code>eatlog.limitation.j_to_kkal</code>.
     */
    public final TableField<LimitationRecord, BigDecimal> J_TO_KKAL = createField(DSL.name("j_to_kkal"), SQLDataType.DECIMAL(6, 2), this, "");

    /**
     * The column <code>eatlog.limitation.u_to_kkal</code>.
     */
    public final TableField<LimitationRecord, BigDecimal> U_TO_KKAL = createField(DSL.name("u_to_kkal"), SQLDataType.DECIMAL(6, 2), this, "");

    private Limitation(Name alias, Table<LimitationRecord> aliased) {
        this(alias, aliased, null);
    }

    private Limitation(Name alias, Table<LimitationRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>eatlog.limitation</code> table reference
     */
    public Limitation(String alias) {
        this(DSL.name(alias), LIMITATION);
    }

    /**
     * Create an aliased <code>eatlog.limitation</code> table reference
     */
    public Limitation(Name alias) {
        this(alias, LIMITATION);
    }

    /**
     * Create a <code>eatlog.limitation</code> table reference
     */
    public Limitation() {
        this(DSL.name("limitation"), null);
    }

    public <O extends Record> Limitation(Table<O> child, ForeignKey<O, LimitationRecord> key) {
        super(child, key, LIMITATION);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Eatlog.EATLOG;
    }

    @Override
    public Identity<LimitationRecord, UInteger> getIdentity() {
        return (Identity<LimitationRecord, UInteger>) super.getIdentity();
    }

    @Override
    public UniqueKey<LimitationRecord> getPrimaryKey() {
        return Keys.KEY_LIMITATION_PRIMARY;
    }

    @Override
    public List<ForeignKey<LimitationRecord, ?>> getReferences() {
        return Arrays.asList(Keys.LIMITATION_ACCOUNT_ACCOUNT_ID_FK);
    }

    private transient Account _account;

    /**
     * Get the implicit join path to the <code>eatlog.account</code> table.
     */
    public Account account() {
        if (_account == null)
            _account = new Account(this, Keys.LIMITATION_ACCOUNT_ACCOUNT_ID_FK);

        return _account;
    }

    @Override
    public Limitation as(String alias) {
        return new Limitation(DSL.name(alias), this);
    }

    @Override
    public Limitation as(Name alias) {
        return new Limitation(alias, this);
    }

    @Override
    public Limitation as(Table<?> alias) {
        return new Limitation(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Limitation rename(String name) {
        return new Limitation(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Limitation rename(Name name) {
        return new Limitation(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Limitation rename(Table<?> name) {
        return new Limitation(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row9 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row9<UInteger, UInteger, String, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal> fieldsRow() {
        return (Row9) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function9<? super UInteger, ? super UInteger, ? super String, ? super BigDecimal, ? super BigDecimal, ? super BigDecimal, ? super BigDecimal, ? super BigDecimal, ? super BigDecimal, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function9<? super UInteger, ? super UInteger, ? super String, ? super BigDecimal, ? super BigDecimal, ? super BigDecimal, ? super BigDecimal, ? super BigDecimal, ? super BigDecimal, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
