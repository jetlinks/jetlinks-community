#!/usr/bin/env bash

KEY_STORE=keyStore.jks
KEY_STORE_PWD=endPass
TRUST_STORE=trustStore.jks
TRUST_STORE_PWD=rootPass

# android support - PKCS12
TRUST_STORE_P12=trustStore.p12
CLIENT_KEY_STORE_P12=client.p12
SERVER_KEY_STORE_P12=server.p12

# PEM
TRUST_STORE_PEM=trustStore.pem
CLIENT_KEY_STORE_PEM=client.pem
SERVER_KEY_STORE_PEM=server.pem

VALIDITY=365

create_keys() {
   echo "creating root key and certificate..."
   keytool -genkeypair -alias root -keyalg EC -dname 'C=CN,L=ChongQing,O=JetLinks Pro,OU=Iot Platform,CN=root' \
        -ext BC=ca:true -validity $VALIDITY -keypass $TRUST_STORE_PWD -keystore $TRUST_STORE -storepass $TRUST_STORE_PWD

   echo "creating CA key and certificate..."
   keytool -genkeypair -alias ca -keyalg EC -dname 'C=CN,L=ChongQing,O=JetLinks Pro,OU=JetLinks Iot Platform,CN=ca' \
        -ext BC=ca:true -validity $VALIDITY -keypass $TRUST_STORE_PWD -keystore $TRUST_STORE -storepass $TRUST_STORE_PWD
   keytool -keystore $TRUST_STORE -storepass $TRUST_STORE_PWD -certreq -alias ca | \
      keytool -keystore $TRUST_STORE -storepass $TRUST_STORE_PWD -alias root -gencert -validity $VALIDITY -ext BC=0 -ext KU=keyCertSign,cRLSign -rfc | \
      keytool -alias ca -importcert -keystore $TRUST_STORE -storepass $TRUST_STORE_PWD

   echo "creating server key and certificate..."
   keytool -genkeypair -alias server -keyalg EC -dname 'C=CN,L=ChongQing,O=JetLinks Pro,OU=JetLinks Iot Platform,CN=server' \
        -validity $VALIDITY -keypass $KEY_STORE_PWD -keystore $KEY_STORE -storepass $KEY_STORE_PWD
   keytool -keystore $KEY_STORE -storepass $KEY_STORE_PWD -certreq -alias server | \
      keytool -keystore $TRUST_STORE -storepass $TRUST_STORE_PWD -alias ca -gencert -ext KU=dig,keyEnc,keyAgree -ext EKU=serverAuth -validity $VALIDITY -rfc > server.csr
   keytool -alias server -importcert -keystore $KEY_STORE -storepass $KEY_STORE_PWD -trustcacerts -file server.csr

   echo "creating client key and certificate..."
   keytool -genkeypair -alias client -keyalg EC -dname 'C=CN,L=ChongQing,O=JetLinks Pro,OU=JetLinks Iot Platform,CN=client' \
        -validity $VALIDITY -keypass $KEY_STORE_PWD -keystore $KEY_STORE -storepass $KEY_STORE_PWD
   keytool -keystore $KEY_STORE -storepass $KEY_STORE_PWD -certreq -alias client | \
      keytool -keystore $TRUST_STORE -storepass $TRUST_STORE_PWD -alias ca -gencert -ext KU=dig,keyEnc,keyAgree -ext EKU=clientAuth -validity $VALIDITY -rfc > client.csr
   keytool -alias client -importcert -keystore $KEY_STORE -storepass $KEY_STORE_PWD -trustcacerts -file client.csr

   echo "creating self-signed key and certificate..."
   keytool -genkeypair -alias self -keyalg EC -dname 'C=CN,L=ChongQing,O=JetLinks Pro,OU=JetLinks Iot Platform,CN=self' \
        -ext BC=ca:true -ext KU=keyCertSign -ext KU=dig -validity $VALIDITY -keypass $KEY_STORE_PWD -keystore $KEY_STORE -storepass $KEY_STORE_PWD

   echo "creating certificate with no digitalSignature keyusage..."
   keytool -genkeypair -alias nosigning -keyalg EC -dname 'C=CN,L=ChongQing,O=JetLinks Pro,OU=JetLinks Iot Platform,CN=nosigning' \
        -ext BC=ca:true -ext KU=keyEn -validity $VALIDITY -keypass $KEY_STORE_PWD -keystore $KEY_STORE -storepass $KEY_STORE_PWD
}

export_p12() {
   echo "exporting keys into PKCS#12"
   keytool -v -importkeystore -srckeystore $KEY_STORE -srcstorepass $KEY_STORE_PWD -alias client \
      -destkeystore $CLIENT_KEY_STORE_P12 -deststorepass $KEY_STORE_PWD -deststoretype PKCS12
   keytool -v -importkeystore -srckeystore $KEY_STORE -srcstorepass $KEY_STORE_PWD -alias server \
      -destkeystore $SERVER_KEY_STORE_P12 -deststorepass $KEY_STORE_PWD -deststoretype PKCS12
   keytool -v -importkeystore -srckeystore $TRUST_STORE -srcstorepass $TRUST_STORE_PWD \
      -destkeystore $TRUST_STORE_P12 -deststorepass $TRUST_STORE_PWD -deststoretype PKCS12
}

export_pem() {
   openssl version

   if [ $? -eq 0 ] ; then
      echo "exporting keys into PEM format"
      openssl pkcs12 -in $SERVER_KEY_STORE_P12 -passin pass:$KEY_STORE_PWD -nodes -out $SERVER_KEY_STORE_PEM
      openssl pkcs12 -in $CLIENT_KEY_STORE_P12 -passin pass:$KEY_STORE_PWD -nodes -out $CLIENT_KEY_STORE_PEM
      openssl pkcs12 -in $TRUST_STORE_P12 -passin pass:$TRUST_STORE_PWD -nokeys -out $TRUST_STORE_PEM
      openssl ecparam -genkey -name prime256v1 -noout -out ec_private.pem
      openssl ec -in ec_private.pem -pubout -out ec_public.pem
   fi
}

create_keys
export_p12
export_pem