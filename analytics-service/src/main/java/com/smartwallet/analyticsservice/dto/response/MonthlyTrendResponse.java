package com.smartwallet.analyticsservice.dto.response;

import com.smartwallet.analyticsservice.entity.CurrencyCode;

import java.util.List;

public record MonthlyTrendResponse (
        List<MonthlyTrendItemResponse>  months,
        CurrencyCode currency
){
}
