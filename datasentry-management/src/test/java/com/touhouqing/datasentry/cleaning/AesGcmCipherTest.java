package com.touhouqing.datasentry.cleaning;

import com.touhouqing.datasentry.cleaning.util.AesGcmCipher;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AesGcmCipherTest {

	@Test
	void encryptAndDecryptRoundTrip() {
		byte[] key = new byte[32];
		new SecureRandom().nextBytes(key);
		byte[] plaintext = "hello-datasentry".getBytes(StandardCharsets.UTF_8);
		String ciphertext = AesGcmCipher.encryptToBase64(key, plaintext);
		byte[] decrypted = AesGcmCipher.decryptFromBase64(key, ciphertext);
		assertArrayEquals(plaintext, decrypted);
	}

}
