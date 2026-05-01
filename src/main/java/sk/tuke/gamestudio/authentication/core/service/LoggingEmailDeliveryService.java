package sk.tuke.gamestudio.authentication.core.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoggingEmailDeliveryService implements EmailDeliveryService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String appBaseUrl;
    private final String frontendUrl;
    private final boolean allowLogFallback;

    public LoggingEmailDeliveryService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:}") String fromAddress,
            @Value("${app.base-url:}") String appBaseUrl,
            @Value("${app.frontend-url:}") String frontendUrl,
            @Value("${spring.mail.username:}") String smtpUsername,
            @Value("${app.mail.allow-log-fallback:false}") boolean allowLogFallback
    ) {
        this.mailSender = mailSender;
        String base = appBaseUrl == null ? "" : appBaseUrl.trim();
        String frontend = frontendUrl == null ? "" : frontendUrl.trim();
        this.appBaseUrl = !base.isEmpty() ? base : frontend;
        this.frontendUrl = !frontend.isEmpty() ? frontend : base;
        this.allowLogFallback = allowLogFallback;

        String from = fromAddress == null ? "" : fromAddress.trim();
        String username = smtpUsername == null ? "" : smtpUsername.trim();
        this.fromAddress = from.isEmpty() ? username : from;
    }

    @Override
    public void sendEmailVerification(String email, String username, String verificationToken) {
        String verifyUrl = verificationLink(verificationToken);
        String subject = "Potvrdenie registrácie — Flood Fill";
        String htmlBody = buildVerificationEmailHtml(username, verifyUrl);
        String textFallback = "Vitajte vo Flood Fill, " + username + "!\n\n"
                + "Potvrďte svoj účet kliknutím na odkaz:\n" + verifyUrl
                + "\n\nOdkaz platí 24 hodín.\n\n"
                + "Ak ste registráciu nevykonali, tento email ignorujte.";
        sendHtmlOrLog(email, subject, htmlBody, textFallback);
    }

    @Override
    public void sendPasswordReset(String email, String resetToken) {
        String base = frontendUrl == null ? "" : frontendUrl.trim();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String resetUrl = base + "/?reset-token=" + resetToken;
        String subject = "Obnova hesla — Flood Fill";
        String htmlBody = buildPasswordResetEmailHtml(resetUrl);
        String textFallback = "Dostali sme žiadosť o obnovu hesla pre váš účet.\n\n"
                + "Kliknite na odkaz nižšie pre nastavenie nového hesla:\n" + resetUrl
                + "\n\nOdkaz je platný 1 hodinu a môže byť použitý iba raz.\n\n"
                + "Ak ste žiadosť neposlali vy, tento email ignorujte.";
        sendHtmlOrLog(email, subject, htmlBody, textFallback);
    }

    // ── HTML send ────────────────────────────────────────────────────────────

    private void sendHtmlOrLog(String to, String subject, String htmlBody, String textFallback) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (!fromAddress.isEmpty()) helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textFallback, htmlBody);
            mailSender.send(message);
            log.info("HTML email sent successfully to {} with subject '{}'", to, subject);
        } catch (MessagingException | MailException e) {
            handleFailure(to, subject, textFallback, e.getMessage());
        }
    }

    // ── Template ─────────────────────────────────────────────────────────────

    private String buildVerificationEmailHtml(String username, String verifyUrl) {
        String u = escapeHtml(username);
        String url = escapeHtml(verifyUrl);
        return """
<!DOCTYPE html>
<html lang="sk">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1.0"/>
<title>Vitajte vo Flood Fill</title>
</head>
<body style="margin:0;padding:0;background:#0f0f13;font-family:'Segoe UI',Arial,sans-serif;-webkit-font-smoothing:antialiased;">
<table width="100%" cellpadding="0" cellspacing="0" border="0" style="background:#0f0f13;padding:40px 16px 60px;">
<tr><td align="center">
<table width="620" cellpadding="0" cellspacing="0" border="0" style="max-width:620px;">

  <!-- HEADER -->
  <tr><td style="background:linear-gradient(135deg,#1a1a2e 0%,#16213e 50%,#0f3460 100%);border-radius:20px 20px 0 0;padding:48px 40px 40px;text-align:center;">
    <!-- Logo -->
    <table cellpadding="0" cellspacing="0" border="0" style="margin:0 auto 28px;">
      <tr>
        <td>
          <table cellpadding="0" cellspacing="0" border="0" style="display:inline-table;">
            <tr>
              <td style="width:27px;height:27px;background:#ef4444;border-radius:4px;"></td>
              <td style="width:3px;"></td>
              <td style="width:27px;height:27px;background:#22c55e;border-radius:4px;"></td>
            </tr>
            <tr><td colspan="3" style="height:3px;"></td></tr>
            <tr>
              <td style="width:27px;height:27px;background:#3b82f6;border-radius:4px;"></td>
              <td style="width:3px;"></td>
              <td style="width:27px;height:27px;background:#eab308;border-radius:4px;"></td>
            </tr>
          </table>
        </td>
        <td style="width:14px;"></td>
        <td style="font-size:34px;font-weight:800;letter-spacing:-1px;color:#ffffff;vertical-align:middle;">
          Flood<span style="color:#818cf8;">Fill</span>
        </td>
      </tr>
    </table>
    <!-- Heading -->
    <h1 style="margin:0 0 10px;font-size:30px;font-weight:800;color:#ffffff;line-height:1.25;">
      Vitajte medzi nami!
      <span style="font-style:normal;">&#127918;</span>
    </h1>
    <p style="margin:0;font-size:15px;color:#94a3b8;">Váš účet bol úspešne vytvorený a čaká na vás.</p>
  </td></tr>

  <!-- BODY -->
  <tr><td style="background:#16161f;padding:40px;border:1px solid rgba(255,255,255,0.06);border-top:none;">

    <!-- Greeting -->
    <p style="margin:0 0 28px;font-size:16px;color:#cbd5e1;line-height:1.7;">
      Zdravím, <strong style="color:#e2e8f0;">""" + u + """
</strong> &#128075;<br><br>
      Tešíme sa, že ste sa rozhodli pripojiť k Flood Fill — miestu, kde sa stretáva stratégia,
      farby a súťažný duch. Váš účet je aktívny a čaká na vás.
    </p>

    <!-- Success banner -->
    <table width="100%" cellpadding="0" cellspacing="0" border="0"
           style="background:linear-gradient(135deg,rgba(34,197,94,.12),rgba(16,185,129,.08));
                  border:1px solid rgba(34,197,94,.25);border-radius:14px;margin-bottom:32px;">
      <tr>
        <td style="padding:20px 24px;">
          <table cellpadding="0" cellspacing="0" border="0"><tr>
            <td style="width:42px;height:42px;background:linear-gradient(135deg,#22c55e,#10b981);
                       border-radius:10px;text-align:center;vertical-align:middle;font-size:20px;
                       flex-shrink:0;">&#10003;</td>
            <td style="width:16px;"></td>
            <td>
              <strong style="display:block;font-size:15px;font-weight:700;color:#4ade80;margin-bottom:3px;">
                Registrácia prebehla úspešne
              </strong>
              <span style="font-size:13px;color:#86efac;">
                Váš email bol overený &middot; Účet je plne aktívny
              </span>
            </td>
          </tr></table>
        </td>
      </tr>
    </table>

    <!-- Divider -->
    <div style="height:1px;background:linear-gradient(90deg,transparent,rgba(255,255,255,.08),transparent);margin:0 0 32px;"></div>

    <!-- CTA -->
    <div style="text-align:center;margin:36px 0 8px;">
      <a href=\"""" + url + """
\"
         style="display:inline-block;background:linear-gradient(135deg,#6366f1 0%,#8b5cf6 100%);
                color:#ffffff;text-decoration:none;font-size:18px;font-weight:700;
                letter-spacing:.5px;padding:20px 72px;border-radius:50px;
                box-shadow:0 8px 32px rgba(99,102,241,.35);">
        &#128640;&nbsp;&nbsp;Začať hrať
      </a>
    </div>

    <!-- Divider -->
    <div style="height:1px;background:linear-gradient(90deg,transparent,rgba(255,255,255,.08),transparent);margin:32px 0;"></div>

    <!-- Security -->
    <table width="100%" cellpadding="0" cellspacing="0" border="0"
           style="background:rgba(251,191,36,.05);border:1px solid rgba(251,191,36,.15);border-radius:14px;">
      <tr><td style="padding:20px 24px;">
        <table cellpadding="0" cellspacing="0" border="0"><tr>
          <td style="font-size:22px;vertical-align:top;padding-right:14px;">&#128274;</td>
          <td>
            <strong style="display:block;font-size:13px;font-weight:700;color:#fbbf24;margin-bottom:5px;">
              Bezpečnosť vášho účtu
            </strong>
            <p style="margin:0;font-size:13px;color:#78716c;line-height:1.7;">
              Ak si sa nezaregistroval/a ty, jednoducho tento email ignoruj — žiadna akcia nie je potrebná.<br>
              Platnosť overovacieho odkazu vyprší za 24 hodín.<br>
              Nikdy nezdieľaj tento odkaz s nikým iným.
            </p>
          </td>
        </tr></table>
      </td></tr>
    </table>

  </td></tr>

  <!-- FOOTER -->
  <tr><td style="background:#111118;border:1px solid rgba(255,255,255,.06);border-top:none;
                 border-radius:0 0 20px 20px;padding:28px 40px;text-align:center;">
    <!-- Footer logo -->
    <table cellpadding="0" cellspacing="0" border="0" style="margin:0 auto 12px;">
      <tr>
        <td>
          <table cellpadding="0" cellspacing="0" border="0">
            <tr>
              <td style="width:10px;height:10px;background:#ef4444;border-radius:2px;"></td>
              <td style="width:2px;"></td>
              <td style="width:10px;height:10px;background:#22c55e;border-radius:2px;"></td>
            </tr>
            <tr><td colspan="3" style="height:2px;"></td></tr>
            <tr>
              <td style="width:10px;height:10px;background:#3b82f6;border-radius:2px;"></td>
              <td style="width:2px;"></td>
              <td style="width:10px;height:10px;background:#eab308;border-radius:2px;"></td>
            </tr>
          </table>
        </td>
        <td style="width:8px;"></td>
        <td style="font-size:14px;font-weight:700;color:#475569;vertical-align:middle;">FloodFill</td>
      </tr>
    </table>
    <p style="margin:0;font-size:12px;color:#334155;line-height:1.7;">
      &copy; 2026 Flood Fill &middot; TUKE Game Studio<br>
      Tento email bol odoslaný automaticky &mdash; neodpovedaj na&#328;.
    </p>
  </td></tr>

</table>
</td></tr>
</table>
</body>
</html>
""";
    }

    private String buildPasswordResetEmailHtml(String resetUrl) {
        String url = escapeHtml(resetUrl);
        return """
<!DOCTYPE html>
<html lang="sk">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1.0"/>
<title>Obnova hesla — Flood Fill</title>
</head>
<body style="margin:0;padding:0;background:#0f0f13;font-family:'Segoe UI',Arial,sans-serif;-webkit-font-smoothing:antialiased;">
<table width="100%" cellpadding="0" cellspacing="0" border="0" style="background:#0f0f13;padding:40px 16px 60px;">
<tr><td align="center">
<table width="620" cellpadding="0" cellspacing="0" border="0" style="max-width:620px;">

  <!-- HEADER -->
  <tr><td style="background:linear-gradient(135deg,#1a1a2e 0%,#16213e 50%,#0f3460 100%);border-radius:20px 20px 0 0;padding:48px 40px 40px;text-align:center;">
    <table cellpadding="0" cellspacing="0" border="0" style="margin:0 auto 28px;">
      <tr>
        <td>
          <table cellpadding="0" cellspacing="0" border="0" style="display:inline-table;">
            <tr>
              <td style="width:27px;height:27px;background:#ef4444;border-radius:4px;"></td>
              <td style="width:3px;"></td>
              <td style="width:27px;height:27px;background:#22c55e;border-radius:4px;"></td>
            </tr>
            <tr><td colspan="3" style="height:3px;"></td></tr>
            <tr>
              <td style="width:27px;height:27px;background:#3b82f6;border-radius:4px;"></td>
              <td style="width:3px;"></td>
              <td style="width:27px;height:27px;background:#eab308;border-radius:4px;"></td>
            </tr>
          </table>
        </td>
        <td style="width:14px;"></td>
        <td style="font-size:34px;font-weight:800;letter-spacing:-1px;color:#ffffff;vertical-align:middle;">
          Flood<span style="color:#818cf8;">Fill</span>
        </td>
      </tr>
    </table>
    <h1 style="margin:0 0 10px;font-size:28px;font-weight:800;color:#ffffff;line-height:1.25;">
      Obnova hesla &#128274;
    </h1>
    <p style="margin:0;font-size:15px;color:#94a3b8;">Dostali sme žiadosť o reset vášho hesla.</p>
  </td></tr>

  <!-- BODY -->
  <tr><td style="background:#16161f;padding:40px;border:1px solid rgba(255,255,255,0.06);border-top:none;">

    <p style="margin:0 0 28px;font-size:16px;color:#cbd5e1;line-height:1.7;">
      Kliknite na tlačidlo nižšie a nastavte si nové heslo.
      Odkaz je platný <strong style="color:#e2e8f0;">1 hodinu</strong> a môže byť použitý <strong style="color:#e2e8f0;">iba raz</strong>.
    </p>

    <!-- CTA -->
    <div style="text-align:center;margin:36px 0;">
      <a href=\"""" + url + """
\"
         style="display:inline-block;background:linear-gradient(135deg,#6366f1 0%,#8b5cf6 100%);
                color:#ffffff;text-decoration:none;font-size:17px;font-weight:700;
                letter-spacing:.5px;padding:18px 64px;border-radius:50px;
                box-shadow:0 8px 32px rgba(99,102,241,.35);">
        &#128273;&nbsp;&nbsp;Nastaviť nové heslo
      </a>
    </div>

    <!-- Divider -->
    <div style="height:1px;background:linear-gradient(90deg,transparent,rgba(255,255,255,.08),transparent);margin:32px 0;"></div>

    <!-- Warning -->
    <table width="100%" cellpadding="0" cellspacing="0" border="0"
           style="background:rgba(251,191,36,.05);border:1px solid rgba(251,191,36,.15);border-radius:14px;">
      <tr><td style="padding:20px 24px;">
        <table cellpadding="0" cellspacing="0" border="0"><tr>
          <td style="font-size:22px;vertical-align:top;padding-right:14px;">&#9888;&#65039;</td>
          <td>
            <strong style="display:block;font-size:13px;font-weight:700;color:#fbbf24;margin-bottom:5px;">
              Dôležité upozornenie
            </strong>
            <p style="margin:0;font-size:13px;color:#78716c;line-height:1.7;">
              Ak ste o obnovu hesla nepožiadali vy, tento email ignorujte — vaše heslo zostáva nezmenené.<br>
              Platnosť odkazu vyprší za 1 hodinu.<br>
              Nikdy nezdieľajte tento odkaz s nikým iným.
            </p>
          </td>
        </tr></table>
      </td></tr>
    </table>

  </td></tr>

  <!-- FOOTER -->
  <tr><td style="background:#111118;border:1px solid rgba(255,255,255,.06);border-top:none;
                 border-radius:0 0 20px 20px;padding:28px 40px;text-align:center;">
    <table cellpadding="0" cellspacing="0" border="0" style="margin:0 auto 12px;">
      <tr>
        <td>
          <table cellpadding="0" cellspacing="0" border="0">
            <tr>
              <td style="width:10px;height:10px;background:#ef4444;border-radius:2px;"></td>
              <td style="width:2px;"></td>
              <td style="width:10px;height:10px;background:#22c55e;border-radius:2px;"></td>
            </tr>
            <tr><td colspan="3" style="height:2px;"></td></tr>
            <tr>
              <td style="width:10px;height:10px;background:#3b82f6;border-radius:2px;"></td>
              <td style="width:2px;"></td>
              <td style="width:10px;height:10px;background:#eab308;border-radius:2px;"></td>
            </tr>
          </table>
        </td>
        <td style="width:8px;"></td>
        <td style="font-size:14px;font-weight:700;color:#475569;vertical-align:middle;">FloodFill</td>
      </tr>
    </table>
    <p style="margin:0;font-size:12px;color:#334155;line-height:1.7;">
      &copy; 2026 Flood Fill &middot; TUKE Game Studio<br>
      Tento email bol odoslaný automaticky &mdash; neodpovedaj na&#328;.
    </p>
  </td></tr>

</table>
</td></tr>
</table>
</body>
</html>
""";
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String verificationLink(String token) {
        String base = appBaseUrl == null ? "" : appBaseUrl.trim();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String path = "/auth/verify-email?token=" + token;
        return base.isEmpty() ? path : base + path;
    }

    private void handleFailure(String to, String subject, String body, String reason) {
        if (allowLogFallback) {
            log.warn("SMTP send failed, falling back to log. to={}, subject={}, reason={}", to, subject, reason);
            log.info("EMAIL FALLBACK -> to={} subject='{}' body='{}'", to, subject, body);
        }
        throw new EmailDeliveryException(buildUserFriendlyReason(reason));
    }

    private String buildUserFriendlyReason(String reason) {
        String n = reason == null ? "" : reason.toLowerCase();
        if (n.contains("authentication failed") || n.contains("username and password not accepted")
                || n.contains("535-5.7.8") || n.contains("invalid credentials"))
            return "SMTP autentifikácia zlyhala. Skontrolujte MAIL_USERNAME/MAIL_PASSWORD. Pre Gmail použite App Password.";
        if (n.contains("could not connect") || n.contains("connection refused") || n.contains("timeout"))
            return "Nedá sa pripojiť k SMTP serveru. Skontrolujte MAIL_HOST, MAIL_PORT a firewall.";
        if (n.contains("sender address rejected") || n.contains("from address"))
            return "SMTP server odmietol odosielateľa. Nastavte APP_MAIL_FROM alebo MAIL_USERNAME na platnú adresu.";
        return "Nepodarilo sa odoslať email. Dôvod: " + (reason == null ? "Neznáma chyba" : reason);
    }

    private static String escapeHtml(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#x27;");
    }
}
