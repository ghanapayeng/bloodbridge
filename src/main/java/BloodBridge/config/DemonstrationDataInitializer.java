package BloodBridge.config;

import BloodBridge.analytics.BloodDemandRecord;
import BloodBridge.analytics.BloodDemandRecordRepository;
import BloodBridge.common.BloodComponent;
import BloodBridge.common.BloodGroup;
import BloodBridge.inventory.BloodInventory;
import BloodBridge.inventory.BloodInventoryRepository;
import BloodBridge.inventory.InventoryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Initializes realistic synthetic demonstration data for blood inventory and AI demand forecasting.
 * Strictly self-contained database seeder without any external CSV or network dependencies.
 */
@Component
@Order(10)
public class DemonstrationDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemonstrationDataInitializer.class);

    private final BloodInventoryRepository inventoryRepository;
    private final BloodDemandRecordRepository demandRecordRepository;
    private final JdbcTemplate jdbcTemplate;

    public DemonstrationDataInitializer(
            BloodInventoryRepository inventoryRepository,
            BloodDemandRecordRepository demandRecordRepository,
            JdbcTemplate jdbcTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.demandRecordRepository = demandRecordRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureSchemaColumns();
        seedDemonstrationInventory();
        seedDemonstrationDemandRecords();
    }

    private void ensureSchemaColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE blood_inventories ADD COLUMN IF NOT EXISTS component VARCHAR(32) NOT NULL DEFAULT 'WHOLE_BLOOD'");
        } catch (Exception ignored) {
            // MySQL/H2 dialect fallback
            try {
                jdbcTemplate.execute("ALTER TABLE blood_inventories ADD COLUMN component VARCHAR(32) NOT NULL DEFAULT 'WHOLE_BLOOD'");
            } catch (Exception e) {
                log.debug("Column component exists or altered: {}", e.getMessage());
            }
        }

        try {
            jdbcTemplate.execute("ALTER TABLE blood_inventories ADD COLUMN IF NOT EXISTS is_demo BOOLEAN NOT NULL DEFAULT FALSE");
        } catch (Exception ignored) {
            try {
                jdbcTemplate.execute("ALTER TABLE blood_inventories ADD COLUMN is_demo BOOLEAN NOT NULL DEFAULT FALSE");
            } catch (Exception e) {
                log.debug("Column is_demo exists or altered: {}", e.getMessage());
            }
        }
    }

    private void seedDemonstrationInventory() {
        if (inventoryRepository.countByIsDemoTrue() > 0) {
            log.info("Demonstration inventory records already exist in database (count > 0). Skipping re-seed.");
            return;
        }

        log.info("Seeding realistic demonstration blood inventory across all 8 blood groups and 5 components...");

        LocalDate today = LocalDate.now();
        List<BloodInventory> batches = new ArrayList<>();

        // O+ (High demand, abundant stock)
        batches.add(new BloodInventory(BloodGroup.O_POSITIVE, "+", BloodComponent.PRBC, 24,
                today.minusDays(5), today.plusDays(37), "DEMO-INV-OPOS-PRBC-01", "Apex National Blood Center", InventoryStatus.AVAILABLE, true));
        batches.add(new BloodInventory(BloodGroup.O_POSITIVE, "+", BloodComponent.PLATELETS, 7,
                today.minusDays(2), today.plusDays(3), "DEMO-INV-OPOS-PLT-02", "City General Hospital Blood Bank", InventoryStatus.AVAILABLE, true));
        batches.add(new BloodInventory(BloodGroup.O_POSITIVE, "+", BloodComponent.FFP, 16,
                today.minusDays(18), today.plusDays(347), "DEMO-INV-OPOS-FFP-03", "Metro Red Cross Blood Bank", InventoryStatus.AVAILABLE, true));

        // O- (CRITICAL SHORTAGE - Universal donor, high emergency consumption)
        batches.add(new BloodInventory(BloodGroup.O_NEGATIVE, "-", BloodComponent.PRBC, 2,
                today.minusDays(3), today.plusDays(39), "DEMO-INV-ONEG-PRBC-01", "City General Hospital Blood Bank", InventoryStatus.AVAILABLE, true));
        batches.add(new BloodInventory(BloodGroup.O_NEGATIVE, "-", BloodComponent.WHOLE_BLOOD, 1,
                today.minusDays(10), today.plusDays(25), "DEMO-INV-ONEG-WB-02", "Sunrise Care Hospital", InventoryStatus.AVAILABLE, true));

        // B+ (Highest population prevalence in India ~33%, strong stock)
        batches.add(new BloodInventory(BloodGroup.B_POSITIVE, "+", BloodComponent.PRBC, 32,
                today.minusDays(4), today.plusDays(38), "DEMO-INV-BPOS-PRBC-01", "Apex National Blood Center", InventoryStatus.AVAILABLE, true));
        batches.add(new BloodInventory(BloodGroup.B_POSITIVE, "+", BloodComponent.PLATELETS, 8,
                today.minusDays(1), today.plusDays(4), "DEMO-INV-BPOS-PLT-02", "City General Hospital Blood Bank", InventoryStatus.AVAILABLE, true));
        batches.add(new BloodInventory(BloodGroup.B_POSITIVE, "+", BloodComponent.FFP, 22,
                today.minusDays(14), today.plusDays(351), "DEMO-INV-BPOS-FFP-03", "Metro Red Cross Blood Bank", InventoryStatus.AVAILABLE, true));
        batches.add(new BloodInventory(BloodGroup.B_POSITIVE, "+", BloodComponent.CRYOPRECIPITATE, 12,
                today.minusDays(25), today.plusDays(340), "DEMO-INV-BPOS-CRYO-04", "Apex National Blood Center", InventoryStatus.AVAILABLE, true));

        // B- (LOW STOCK)
        batches.add(new BloodInventory(BloodGroup.B_NEGATIVE, "-", BloodComponent.PRBC, 3,
                today.minusDays(6), today.plusDays(36), "DEMO-INV-BNEG-PRBC-01", "Metro Red Cross Blood Bank", InventoryStatus.AVAILABLE, true));
        batches.add(new BloodInventory(BloodGroup.B_NEGATIVE, "-", BloodComponent.WHOLE_BLOOD, 2,
                today.minusDays(8), today.plusDays(27), "DEMO-INV-BNEG-WB-02", "City General Hospital Blood Bank", InventoryStatus.AVAILABLE, true));

        // A+ (Moderate stock)
        batches.add(new BloodInventory(BloodGroup.A_POSITIVE, "+", BloodComponent.PRBC, 20,
                today.minusDays(7), today.plusDays(35), "DEMO-INV-APOS-PRBC-01", "Apex National Blood Center", InventoryStatus.AVAILABLE, true));
        batches.add(new BloodInventory(BloodGroup.A_POSITIVE, "+", BloodComponent.FFP, 14,
                today.minusDays(10), today.plusDays(355), "DEMO-INV-APOS-FFP-02", "City General Hospital Blood Bank", InventoryStatus.AVAILABLE, true));
        batches.add(new BloodInventory(BloodGroup.A_POSITIVE, "+", BloodComponent.CRYOPRECIPITATE, 6,
                today.minusDays(15), today.plusDays(350), "DEMO-INV-APOS-CRYO-03", "Metro Red Cross Blood Bank", InventoryStatus.AVAILABLE, true));

        // A- (LOW STOCK)
        batches.add(new BloodInventory(BloodGroup.A_NEGATIVE, "-", BloodComponent.PRBC, 4,
                today.minusDays(4), today.plusDays(38), "DEMO-INV-ANEG-PRBC-01", "Sunrise Care Hospital", InventoryStatus.AVAILABLE, true));
        batches.add(new BloodInventory(BloodGroup.A_NEGATIVE, "-", BloodComponent.PLATELETS, 2,
                today.minusDays(2), today.plusDays(3), "DEMO-INV-ANEG-PLT-02", "Apex National Blood Center", InventoryStatus.AVAILABLE, true));

        // AB+ (Universal plasma donor)
        batches.add(new BloodInventory(BloodGroup.AB_POSITIVE, "+", BloodComponent.FFP, 18,
                today.minusDays(8), today.plusDays(357), "DEMO-INV-ABPOS-FFP-01", "Metro Red Cross Blood Bank", InventoryStatus.AVAILABLE, true));
        batches.add(new BloodInventory(BloodGroup.AB_POSITIVE, "+", BloodComponent.WHOLE_BLOOD, 9,
                today.minusDays(12), today.plusDays(23), "DEMO-INV-ABPOS-WB-02", "City General Hospital Blood Bank", InventoryStatus.AVAILABLE, true));

        // AB- (CRITICAL SHORTAGE - Rarest blood group ~1%)
        batches.add(new BloodInventory(BloodGroup.AB_NEGATIVE, "-", BloodComponent.PRBC, 1,
                today.minusDays(7), today.plusDays(35), "DEMO-INV-ABNEG-PRBC-01", "Apex National Blood Center", InventoryStatus.AVAILABLE, true));
        batches.add(new BloodInventory(BloodGroup.AB_NEGATIVE, "-", BloodComponent.FFP, 2,
                today.minusDays(22), today.plusDays(343), "DEMO-INV-ABNEG-FFP-02", "City General Hospital Blood Bank", InventoryStatus.AVAILABLE, true));

        // Demonstration Reserved and Issued workflow units
        batches.add(new BloodInventory(BloodGroup.B_POSITIVE, "+", BloodComponent.PRBC, 4,
                today.minusDays(4), today.plusDays(38), "DEMO-INV-BPOS-RES-01", "Apex National Blood Center", InventoryStatus.RESERVED, true));
        batches.add(new BloodInventory(BloodGroup.O_POSITIVE, "+", BloodComponent.PLATELETS, 5,
                today.minusDays(3), today.plusDays(2), "DEMO-INV-OPOS-TRF-01", "City General Hospital Blood Bank", InventoryStatus.ISSUED, true));

        inventoryRepository.saveAll(batches);
        log.info("Successfully seeded {} demonstration inventory batches.", batches.size());
    }

    private void seedDemonstrationDemandRecords() {
        if (demandRecordRepository.count() > 0) {
            log.info("Blood demand records already present in database (count > 0). Skipping re-seed.");
            return;
        }

        log.info("Seeding realistic historical and multi-horizon projected blood demand records...");

        LocalDate today = LocalDate.now();
        List<BloodDemandRecord> records = new ArrayList<>();

        // Generate past 18 months of historical records + 6 upcoming projected months
        for (int monthOffset = -18; monthOffset <= 6; monthOffset++) {
            LocalDate monthDate = today.plusMonths(monthOffset).withDayOfMonth(1);
            int year = monthDate.getYear();
            int month = monthDate.getMonthValue();
            boolean isForecast = monthOffset > 0;

            // Seasonal multipliers:
            // Monsoon (July - October / months 7-10): Dengue surge (+35% demand, especially Platelets)
            // Summer (May - June / months 5-6): Donation drop (+15% demand pressure)
            // Winter/Festive (November - December / months 11-12): Festival activities
            double seasonalFactor = 1.0;
            String seasonNotes = "Standard baseline clinical consumption";
            if (month >= 7 && month <= 10) {
                seasonalFactor = 1.38;
                seasonNotes = "Monsoon surge: Vector-borne illnesses (dengue, malaria) causing high platelet velocity";
            } else if (month == 5 || month == 6) {
                seasonalFactor = 1.15;
                seasonNotes = "Summer seasonal voluntary donation dip creates reserve supply pressure";
            } else if (month == 11 || month == 12) {
                seasonalFactor = 1.08;
                seasonNotes = "Year-end festive surgeries and trauma emergency preparedness";
            }

            if (isForecast) {
                seasonNotes = "[AI DEMO PROJECTION] " + seasonNotes;
            }

            // Distribute demand across all 8 blood groups and 5 components
            for (BloodGroup bg : BloodGroup.values()) {
                double groupShare;
                switch (bg) {
                    case B_POSITIVE -> groupShare = 0.33;
                    case O_POSITIVE -> groupShare = 0.31;
                    case A_POSITIVE -> groupShare = 0.22;
                    case AB_POSITIVE -> groupShare = 0.08;
                    case O_NEGATIVE -> groupShare = 0.025;
                    case B_NEGATIVE -> groupShare = 0.018;
                    case A_NEGATIVE -> groupShare = 0.012;
                    case AB_NEGATIVE -> groupShare = 0.005;
                    default -> groupShare = 0.10;
                }

                for (BloodComponent comp : BloodComponent.values()) {
                    double compShare;
                    switch (comp) {
                        case PRBC -> compShare = 0.44;
                        case WHOLE_BLOOD -> compShare = 0.24;
                        case PLATELETS -> compShare = (month >= 7 && month <= 10) ? 0.26 : 0.16;
                        case FFP -> compShare = 0.10;
                        case CRYOPRECIPITATE -> compShare = 0.06;
                        default -> compShare = 0.20;
                    }

                    // Total baseline monthly hospital units across region ~ 650 units
                    double baselineMonthlyUnits = 650.0;
                    int demanded = (int) Math.round(baselineMonthlyUnits * groupShare * compShare * seasonalFactor);
                    if (demanded < 1) demanded = 1;

                    // Supplied units (historical reflects realistic shortages in rare groups and monsoon peaks)
                    int supplied;
                    if (isForecast) {
                        supplied = 0; // future demand has not yet occurred
                    } else {
                        if (bg == BloodGroup.O_NEGATIVE || bg == BloodGroup.AB_NEGATIVE) {
                            supplied = (int) Math.round(demanded * 0.78); // historical shortage in rare types
                        } else if (comp == BloodComponent.PLATELETS && (month >= 7 && month <= 10)) {
                            supplied = (int) Math.round(demanded * 0.84); // platelet shortage during monsoon dengue spike
                        } else {
                            supplied = (int) Math.round(demanded * 0.96);
                        }
                    }

                    records.add(new BloodDemandRecord(
                            year,
                            month,
                            monthDate,
                            bg,
                            comp,
                            demanded,
                            supplied,
                            isForecast,
                            "Regional Blood Transfusion Center",
                            seasonNotes,
                            true
                    ));
                }
            }
        }

        demandRecordRepository.saveAll(records);
        log.info("Successfully seeded {} realistic historical and forecast blood demand records.", records.size());
    }
}
