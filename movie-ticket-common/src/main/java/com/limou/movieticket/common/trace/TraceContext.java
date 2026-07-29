package com.limou.movieticket.common.trace;

import org.slf4j.MDC;

public final class TraceContext {
    public static final String HEADER_NAME = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    private TraceContext() {
    }

    public static String getTraceId() {
        return MDC.get(MDC_KEY);
    }

    static void setTraceId(String traceId) {
        MDC.put(MDC_KEY, traceId);
    }

    static void clear() {
        MDC.remove(MDC_KEY);
    }
}
