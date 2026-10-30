package BloodBridge.analytics;

import BloodBridge.analytics.AnalyticsDtos.BloodGroupDemandForecast;
import BloodBridge.analytics.AnalyticsDtos.DemandForecastResponse;
import BloodBridge.analytics.ForecastDtos.*;
import BloodBridge.common.BloodComponent;
import BloodBridge.common.BloodGroup;
import BloodBridge.inventory.BloodInventory;
import BloodBridge.inventory.BloodInventoryRepository;
import BloodBridge.inventory.InventoryStatus;
import BloodBridge.request.BloodRequest;
import BloodBridge.request.BloodRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DemandPredictionService {

    private final BloodRequestRepository bloodRequestRepository;
    private final BloodInventoryRepository inventoryRepository;
    private final BloodDemandRecordRepository demandRecordRepository;

    public DemandPredictionService(
            BloodRequestRepository bloodRequestRepository,
            BloodInventoryRepository inventoryRepository,
            BloodDemandRecordRepository demandRecordRepository) {
        this.bloodRequestRepository = bloodRequestRepository;
        this.inventoryRepository = inventoryRepository;
        this.demandRecordRepository = demandRecordRepository;
    }

    /**
     * Complete Summary of Blood Demand Forecasting including National Planning figures,
     * multi-horizon predictions (7d, 30d, 90d), and active shortage counts.
     */
    @Transactional(readOnly = true)
    public ForecastSummaryResponse getForecastSummary() {
        NationalPlanningFigures nationalFigures = NationalPlanningFigures.createDefault();

        List<BloodInventory> available = inventoryRepository.findByStatus(InventoryStatus.AVAILABLE);
        int totalCurrentInventory = available.stream().mapToInt(BloodInventory::getQuantity).sum();

        List<BloodGroupDemandDetail> groupDetails = getBloodGroupDemandBreakdown();

        int totalHistorical30d = groupDetails.stream().mapToInt(BloodGroupDemandDetail::historical30DayDemandUnits).sum();
        int total7d = groupDetails.stream().mapToInt(BloodGroupDemandDetail::forecast7DayDemandUnits).sum();
        int total30d = groupDetails.stream().mapToInt(BloodGroupDemandDetail::forecast30DayDemandUnits).sum();
        int total90d = groupDetails.stream().mapToInt(BloodGroupDemandDetail::forecast90DayDemandUnits).sum();

        List<ShortageAlertDto> shortageAlerts = getShortageAlerts();
        int activeShortagesCount = shortageAlerts.size();

        // Identify highest demand blood group
        BloodGroupDemandDetail highestGroup = groupDetails.stream()
                .max(Comparator.comparingInt(BloodGroupDemandDetail::forecast30DayDemandUnits))
                .orElse(null);

        String highestBg = highestGroup != null ? highestGroup.bloodGroup().name() : "B_POSITIVE";
        String highestBgDisplay = highestGroup != null ? highestGroup.bloodGroupDisplay() : "B+";

        // Identify highest demand component
        List<ComponentDemandDetail> componentDetails = getComponentDemandBreakdown();
        ComponentDemandDetail highestComponent = componentDetails.stream()
                .max(Comparator.comparingInt(ComponentDemandDetail::forecast30DayDemandUnits))
                .orElse(null);
        String highestCompName = highestComponent != null ? highestComponent.componentName() : "PRBC";

        List<String> recommendations = new ArrayList<>();
        if (!shortageAlerts.isEmpty()) {
            for (ShortageAlertDto alert : shortageAlerts) {
                if ("CRITICAL".equals(alert.severity())) {
                    recommendations.add("Priority Critical: Immediate targeted donor drive needed for "
                            + alert.bloodGroupDisplay() + " (" + alert.componentName()
                            + ") to bridge projected " + alert.projectedDeficitUnits() + "-unit deficit.");
                }
            }
        }
        recommendations.add("Maintain rolling buffer for B+ and O+ which together represent >64% of regional acute trauma consumption.");
        recommendations.add("Monitor platelet shelf-life closely (5-day limit); prioritize dynamic cross-hospital sharing to minimize discard rates.");

        return new ForecastSummaryResponse(
                nationalFigures,
                totalCurrentInventory,
                totalHistorical30d,
                total7d,
                total30d,
                total90d,
                activeShortagesCount,
                highestBg,
                highestBgDisplay,
                highestCompName,
                recommendations,
                true,
                "Time-series trend model with demographic prevalence & seasonal clinical adjustments (Classroom Demonstration)"
        );
    }

    /**
     * Monthly historical demand trends and upcoming projections with seasonal factor annotations.
     */
    @Transactional(readOnly = true)
    public List<MonthlyDemandTrendDto> getMonthlyDemandTrends() {
        List<BloodDemandRecord> records = demandRecordRepository.findAllByOrderByPeriodDateAsc();
        if (records.isEmpty()) {
            return generateFallbackMonthlyTrends();
        }

        Map<String, MonthlyDemandTrendDto> monthlyMap = new LinkedHashMap<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy");

        for (BloodDemandRecord r : records) {
            String key = r.getPeriodYear() + "-" + String.format("%02d", r.getPeriodMonth());
            String label = r.getPeriodDate().format(labelFormatter);

            MonthlyDemandTrendDto existing = monthlyMap.get(key);
            if (existing == null) {
                monthlyMap.put(key, new MonthlyDemandTrendDto(
                        r.getPeriodYear(),
                        r.getPeriodMonth(),
                        label,
                        r.getUnitsDemanded(),
                        r.getUnitsSupplied(),
                        r.isForecast(),
                        r.getNotes() != null ? r.getNotes() : "Baseline consumption"
                ));
            } else {
                int totalDemand = existing.demandUnits() + r.getUnitsDemanded();
                int totalSupplied = existing.suppliedUnits() + r.getUnitsSupplied();
                monthlyMap.put(key, new MonthlyDemandTrendDto(
                        existing.year(),
                        existing.month(),
                        existing.monthLabel(),
                        totalDemand,
                        totalSupplied,
                        existing.isForecast(),
                        existing.seasonalContext()
                ));
            }
        }

        return new ArrayList<>(monthlyMap.values());
    }

    /**
     * Demand forecast breakdown across all 8 blood groups.
     */
    @Transactional(readOnly = true)
    public List<BloodGroupDemandDetail> getBloodGroupDemandBreakdown() {
        List<BloodInventory> available = inventoryRepository.findByStatus(InventoryStatus.AVAILABLE);

        // Calculate available stock per blood group and component breakdown
        Map<BloodGroup, Integer> stockMap = new EnumMap<>(BloodGroup.class);
        Map<BloodGroup, Map<String, Integer>> componentMap = new EnumMap<>(BloodGroup.class);
        for (BloodGroup bg : BloodGroup.values()) {
            stockMap.put(bg, 0);
            componentMap.put(bg, new LinkedHashMap<>());
        }

        for (BloodInventory item : available) {
            BloodGroup bg = item.getBloodGroup();
            stockMap.put(bg, stockMap.getOrDefault(bg, 0) + item.getQuantity());

            String compCode = item.getComponent() != null ? item.getComponent().getCode() : "WB";
            Map<String, Integer> compStock = componentMap.get(bg);
            compStock.put(compCode, compStock.getOrDefault(compCode, 0) + item.getQuantity());
        }

        // Aggregate 30-day demand from records or requests
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<Object[]> demandRows = demandRecordRepository.sumDemandByBloodGroupSince(thirtyDaysAgo);
        Map<BloodGroup, Integer> historicalMap = new EnumMap<>(BloodGroup.class);
        for (Object[] row : demandRows) {
            BloodGroup bg = (BloodGroup) row[0];
            int count = ((Number) row[1]).intValue();
            historicalMap.put(bg, count);
        }

        List<BloodGroupDemandDetail> results = new ArrayList<>();

        for (BloodGroup bg : BloodGroup.values()) {
            double prevalence;
            int base30dEstimate;

            switch (bg) {
                case B_POSITIVE -> { prevalence = 33.0; base30dEstimate = 215; }
                case O_POSITIVE -> { prevalence = 31.0; base30dEstimate = 200; }
                case A_POSITIVE -> { prevalence = 22.0; base30dEstimate = 142; }
                case AB_POSITIVE -> { prevalence = 8.0; base30dEstimate = 52; }
                case O_NEGATIVE -> { prevalence = 2.5; base30dEstimate = 18; }
                case B_NEGATIVE -> { prevalence = 1.8; base30dEstimate = 12; }
                case A_NEGATIVE -> { prevalence = 1.2; base30dEstimate = 8; }
                case AB_NEGATIVE -> { prevalence = 0.5; base30dEstimate = 4; }
                default -> { prevalence = 10.0; base30dEstimate = 50; }
            }

            int past30d = historicalMap.getOrDefault(bg, base30dEstimate);

            int forecast7d = (int) Math.ceil((past30d / 30.0) * 7.0);
            int forecast30d = past30d;
            int forecast90d = (int) Math.round(past30d * 3.1);

            int currentStock = stockMap.getOrDefault(bg, 0);

            int deficit7d = Math.max(0, forecast7d - currentStock);
            int deficit30d = Math.max(0, forecast30d - currentStock);

            String severity;
            String recommendation;

            if (currentStock < forecast7d) {
                severity = "CRITICAL_DEFICIT";
                recommendation = "CRITICAL: Current inventory (" + currentStock + " units) fails to cover the 7-day projected requirement of "
                        + forecast7d + " units. Immediate donor callout required for " + bg.getDisplay() + ".";
            } else if (currentStock < forecast30d) {
                severity = "MODERATE_DEFICIT";
                recommendation = "WATCH: Short-term 7-day demand is secured (" + currentStock + "/" + forecast7d
                        + " units), but reserve buffer falls below 30-day projection (" + forecast30d + " units).";
            } else {
                severity = "OPTIMAL";
                recommendation = "OPTIMAL: Healthy reserve margin. Inventory (" + currentStock
                        + " units) comfortably meets operational requirements.";
            }

            results.add(new BloodGroupDemandDetail(
                    bg,
                    bg.getDisplay(),
                    prevalence,
                    currentStock,
                    past30d,
                    forecast7d,
                    forecast30d,
                    forecast90d,
                    deficit7d,
                    deficit30d,
                    severity,
                    recommendation,
                    componentMap.get(bg)
            ));
        }

        return results;
    }

    /**
     * Demand forecast breakdown across all 5 blood components.
     */
    @Transactional(readOnly = true)
    public List<ComponentDemandDetail> getComponentDemandBreakdown() {
        List<BloodInventory> available = inventoryRepository.findByStatus(InventoryStatus.AVAILABLE);

        Map<BloodComponent, Integer> stockMap = new EnumMap<>(BloodComponent.class);
        for (BloodComponent c : BloodComponent.values()) {
            stockMap.put(c, 0);
        }

        for (BloodInventory item : available) {
            BloodComponent c = item.getComponent() != null ? item.getComponent() : BloodComponent.WHOLE_BLOOD;
            stockMap.put(c, stockMap.getOrDefault(c, 0) + item.getQuantity());
        }

        List<ComponentDemandDetail> list = new ArrayList<>();

        for (BloodComponent comp : BloodComponent.values()) {
            int currentStock = stockMap.getOrDefault(comp, 0);
            int forecast7d;
            int forecast30d;
            int forecast90d;
            int safeBuffer;
            String clinical;

            switch (comp) {
                case PRBC -> {
                    forecast7d = 65;
                    forecast30d = 280;
                    forecast90d = 850;
                    safeBuffer = 40;
                    clinical = "Severe anemia, major surgeries, acute obstetric hemorrhage, and trauma resuscitation.";
                }
                case WHOLE_BLOOD -> {
                    forecast7d = 35;
                    forecast30d = 150;
                    forecast90d = 460;
                    safeBuffer = 20;
                    clinical = "Massive hemorrhage protocols, military trauma, and exchange transfusions.";
                }
                case PLATELETS -> {
                    forecast7d = 28;
                    forecast30d = 120;
                    forecast90d = 380;
                    safeBuffer = 15;
                    clinical = "Dengue thrombocytopenia, chemotherapy-induced aplasia, and bleeding diathesis.";
                }
                case FFP -> {
                    forecast7d = 18;
                    forecast30d = 75;
                    forecast90d = 230;
                    safeBuffer = 12;
                    clinical = "Multiple coagulation factor deficiencies, warfarin reversal, and liver failure.";
                }
                case CRYOPRECIPITATE -> {
                    forecast7d = 10;
                    forecast30d = 45;
                    forecast90d = 140;
                    safeBuffer = 8;
                    clinical = "Hypofibrinogenemia, Hemophilia A, von Willebrand disease, and DIC.";
                }
                default -> {
                    forecast7d = 20;
                    forecast30d = 80;
                    forecast90d = 250;
                    safeBuffer = 10;
                    clinical = "General therapeutic clinical indications.";
                }
            }

            boolean shortageRisk = currentStock < safeBuffer;

            list.add(new ComponentDemandDetail(
                    comp,
                    comp.getDisplayName(),
                    comp.getCode(),
                    comp.getShelfLifeDays(),
                    currentStock,
                    forecast7d,
                    forecast30d,
                    forecast90d,
                    safeBuffer,
                    shortageRisk,
                    clinical
            ));
        }

        return list;
    }

    /**
     * Active shortage alerts highlighting high-risk deficits across blood groups and components.
     */
    @Transactional(readOnly = true)
    public List<ShortageAlertDto> getShortageAlerts() {
        List<BloodGroupDemandDetail> groupDetails = getBloodGroupDemandBreakdown();
        List<ShortageAlertDto> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        int alertSeq = 1;

        for (BloodGroupDemandDetail gd : groupDetails) {
            if ("CRITICAL_DEFICIT".equals(gd.shortageSeverity())) {
                alerts.add(new ShortageAlertDto(
                        "SHORTAGE-CRIT-" + alertSeq++,
                        "CRITICAL",
                        gd.bloodGroup(),
                        gd.bloodGroupDisplay(),
                        BloodComponent.PRBC,
                        "PRBC",
                        gd.currentInventoryUnits(),
                        gd.forecast7DayDemandUnits(),
                        gd.deficit7DayUnits(),
                        "CRITICAL DEFICIT: Urgent " + gd.bloodGroupDisplay() + " Shortage",
                        "Stock cannot meet 7-day demand (" + gd.currentInventoryUnits() + "/" + gd.forecast7DayDemandUnits()
                                + " units). Activate immediate targeted volunteer donor outreach.",
                        today.plusDays(4)
                ));
            } else if ("MODERATE_DEFICIT".equals(gd.shortageSeverity()) && gd.currentInventoryUnits() < 10) {
                alerts.add(new ShortageAlertDto(
                        "SHORTAGE-MOD-" + alertSeq++,
                        "MODERATE",
                        gd.bloodGroup(),
                        gd.bloodGroupDisplay(),
                        BloodComponent.WHOLE_BLOOD,
                        "Whole Blood",
                        gd.currentInventoryUnits(),
                        gd.forecast7DayDemandUnits(),
                        Math.max(1, gd.deficit30DayUnits() / 4),
                        "MODERATE RISK: Low Reserve Buffer for " + gd.bloodGroupDisplay(),
                        "Stock is currently under 30-day recommended quota (" + gd.currentInventoryUnits() + "/" + gd.forecast30DayDemandUnits()
                                + " units). Schedule blood donor camp within 14 days.",
                        today.plusDays(10)
                ));
            }
        }

        // Check Platelet short-life expiry alert
        List<ComponentDemandDetail> components = getComponentDemandBreakdown();
        for (ComponentDemandDetail cd : components) {
            if (cd.component() == BloodComponent.PLATELETS && cd.shortageRisk()) {
                alerts.add(new ShortageAlertDto(
                        "SHORTAGE-COMP-" + alertSeq++,
                        "CRITICAL",
                        BloodGroup.O_POSITIVE,
                        "All Groups (O+/B+ Focus)",
                        BloodComponent.PLATELETS,
                        "Platelets",
                        cd.currentInventoryUnits(),
                        cd.forecast7DayDemandUnits(),
                        cd.safeBufferThresholdUnits() - cd.currentInventoryUnits(),
                        "PLATELET DEFICIT: Platelet reserves below minimum emergency threshold",
                        "High platelet turnover and 5-day shelf-life require immediate apheresis donor scheduling.",
                        today.plusDays(2)
                ));
            }
        }

        return alerts;
    }

    /**
     * Backward-compatible demand forecast endpoint for existing modal integration.
     */
    @Transactional(readOnly = true)
    public DemandForecastResponse calculateForecast() {
        List<BloodGroupDemandDetail> details = getBloodGroupDemandBreakdown();
        List<BloodInventory> available = inventoryRepository.findByStatus(InventoryStatus.AVAILABLE);
        int totalInventory = available.stream().mapToInt(BloodInventory::getQuantity).sum();

        int totalHistorical30d = details.stream().mapToInt(BloodGroupDemandDetail::historical30DayDemandUnits).sum();
        int totalProjectedMonthly = details.stream().mapToInt(BloodGroupDemandDetail::forecast30DayDemandUnits).sum();

        List<BloodGroupDemandForecast> forecasts = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        for (BloodGroupDemandDetail d : details) {
            boolean shortageRisk = "CRITICAL_DEFICIT".equals(d.shortageSeverity());
            forecasts.add(new BloodGroupDemandForecast(
                    d.bloodGroup(),
                    d.bloodGroupDisplay(),
                    d.currentInventoryUnits(),
                    d.historical30DayDemandUnits(),
                    d.forecast7DayDemandUnits(),
                    d.forecast30DayDemandUnits(),
                    shortageRisk,
                    d.deficit7DayUnits(),
                    d.recommendation()
            ));

            if (shortageRisk) {
                recommendations.add("Priority Alert: Organize urgent blood drive for " + d.bloodGroupDisplay()
                        + " (Est. 7-day deficit: " + d.deficit7DayUnits() + " units).");
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Inventory levels across all blood groups are currently operating within safe baseline margins.");
        }

        return new DemandForecastResponse(
                totalInventory,
                totalHistorical30d,
                totalProjectedMonthly,
                forecasts,
                recommendations
        );
    }

    private List<MonthlyDemandTrendDto> generateFallbackMonthlyTrends() {
        List<MonthlyDemandTrendDto> fallback = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = -12; i <= 6; i++) {
            LocalDate dt = today.plusMonths(i).withDayOfMonth(1);
            boolean isFc = i > 0;
            int demand = 550 + (int)(Math.sin(i) * 90) + (isFc ? 40 : 0);
            int supplied = isFc ? 0 : (int)(demand * 0.94);
            fallback.add(new MonthlyDemandTrendDto(
                    dt.getYear(),
                    dt.getMonthValue(),
                    dt.format(fmt),
                    demand,
                    supplied,
                    isFc,
                    isFc ? "[DEMO PROJECTION] Trend model" : "Historical baseline"
            ));
        }
        return fallback;
    }
}
