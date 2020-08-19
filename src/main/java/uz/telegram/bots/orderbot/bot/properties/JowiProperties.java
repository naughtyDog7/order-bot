package uz.telegram.bots.orderbot.bot.properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "jowi")
@ConstructorBinding
public class JowiProperties {
    private final String apiUrlV010;
    private final String apiUrlV3;
    private final String apiKey;
    private final String sig;

    public JowiProperties(String apiKey, String apiSecret, String apiUrlV010, String apiUrlV3) {
        this.apiKey = apiKey;
        this.apiUrlV010 = apiUrlV010;
        this.apiUrlV3 = apiUrlV3;
        String hash = DigestUtils.sha256Hex(apiKey + apiSecret);
        sig = hash.substring(0, 10) + hash.substring(hash.length() - 5);
    }

    public String getApiUrlV010() {
        return apiUrlV010;
    }

    public String getApiUrlV3() {
        return apiUrlV3;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getSig() {
        return sig;
    }

    @Override
    public String toString() {
        return "JowiProperties{" +
                "apiUrlV010='" + apiUrlV010 + '\'' +
                ", apiUrlV3='" + apiUrlV3 + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", sig='" + sig + '\'' +
                '}';
    }
}
