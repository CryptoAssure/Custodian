certificate password : h4rdc0r_
keytool -importcert -file certificate.cer -keystore Nubot_keystore.jks -alias "Alias"



keytool -importcert -file api.vaultofsatoshi.com.cer -keystore nubot_keystore.jks -alias “vosaug2014”

keytool -importcert -file smtp.cert -keystore nubot_keystore.jks -alias “googlesmtpcertset2014”


keytool -importcert -file bter.cer -keystore nubot_keystore.jks -alias “btersep2014”
keytool -importcert -file bitstam.cer -keystore nubot_keystore.jks -alias “bitstampoct2014”


keytool -importcert -file open.cer -keystore nubot_keystore.jks -alias “openexchangesratesoct2014”

keytool -importcert -file yahoo.cer -keystore nubot_keystore.jks -alias “yahoooct2014”
