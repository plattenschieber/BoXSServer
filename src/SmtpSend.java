package server;

import java.util.Date;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import static util.Log.*;

public class SmtpSend
{

	public static boolean active = false;

	public synchronized static void sendMail(String adress, String subject,
			String message, String file)
	{
		info("sending mail to " + adress);
		java.util.Properties props = new java.util.Properties();
		props.put("mail.smtp.host", "canis.uberspace.de");
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");

		Authenticator auth = new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication()
			{
				return new PasswordAuthentication(
						"boxs@jeronim.de", "boxs");
			}
		};
		Session mailsession = Session.getDefaultInstance(props, auth);

		Message m = new MimeMessage(mailsession);

		try
		{
			InternetAddress from = new InternetAddress("boxs@jeronim.de");
			m.setFrom(from);

			InternetAddress to = new InternetAddress(adress);
			m.setRecipient(Message.RecipientType.TO, to);

			MimeBodyPart mbp1 = new MimeBodyPart();
			mbp1.setText(message);

			Multipart mp = new MimeMultipart();
			mp.addBodyPart(mbp1);

			if (file != null)
			{
				MimeBodyPart mbp2 = new MimeBodyPart();

				// attach the file to the message
				FileDataSource fds = new FileDataSource(file);
				mbp2.setDataHandler(new DataHandler(fds));
				String fname = fds.getName();
				final String nogo = ",()\\\n :;/";
				for (int i = 0; i < nogo.length(); i++)
					fname = fname.replace(nogo.charAt(i), 'x');

				mbp2.setFileName(fname);
				mp.addBodyPart(mbp2);
			}

			// add the Multipart to the message
			m.setContent(mp);

			m.setSentDate(new Date());
			m.setSubject(subject);
			Transport.send(m);
			info("Mail sent");
		} catch (Exception e)
		{
			error(e.getMessage());
			//e.printStackTrace();
		}
	}
}
