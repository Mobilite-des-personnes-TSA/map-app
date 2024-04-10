package tisseo;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import java.io.IOException;

public class TisseoApiClient {

    private static final String ENTRY_POINT = "https://api.tisseo.fr/v2/";
    private static final String API_KEY = "API_KEY";

    private HttpClient httpClient;

    public TisseoApiClient() {
        this.httpClient = HttpClients.createDefault();
    }

    public String executeGetRequest(String service, String format, String parameters) throws IOException {
        String url = buildUrl(service, format, parameters);
        System.out.println("url :");
        System.out.println(url);
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = httpClient.execute(httpGet);

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            return EntityUtils.toString(entity, "UTF-8");
        } else {
            return null;
        }
    }

    private String buildUrl(String service, String format, String parameters) {
        StringBuilder urlBuilder = new StringBuilder(ENTRY_POINT);
        urlBuilder.append(service).append(".").append(format).append("?");
        if (parameters != null && !parameters.isEmpty()) {
            String[] params = parameters.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    try {
                        urlBuilder.append(URLEncoder.encode(keyValue[0], "UTF-8"))
                                .append("=")
                                .append(URLEncoder.encode(keyValue[1], "UTF-8"))
                                .append("&");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        urlBuilder.append("key=").append(API_KEY);
        return urlBuilder.toString();
    }

    public String apiAutocompleteJson(String parameter) {  // return Json with results for autocompletion
        return apiAutocomplete("json", parameter);
    }
    public String apiAutocompleteXml(String parameter) {  // return XML with results for autocompletion
        return apiAutocomplete("xml", parameter);
    }
    private String apiAutocomplete(String format, String parameter) {
        try {
            return this.executeGetRequest("places", format, "term="+parameter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String apiJourney(String format, String departurePlace, String arrivalPlace,
                             String roadMode, String number) {
        try {
            return this.executeGetRequest("journeys", format,
                    "departurePlace="+departurePlace + "&arrivalPlace="+arrivalPlace +
                            "&roadMode="+roadMode + "&number="+number +
                            "&displayWording=1" + "&lang=fr");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
