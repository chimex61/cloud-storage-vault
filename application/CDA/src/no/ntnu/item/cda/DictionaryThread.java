package no.ntnu.item.cda;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import de.rtner.security.auth.spi.PBKDF2Engine;
import de.rtner.security.auth.spi.PBKDF2Parameters;

public class DictionaryThread implements Runnable {

	public Thread thread;
	private String[] passwords;
	private SecretKey t_key;
	private byte[] t_plain;
	private byte[] t_salt;
	private byte[] t_cipher;

	public DictionaryThread() {

	}

	public DictionaryThread(String id, String[] words) {
		thread = new Thread(this, id);
		passwords = words;
		t_salt = CDAMapper.salt;
		t_cipher = CDAMapper.cipher;
		thread.start();
	}

	@Override
	public void run() {
		for (String password : passwords) {
			if (check(password)) {
				System.out.println("FOUND PASSWORD: "
						+ System.currentTimeMillis());
				CDAMapper.password = password;
			}
		}
	}

	public boolean check(String word) {
		this.setKey(word);
		this.t_plain = symECBDecrypt(t_cipher, t_key);

		// 68 = char D and indicate correct decryption of first byte
		if (this.t_plain != null && this.t_plain[0] == 68) {
			String[] parts = new String(t_plain).split(":");
			String[] parts2 = new String(t_plain).split("|");
			if (parts.length == 4 && parts2.length == 5) {
				return true;
			}
		}
		return false;
	}

	public void setKey(String password) {
		PBKDF2Parameters param = new PBKDF2Parameters("HmacSHA256", "UTF-8",
				t_salt, 4096);
		PBKDF2Engine engine = new PBKDF2Engine(param);
		param.setDerivedKey(engine.deriveKey(password, 16));
		t_key = new SecretKeySpec(param.getDerivedKey(), "AES");
	}

	public byte[] symECBDecrypt(byte[] cipherText, SecretKey key) {
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "BC");
			cipher.init(Cipher.DECRYPT_MODE, key);
			return cipher.doFinal(cipherText);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			if (e.getMessage().equals("Illegal key size or default parameter")) {
				System.out
						.println("\nERROR: Have you installed the Java(TM) Cryptography Extension (JCE) Jurisdiction Policy Files?");
				e.printStackTrace();
			}
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
		return null;
	}

}
