package com.touhouqing.datasentry.cleaning.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class AesGcmCipher {

	private static final int IV_LENGTH = 12;

	private static final int TAG_LENGTH_BITS = 128;

	private static final SecureRandom RANDOM = new SecureRandom();

	private AesGcmCipher() {
	}

	public static String encryptToBase64(byte[] key, byte[] plaintext) {
		validateKey(key);
		byte[] iv = new byte[IV_LENGTH];
		RANDOM.nextBytes(iv);
		byte[] cipherText = encrypt(key, iv, plaintext);
		byte[] combined = new byte[iv.length + cipherText.length];
		System.arraycopy(iv, 0, combined, 0, iv.length);
		System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
		return Base64.getEncoder().encodeToString(combined);
	}

	public static byte[] decryptFromBase64(byte[] key, String cipherTextBase64) {
		validateKey(key);
		byte[] combined = Base64.getDecoder().decode(cipherTextBase64);
		if (combined.length <= IV_LENGTH) {
			throw new IllegalArgumentException("Invalid ciphertext length");
		}
		byte[] iv = new byte[IV_LENGTH];
		byte[] cipherText = new byte[combined.length - IV_LENGTH];
		System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
		System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);
		return decrypt(key, iv, cipherText);
	}

	private static byte[] encrypt(byte[] key, byte[] iv, byte[] plaintext) {
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
			GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
			return cipher.doFinal(plaintext);
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to encrypt", e);
		}
	}

	private static byte[] decrypt(byte[] key, byte[] iv, byte[] ciphertext) {
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
			GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
			cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
			return cipher.doFinal(ciphertext);
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to decrypt", e);
		}
	}

	private static void validateKey(byte[] key) {
		if (key == null || key.length != 32) {
			throw new IllegalArgumentException("AES-256 key must be 32 bytes");
		}
	}

}
