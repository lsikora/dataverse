package edu.harvard.iq.dataverse.metrics;

import javax.faces.view.ViewScoped;
import org.primefaces.model.chart.BarChartModel;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ViewScoped
@Named("FilesStorageChart")
public class PublishedFilesStorageChart implements Serializable {

    private ChartCreator chartCreator;
    private MetricsServiceBean metricsService;

    private final String CHART_TYPE = "publishedFilesStorage";

    private BarChartModel chartModel;
    private List<ChartMetrics> yearlyStats = new ArrayList<>();
    private List<ChartMetrics> chartMetrics = new ArrayList<>();

    private String mode = "YEAR";
    private int selectedYear;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public PublishedFilesStorageChart() {
    }

    @Inject
    public PublishedFilesStorageChart(ChartCreator chartCreator, MetricsServiceBean metricsService) {
        this.chartCreator = chartCreator;
        this.metricsService = metricsService;
    }

    // -------------------- GETTERS --------------------
    public BarChartModel getChartModel() {
        return chartModel;
    }

    public List<ChartMetrics> getYearlyStats() {
        return yearlyStats;
    }

    public String getMode() {
        return mode;
    }

    public int getSelectedYear() {
        return selectedYear;
    }

    // -------------------- LOGIC --------------------
    public void init() {

        chartMetrics = metricsService.countPublishedFilesStorage();

        if (chartMetrics.isEmpty()) {
            yearlyStats.add(new ChartMetrics((double) LocalDateTime.now().getYear(), 0L));
            selectedYear = LocalDate.now().getYear();
        } else {
            yearlyStats = MetricsUtil.countMetricsPerYearAndFillMissingYearsDescending(chartMetrics);
            selectedYear = yearlyStats.get(0).getYear();
        }

        chartModel = chartCreator.createYearlyChart(chartMetrics, CHART_TYPE);
    }

    public void changeChartGrouping() {
        if (isYearlyChartSelected()) {
            chartModel = chartCreator.createYearlyChart(chartMetrics, CHART_TYPE);

        } else if (isMonthlyChartSelected()) {
            chartModel = chartCreator.createMonthlyChart(chartMetrics, selectedYear, CHART_TYPE);
        }
    }

    // -------------------- PRIVATE ---------------------
    private boolean isMonthlyChartSelected() {
        return selectedYear != 0;
    }

    private boolean isYearlyChartSelected() {
        return mode.equals("YEAR");
    }

    // -------------------- SETTERS --------------------
    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setSelectedYear(int selectedYear) {
        this.selectedYear = selectedYear;
    }

    public void setYearlyStats(List<ChartMetrics> yearlyStats) {
        this.yearlyStats = yearlyStats;
    }
}
