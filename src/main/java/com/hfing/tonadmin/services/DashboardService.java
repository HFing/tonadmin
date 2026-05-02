package com.hfing.tonadmin.services;

import com.hfing.tonadmin.dto.response.DashboardStats;

public interface DashboardService {

    DashboardStats getDashboardStats(String branchId);
}