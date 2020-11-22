package ru.v6.mark.prototype.service;

import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.Time;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.util.CollectionStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.CryptoPro.CAdES.CAdESSignature;
import ru.CryptoPro.CAdES.CAdESType;
import ru.CryptoPro.CAdES.exception.CAdESException;
import ru.CryptoPro.JCP.JCP;

import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.security.*;
import java.security.cert.*;
import java.util.*;

@Service
public class SignatureService extends BaseService {


    @Value("${config.CER_PASSWORD}")
    private String cer_password;

    public CAdESSignature getSignature(String cer_alias, boolean detached) {
        if (isProxy()) {
            Authenticator.setDefault(new ProxyAuthenticator(System.getProperty("http.proxyUser"), System.getProperty("http.proxyPassword")));
        }

        System.setProperty("com.sun.security.enableCRLDP", "true");
        System.setProperty("com.ibm.security.enableCRLDP", "true");
        String cer_story = "HDImageStore";
        char[] cer_pass = cer_password.toCharArray();

        KeyStore keyStore = null;
        PrivateKey privateKey = null;
        X509Certificate cert = null;
        X509Certificate certRoot = null;
        CAdESSignature signature = null;

        try {
            keyStore = KeyStore.getInstance(cer_story);
            keyStore.load(null, null );
            privateKey = (PrivateKey) keyStore.getKey(cer_alias, cer_pass);
            cert = (X509Certificate) keyStore.getCertificate(cer_alias);
            certRoot = (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(getClass().getClassLoader().getResourceAsStream("root.cer"));

            //new sign
            signature = new CAdESSignature(detached);
        } catch (Exception e) { //KeyStoreException | IOException| ProviderException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException e) {
            e.printStackTrace();
        }

        List<X509Certificate> chain = Arrays.asList(cert, certRoot);
        //add certs in sign
        Collection<X509CertificateHolder> holderList = new ArrayList<>();
        for (X509Certificate cert1 : chain) {
            try {
                holderList.add(new X509CertificateHolder(cert1.getEncoded()));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }
        }
        try {
            signature.setCertificateStore(new CollectionStore(holderList));
        } catch (CAdESException e) {
            e.printStackTrace();
        }
        final Hashtable table = new Hashtable();
        Attribute attr = new Attribute(CMSAttributes.signingTime,
                new DERSet(new Time(new Date()))); // устанавливаем время подписи
        table.put(attr.getAttrType(), attr);
        AttributeTable attrTable = new AttributeTable(table);

        try {
            signature.addSigner(JCP.PROVIDER_NAME,
                    JCP.GOST_DIGEST_2012_256_OID,
                    JCP.GOST_PARAMS_EXC_2012_256_KEY_OID,
                    privateKey,
                    chain,
                    CAdESType.CAdES_BES,
                    null,
                    false,
                    attrTable,
                    null);
        } catch (CAdESException e) {
            e.printStackTrace();
        }

        return signature;

    }

}

class ProxyAuthenticator extends Authenticator {

    private String user, password;

    public ProxyAuthenticator(String user, String password) {
        this.user = user;
        this.password = password;
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, password.toCharArray());
    }
}