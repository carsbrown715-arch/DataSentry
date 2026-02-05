package com.touhouqing.datasentry.cleaning.service;

import com.touhouqing.datasentry.cleaning.util.AesGcmCipher;
import com.touhouqing.datasentry.properties.DataSentryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class CleaningBackupEncryptionService {

	private final DataSentryProperties dataSentryProperties;

	public boolean isEncryptionEnabled() {
		return dataSentryProperties.getCleaning().getBackup().isEncrypt();
	}

	public boolean hasValidKey() {
		try {
			return resolveKey() != null;
		}
		catch (Exception e) {
			return false;
		}
	}

	public String encrypt(String plaintext) {
		byte[] key = resolveKey();
		if (key == null) {
			throw new IllegalStateException("Backup master key is missing");
		}
		return AesGcmCipher.encryptToBase64(key, plaintext.getBytes(StandardCharsets.UTF_8));
	}

	public String decrypt(String ciphertextBase64) {
		byte[] key = resolveKey();
		if (key == null) {
			throw new IllegalStateException("Backup master key is missing");
		}
		byte[] plain = AesGcmCipher.decryptFromBase64(key, ciphertextBase64);
		return new String(plain, StandardCharsets.UTF_8);
	}

	public String getProviderName() {
		return dataSentryProperties.getCleaning().getBackup().getProvider();
	}

	public String getKeyVersion() {
		return "v1";
	}

	private byte[] resolveKey() {
		String envName = dataSentryProperties.getCleaning().getBackup().getMasterKeyEnv();
		if (envName == null || envName.isBlank()) {
			return null;
		}
		String value = System.getenv(envName);
		if (value == null || value.isBlank()) {
			return null;
		}
		byte[] decoded = Base64.getDecoder().decode(value);
		return decoded.length == 32 ? decoded : null;
	}

}
