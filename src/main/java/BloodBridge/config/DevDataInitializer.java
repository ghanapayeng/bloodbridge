package BloodBridge.config;

import BloodBridge.auth.AuthDtos.RegisterRequest;
import BloodBridge.auth.AuthService;
import BloodBridge.auth.UserAccount;
import BloodBridge.auth.UserAccountRepository;
import BloodBridge.common.BloodGroup;
import BloodBridge.donor.DonorDtos.UpsertDonorProfileRequest;
import BloodBridge.donor.DonorProfileService;
import BloodBridge.request.BloodRequestDtos.CreateBloodRequest;
import BloodBridge.request.BloodRequestService;
import BloodBridge.request.RequestUrgency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private final UserAccountRepository userRepository;
    private final AuthService authService;
    private final DonorProfileService donorProfileService;
    private final BloodRequestService bloodRequestService;

    @Value("${bloodbridge.dev.seed-data:false}")
    private boolean seedData;

    public DevDataInitializer(
            UserAccountRepository userRepository,
            AuthService authService,
            DonorProfileService donorProfileService,
            BloodRequestService bloodRequestService) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.donorProfileService = donorProfileService;
        this.bloodRequestService = bloodRequestService;
    }

    @Override
    public void run(String... args) {
        if (!seedData || userRepository.count() > 0) {
            return;
        }

        log.info("Seeding BloodBridge demo / development data...");

        // Create Demo account
        createUserWithProfile("Demo User", "demo@bloodbridge.org", "BloodBridge123!",
                BloodGroup.B_POSITIVE, "+91 98765 43210", "New Delhi", true, LocalDate.now().minusDays(45));

        // Create Sample Donors
        createUserWithProfile("Arjun Sharma", "arjun@example.com", "BloodBridge123!",
                BloodGroup.B_POSITIVE, "+91 98111 22233", "New Delhi", true, LocalDate.now().minusDays(60));

        createUserWithProfile("Sana Khan", "sana@example.com", "BloodBridge123!",
                BloodGroup.B_POSITIVE, "+91 98222 33344", "New Delhi", true, LocalDate.now().minusDays(90));

        createUserWithProfile("Rahul Mehta", "rahul@example.com", "BloodBridge123!",
                BloodGroup.O_NEGATIVE, "+91 98333 44455", "New Delhi", true, LocalDate.now().minusDays(120));

        createUserWithProfile("Meera Kapoor", "meera@example.com", "BloodBridge123!",
                BloodGroup.A_POSITIVE, "+91 98444 55566", "Noida", true, LocalDate.now().minusDays(30));

        createUserWithProfile("Vikram Singh", "vikram@example.com", "BloodBridge123!",
                BloodGroup.AB_POSITIVE, "+91 98555 66677", "Gurugram", true, null);

        // Create sample blood requests by doctors / requesters
        bloodRequestService.create("rahul@example.com", new CreateBloodRequest(
                BloodGroup.B_POSITIVE,
                "New Delhi",
                "City Hospital",
                RequestUrgency.HIGH,
                "Urgent requirement for surgical patient in ICU. Any compatible donor in Delhi please assist."));

        bloodRequestService.create("meera@example.com", new CreateBloodRequest(
                BloodGroup.O_NEGATIVE,
                "New Delhi",
                "AIIMS",
                RequestUrgency.CRITICAL,
                "Emergency transfusion needed for trauma patient. Universal donors urgently requested."));

        bloodRequestService.create("arjun@example.com", new CreateBloodRequest(
                BloodGroup.A_POSITIVE,
                "Noida",
                "Fortis Hospital",
                RequestUrgency.MEDIUM,
                "Scheduled surgery next week. Looking for 2 units of compatible blood."));

        log.info("BloodBridge sample data created successfully.");
    }

    private void createUserWithProfile(
            String fullName,
            String email,
            String password,
            BloodGroup bloodGroup,
            String phone,
            String city,
            boolean available,
            LocalDate lastDonationDate) {
        UserAccount user = authService.register(new RegisterRequest(fullName, email, password));
        donorProfileService.saveMine(user.getEmail(), new UpsertDonorProfileRequest(
                bloodGroup, phone, city, available, lastDonationDate));
    }
}
