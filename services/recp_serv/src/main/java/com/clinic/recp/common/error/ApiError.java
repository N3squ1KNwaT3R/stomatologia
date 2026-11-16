package com.clinic.recp.common.error;

public record ApiError(
        String detail,
        String code
) {
}
