package BloodBridge.export;

import BloodBridge.donation.Donation;
import BloodBridge.donation.DonationRepository;
import BloodBridge.inventory.BloodInventory;
import BloodBridge.inventory.BloodInventoryRepository;
import BloodBridge.request.BloodRequest;
import BloodBridge.request.BloodRequestRepository;
import java.io.StringWriter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportService {

    private final BloodRequestRepository bloodRequestRepository;
    private final DonationRepository donationRepository;
    private final BloodInventoryRepository inventoryRepository;

    public ExportService(
            BloodRequestRepository bloodRequestRepository,
            DonationRepository donationRepository,
            BloodInventoryRepository inventoryRepository) {
        this.bloodRequestRepository = bloodRequestRepository;
        this.donationRepository = donationRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public String exportRequestsCsv() {
        List<BloodRequest> requests = bloodRequestRepository.findAll();
        StringWriter sw = new StringWriter();
        sw.write("Request ID,Created At,Requester Name,Requester Email,Blood Group,City,Hospital,Urgency,Status,Units Needed,Units Fulfilled,Risk Level,Stage,Patient Reference\n");

        for (BloodRequest r : requests) {
            sw.write(String.format("%d,%s,\"%s\",\"%s\",%s,\"%s\",\"%s\",%s,%s,%d,%d,%s,%s,\"%s\"\n",
                    r.getId(),
                    r.getCreatedAt(),
                    escapeCsv(r.getRequester().getFullName()),
                    escapeCsv(r.getRequester().getEmail()),
                    r.getBloodGroup() != null ? r.getBloodGroup().name() : "",
                    escapeCsv(r.getCity()),
                    escapeCsv(r.getHospital()),
                    r.getUrgency() != null ? r.getUrgency().name() : "",
                    r.getStatus() != null ? r.getStatus().name() : "",
                    r.getUnitsNeeded() != null ? r.getUnitsNeeded() : 0,
                    r.getUnitsFulfilled() != null ? r.getUnitsFulfilled() : 0,
                    r.getRiskLevel() != null ? r.getRiskLevel().name() : "",
                    r.getCurrentStage() != null ? r.getCurrentStage().name() : "",
                    escapeCsv(r.getPatientReference() != null ? r.getPatientReference() : "")
            ));
        }
        return sw.toString();
    }

    @Transactional(readOnly = true)
    public String exportDonationsCsv() {
        List<Donation> donations = donationRepository.findAll();
        StringWriter sw = new StringWriter();
        sw.write("Donation ID,Donation Date,Donor Name,Donor Email,Blood Group,Units,Hospital,City,Linked Request ID,Created At\n");

        for (Donation d : donations) {
            String hospital = d.getBloodRequest() != null ? d.getBloodRequest().getHospital() : "Standard Center";
            String city = d.getBloodRequest() != null ? d.getBloodRequest().getCity() : "";
            String bg = d.getBloodRequest() != null && d.getBloodRequest().getBloodGroup() != null
                    ? d.getBloodRequest().getBloodGroup().name() : "";

            sw.write(String.format("%d,%s,\"%s\",\"%s\",%s,%s,\"%s\",\"%s\",%s,%s\n",
                    d.getId(),
                    d.getDonatedAt(),
                    escapeCsv(d.getDonor().getFullName()),
                    escapeCsv(d.getDonor().getEmail()),
                    bg,
                    d.getUnits() != null ? d.getUnits().toString() : "1.0",
                    escapeCsv(hospital),
                    escapeCsv(city),
                    d.getBloodRequest() != null ? d.getBloodRequest().getId().toString() : "",
                    d.getCreatedAt()
            ));
        }
        return sw.toString();
    }

    @Transactional(readOnly = true)
    public String exportInventoryCsv() {
        List<BloodInventory> batches = inventoryRepository.findAll();
        StringWriter sw = new StringWriter();
        sw.write("Inventory ID,Batch Unit ID,Blood Group,Rh Type,Quantity Units,Facility Name,Status,Collection Date,Expiry Date\n");

        for (BloodInventory b : batches) {
            sw.write(String.format("%d,\"%s\",%s,\"%s\",%d,\"%s\",%s,%s,%s\n",
                    b.getId(),
                    escapeCsv(b.getBatchUnitId()),
                    b.getBloodGroup() != null ? b.getBloodGroup().name() : "",
                    escapeCsv(b.getRhType()),
                    b.getQuantity(),
                    escapeCsv(b.getFacilityName()),
                    b.getStatus() != null ? b.getStatus().name() : "",
                    b.getCollectionDate(),
                    b.getExpiryDate()
            ));
        }
        return sw.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
