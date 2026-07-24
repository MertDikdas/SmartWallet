package com.smartwallet.analyticsservice.dto.response;

import java.util.List;

public record MonthlyTrendResponse (
        List<MonthlyTrendItemResponse>  months
){
}
