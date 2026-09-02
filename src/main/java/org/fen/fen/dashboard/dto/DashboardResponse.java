package org.fen.fen.dashboard.dto;

import org.fen.fen.domain.StatusServicoFarmaceutico;

import java.util.List;

public record DashboardResponse(
        Indicators indicators,
        List<ServiceTypeCount> serviceTypes,
        List<StatusCount> statuses
) {
    public record Indicators(
            long awaitingReturn,
            long returnsToday,
            long totalAttendances,
            long expired
    ) {
    }

    public record ServiceTypeCount(String type, long count) {
    }

    public record StatusCount(StatusServicoFarmaceutico status, long count) {
    }
}
