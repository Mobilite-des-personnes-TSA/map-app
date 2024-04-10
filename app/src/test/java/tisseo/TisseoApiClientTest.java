package tisseo;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.IOException;

public class TisseoApiClientTest {
    // Doc Tisséo : https://data.toulouse-metropole.fr/explore/dataset/api-temps-reel-tisseo/files/72b8051dbf567762260cfa66baf8c333/download/

    @Test
    public void testTisseoAutocomplete() {
        TisseoApiClient client = new TisseoApiClient();
            String response = client.apiAutocompleteXml("8 allée des sci");
            assertNotNull(response);
            System.out.println("response :"); // Print the response for manual verification
            System.out.println(response);
    }

    @Test
    public void testTisseoApiJourney() {
        TisseoApiClient client = new TisseoApiClient();
            String response = client.apiJourney("xml", "basso cambo ",
                    "françois verdier", "walk", "1");
            assertNotNull(response);
            System.out.println("response :"); // Print the response for manual verification
            System.out.println(response);
    }

}