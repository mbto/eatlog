package com.github.mbto.eatlog.dao;

import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class EatlogDao {
    @Autowired
    private DSLContext eatlogDsl;
}