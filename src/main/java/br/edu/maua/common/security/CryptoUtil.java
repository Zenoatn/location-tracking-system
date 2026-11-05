package br.edu.maua.common.security;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** Cryptographic helpers for password verification and encrypted text exports. */
public final class CryptoUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int HASH_ITERATIONS = 210_000;
    private static final int HASH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final int GCM_NONCE_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final byte[] EXPORT_MAGIC = "LTS1".getBytes(StandardCharsets.US_ASCII);

    private CryptoUtil() {
    }

    public static String hashPassword(char[] password) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = deriveKey(password, salt, HASH_ITERATIONS, HASH_BITS);
        return "pbkdf2-sha256$" + HASH_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verifyPassword(char[] password, String storedHash)
            throws GeneralSecurityException {
        String[] parts = storedHash.split("\\$", -1);
        if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) {
            return false;
        }
        int iterations;
        byte[] salt;
        byte[] expected;
        try {
            iterations = Integer.parseInt(parts[1]);
            salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (iterations < 100_000 || iterations > 2_000_000
                || salt.length < 16 || expected.length < 16) {
            return false;
        }
        byte[] actual = deriveKey(password, salt, iterations, expected.length * 8);
        return MessageDigest.isEqual(expected, actual);
    }

    public static byte[] encrypt(String plaintext, char[] passphrase) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_BYTES];
        byte[] nonce = new byte[GCM_NONCE_BYTES];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(nonce);
        byte[] key = deriveKey(passphrase, salt, HASH_ITERATIONS, HASH_BITS);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, nonce));
            cipher.updateAAD(EXPORT_MAGIC);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.writeBytes(EXPORT_MAGIC);
            output.writeBytes(salt);
            output.writeBytes(nonce);
            output.writeBytes(ciphertext);
            return output.toByteArray();
        } finally {
            java.util.Arrays.fill(key, (byte) 0);
        }
    }

    public static String decrypt(byte[] encrypted, char[] passphrase) throws GeneralSecurityException {
        int headerLength = EXPORT_MAGIC.length + SALT_BYTES + GCM_NONCE_BYTES;
        if (encrypted.length <= headerLength + 16
                || !MessageDigest.isEqual(EXPORT_MAGIC,
                        java.util.Arrays.copyOfRange(encrypted, 0, EXPORT_MAGIC.length))) {
            throw new GeneralSecurityException("Formato de arquivo criptografado inválido.");
        }
        int saltStart = EXPORT_MAGIC.length;
        byte[] salt = java.util.Arrays.copyOfRange(encrypted, saltStart, saltStart + SALT_BYTES);
        int nonceStart = saltStart + SALT_BYTES;
        byte[] nonce = java.util.Arrays.copyOfRange(encrypted, nonceStart, nonceStart + GCM_NONCE_BYTES);
        byte[] ciphertext = java.util.Arrays.copyOfRange(encrypted, headerLength, encrypted.length);
        byte[] key = deriveKey(passphrase, salt, HASH_ITERATIONS, HASH_BITS);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, nonce));
            cipher.updateAAD(EXPORT_MAGIC);
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (AEADBadTagException exception) {
            throw new GeneralSecurityException("Senha incorreta ou arquivo alterado.", exception);
        } finally {
            java.util.Arrays.fill(key, (byte) 0);
        }
    }

    private static byte[] deriveKey(char[] password, byte[] salt, int iterations, int bits)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bits);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}
