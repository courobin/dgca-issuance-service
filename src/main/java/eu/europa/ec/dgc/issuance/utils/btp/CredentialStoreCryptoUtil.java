package eu.europa.ec.dgc.issuance.utils.btp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSADecrypter;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("btp")
public class CredentialStoreCryptoUtil {

    @Value("${sap.btp.credstore.encryption}")
    private String encryption;

    @Value("${sap.btp.credstore.encrypted}")
    private boolean encryptionEnabled;

    private PrivateKey ownPrivateKey;

    private PublicKey serverPublicKey;

    @PostConstruct
    private void prepare() throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (!encryptionEnabled) {
            return;
        }
        log.info("Encryption: {}", encryption);
        JsonObject convertedObject = new Gson().fromJson(encryption, JsonObject.class);
        log.info("Converted: {}", convertedObject.get("client_private_key"));

        KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder()
            .decode(convertedObject.get("client_private_key").getAsString()));
        this.ownPrivateKey = rsaKeyFactory.generatePrivate(pkcs8EncodedKeySpec);

        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.getDecoder()
            .decode(convertedObject.get("server_public_key").getAsString()));
        this.serverPublicKey = rsaKeyFactory.generatePublic(x509EncodedKeySpec);
    }

    protected void encrypt() {
        throw new NotImplementedException("Encryption is still to be implemented yet.");
    }

    protected String decrypt(String jweResponse) {
        if (!encryptionEnabled) {
            return jweResponse;
        }

        JWEObject jweObject;

        try {
            RSADecrypter rsaDecrypter = new RSADecrypter(ownPrivateKey);
            jweObject = JWEObject.parse(jweResponse);
            jweObject.decrypt(rsaDecrypter);

            Payload payload = jweObject.getPayload();
            return payload.toString();
        } catch (ParseException | JOSEException e) {
            log.error("Failed to parse JWE response: {}.", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
