package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;


import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.OTP;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.OtpRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class EmailServiceVerificationImplement {
    @Autowired private JavaMailSender mailSender;
    @Autowired private OtpRepository otpRepository;
    @Autowired private AccountRepository accountRepository;

    @Transactional
    public void sendVerificationEmail(Account account) throws MessagingException {
        OTP existingOtp = account.getOtp();

        if (existingOtp != null) {
//            account.setOtp(null);
//            accountRepository.save(account);
//            otpRepository.deleteById(existingOtp.getOtpId());
            String otpCode = String.valueOf(100000 + new Random().nextInt(900000));
            existingOtp.setCode(otpCode);
            existingOtp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
            otpRepository.save(existingOtp);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(account.getEmail());
            helper.setSubject("Mã OTP của bạn");
            helper.setText("Mã OTP của bạn là: " + otpCode, true);
            mailSender.send(message);

        }else{
            String otpCode = String.valueOf(100000 + new Random().nextInt(900000));

            OTP otp = new OTP();
            otp.setAccount(account);
            otp.setCode(otpCode);
            otp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
            otpRepository.save(otp);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(account.getEmail());
            helper.setSubject("Mã OTP của bạn");
            helper.setText("Mã OTP của bạn là: " + otpCode, true);
            mailSender.send(message);
        }
    }

}
