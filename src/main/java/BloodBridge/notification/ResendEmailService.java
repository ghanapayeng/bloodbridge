package BloodBridge.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Legacy email service class preserved for backward compatibility.
 * All email dispatch has been migrated to {@link GmailEmailService} using Gmail SMTP via {@link JavaMailSender}.
 *
 * @deprecated Replaced by {@link GmailEmailService} and {@link EmailService}.
 */
@Deprecated
public class ResendEmailService extends GmailEmailService {

    @Autowired
    public ResendEmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password) {
        super(mailSender, username, password);
    }
}
