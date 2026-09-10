package com.engine.paper.infrastructure.signature;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

/**
 * PAdES and PKCS#7 digital signature provider for PDF documents utilizing BouncyCastle.
 */
public class PdfSigner {

    public record SignatureMetadata(String signerName, String location, String reason) {
        public static final SignatureMetadata DEFAULT = new SignatureMetadata("PaperEngine", "Cloud", "Document Authentication");
    }

    /**
     * Signs a PDF document using an X.509 certificate and private key.
     */
    public byte[] signPdf(byte[] pdfBytes, PrivateKey privateKey, Certificate[] certChain, SignatureMetadata metadata) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(metadata.signerName());
            signature.setLocation(metadata.location());
            signature.setReason(metadata.reason());
            signature.setSignDate(Calendar.getInstance());

            SignatureOptions options = new SignatureOptions();
            options.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);

            SignatureInterface signingInterface = content -> {
                try {
                    byte[] contentBytes = content.readAllBytes();
                    CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
                    ContentSigner sha256Signer = new JcaContentSignerBuilder("SHA256withRSA").build(privateKey);

                    X509Certificate cert = (X509Certificate) certChain[0];
                    gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                            new JcaDigestCalculatorProviderBuilder().build())
                            .build(sha256Signer, cert));

                    for (Certificate c : certChain) {
                        gen.addCertificate(new X509CertificateHolder(c.getEncoded()));
                    }

                    CMSProcessableByteArray msg = new CMSProcessableByteArray(contentBytes);
                    CMSSignedData signedData = gen.generate(msg, false);
                    return signedData.getEncoded();
                } catch (Exception e) {
                    throw new IOException("Failed to calculate CMS signature: " + e.getMessage(), e);
                }
            };

            doc.addSignature(signature, signingInterface, options);
            doc.saveIncremental(out);
            return out.toByteArray();
        }
    }

    /**
     * Protects PDF with AES-128 or AES-256 encryption.
     */
    public byte[] encryptPdf(byte[] pdfBytes, String userPassword, String ownerPassword) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            AccessPermission permissions = new AccessPermission();
            permissions.setCanPrint(true);
            permissions.setCanExtractContent(false);

            StandardProtectionPolicy policy = new StandardProtectionPolicy(ownerPassword, userPassword, permissions);
            policy.setEncryptionKeyLength(128);
            policy.setPreferAES(true);

            doc.protect(policy);
            doc.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Generates a self-signed RSA KeyPair and X.509 certificate for testing or standalone setups.
     */
    public static KeyPairAndCertificate generateSelfSignedKeyPair(String dnName) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();

        long now = System.currentTimeMillis();
        Date startDate = new Date(now - 60_000);
        Date endDate = new Date(now + (365L * 24 * 60 * 60 * 1000));

        BigInteger serial = BigInteger.valueOf(now);
        X500Name dn = new X500Name(dnName != null ? dnName : "CN=PaperEngine, O=Motologa, C=US");

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                dn, serial, startDate, endDate, dn, keyPair.getPublic()
        );

        X509CertificateHolder holder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);

        return new KeyPairAndCertificate(keyPair, cert);
    }

    public record KeyPairAndCertificate(KeyPair keyPair, X509Certificate certificate) {
    }
}
