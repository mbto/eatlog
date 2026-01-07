package com.github.mbto.eatlog.common.dto;

import com.github.mbto.eatlog.utils.geoip.GeoInfo;
import lombok.Getter;
import lombok.Setter;
import org.jooq.types.UInteger;

import java.util.Objects;

@Getter
@Setter
public class IpWrapper {
    private UInteger ip;
    private GeoInfo geoInfo;

    public IpWrapper() {
    }

    public IpWrapper(UInteger ip) {
        this.ip = ip;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof IpWrapper ipWrapper)) return false;
        return Objects.equals(geoInfo, ipWrapper.geoInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(geoInfo);
    }

    @Override
    public String toString() {
        return "IpWrapper{" +
               "ip=" + ip +
               ", geoInfo=" + (geoInfo != null ? "exists" : null) +
               '}';
    }
}