package rs.mivanovic.pakrunner;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author milos
 * 
 */
public class InputParameters {
	public double thermic_cp;	// Cp
	public double thermic_k;	// K
	public double thermic_ha;	// Ha
	public double thermic_hwc;	// Hwc
	public double thermic_s_cp;	// Cr
	public double thermic_s_k;	// Kr
	public double Tr;			// Temperatura stene na koti 260 [°C],	Tr u Ulaz.csv fajlu
		
	/**
	 * Serijalizacija samo onih vrednosti koje se nalaze u klasi
	 * @return
	 */
	public String serialize() {
		return String.format("thermic_cp=%10.3e, thermic_k=%10.3e, thermic_ha=%10.3e, thermic_hwc=%10.3e, "
				+ "thermic_s_cp=%10.3e, thermic_s_k=%10.3e, Tr=%10.3e", thermic_cp, thermic_k, thermic_ha, thermic_hwc, 
				thermic_s_cp, thermic_s_k, Tr);
	}
	
	/**
	 * Serijalizacija Ulaz.csv fajla iz JSON stringa u odgovarajucem formatu
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 */
	public String UlazCSV(String input) throws JsonProcessingException, IOException {

		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(input);

		JsonNode params = jsonNode.get("Params");

		for (JsonNode node : params) {
			for (JsonNode innerNode : node.get("Values")) {
				
				String name = innerNode.get("Name").asText();
				Double value = innerNode.get("Value").asDouble();
				
				if (name.equals("thermic_cp")) this.thermic_cp = value;
				if (name.equals("thermic_k")) this.thermic_k = value;
				if (name.equals("thermic_ha")) this.thermic_ha = value;
				if (name.equals("thermic_hwc")) this.thermic_hwc = value;
				if (name.equals("thermic_s_cp")) this.thermic_s_cp = value;
				if (name.equals("thermic_s_k")) this.thermic_s_k = value;
			}
		}
		
		// Parametar Tr je odvojen, pa ga citamo posebno
		this.Tr = jsonNode.get("tempS").get("Value").asDouble();
		
		// Zaglavlje CSV fajla
		String zaglavlje = "Cp,K,Ha,Hwc,Rc,a,Tr,Hrc,Cr,Kr,Hrw,Zg,Crz,Krz\n";
		
		return zaglavlje +
				String.format("%10.3e,%10.3e,%10.3e,%10.3e,1.00E+00,4.00E-02,%10.3e,1.01E+06,%10.3e,%10.3e,1.01E+06,3.70E+02,6.10E+02,1.50E+05\n", 
						this.thermic_cp, this.thermic_k, this.thermic_ha, this.thermic_hwc, this.Tr, this.thermic_s_cp, this.thermic_s_k );
	}
}
