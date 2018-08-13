package rs.mivanovic.pakrunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Milos Ivanovic RESTfull API http://localhost:8080/pakrest/rest/
 */
@Path("/api")
public class PakREST {

	@Context
	UriInfo uriInfo;
	
	public static final String ROOT_DIR = PakUtil.getResource("ROOT_DIR");  //"/home/milos/Desktop/Trebisnjica/pakrunner/" + GUID;	
	//public static final String GUID = PakUtil.getResource("GUID");     //"d7f88f3e-3754-4132-8c7a-1b41722afd40";
	//public static final String COMMAND = PakUtil.getResource("COMMAND");  //"./pakv.run";
	public static final String KILL_PROCESSES_COMMAND = PakUtil.getResource("KILL_PROCESSES_COMMAND");  //"./kill_processes.sh";
	public static final String LOG_FILE = PakUtil.getResource("LOG_FILE");  //"pak.log";
	public static final String RESULT_ZIP = PakUtil.getResource("RESULT_ZIP");  //"rezultati.zip";	
	public static final String RESULT_FILES = PakUtil.getResource("RESULT_FILES");   //{ "pakv","pakv.run","PIJEZT.DAT" };

	private static Process process = null;
	private static long startTime = System.nanoTime();
	private static boolean previousStatus = false;
	
	public static final String MASTER_DIR = PakUtil.getResource("MASTER_DIR");
	public static final String RESULT_DIR = PakUtil.getResource("RESULT_DIR");
	// Promenjeno da ne bude final
	private static String GUID = "1111-1111-22222-33333";
	private static String COMMAND = "./pakv.run";
	
	
	
	@GET
	@Path("/proba")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response start() throws JSONException, IOException {
		JSONObject json = new JSONObject();
		String direktorijum = PakUtil.getResource("ROOT_DIR");
		json.put("ROOT_DIR", direktorijum);
		
		List<java.nio.file.Path> lista = Files.find(Paths.get(ROOT_DIR), 1, 
        		(path, attr) -> String.valueOf(path).endsWith(".tab") ).collect(Collectors.toList());
				
		int i = 0;
		for (java.nio.file.Path stavka : lista)
			json.put (new Integer(i++).toString(),  new File(stavka.toString()).getName());
			
					
		return Response.status(200).entity(json.toString()).build();
	}

	@POST
	@Path("/start")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response startService(String input) throws JSONException {
		
	    try {
	    	
		    if ( !input.isEmpty() ) {
		    	ObjectMapper objectMapper = new ObjectMapper();
		    	JsonNode jsonNode = objectMapper.readTree(input);

		    	GUID = jsonNode.get("guid").asText();
		    	COMMAND = jsonNode.get("command").asText();
		    }		    
		} catch (IOException e) {
			return Response.status(200).entity(false).build();
		}
	    
		String[] commands = COMMAND.split("\\s+");
				
		// Ako je vec aktivan, prvo ga ubij
		stopService();
		
		try {
			ProcessBuilder pb = new ProcessBuilder(commands);
			String workdir =  RESULT_DIR + File.separator + GUID;
			File f = new File(workdir);
			f.mkdirs();
			pb.directory(f);
			
			
			Files.deleteIfExists((new File(workdir + File.separator + LOG_FILE)).toPath());
			pb.redirectOutput(Redirect.appendTo(new File(workdir + File.separator + LOG_FILE)));
			
			process = pb.start();
			startTime = System.nanoTime();
		} catch (Exception e) {
			return Response.status(200).entity(false).build();
		}

		return Response.status(200).entity(true).build();
	}
	
	@POST
	@Path("/stop")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response stopService() throws JSONException {

		// Ubij glavni proces
		if (process != null) {
			try {
				process.destroy();
				startTime = System.nanoTime();
			} catch (Exception e) {
				return Response.status(200).entity(false).build();
			}
		}
		
        // Ubij sve procese koji u nazivu radnog direktorijuma imaju GUID
		String command = KILL_PROCESSES_COMMAND + " " + GUID;
        String[] commands = command.split("\\s+");
        
        String workdir =  RESULT_DIR + File.separator + GUID;

        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
            File f = new File(workdir);
            pb.directory(f);
            pb.redirectOutput(Redirect.appendTo(new File(workdir + File.separator + LOG_FILE)));
            pb.start();

        } catch (Exception e) {
        }
		
		return Response.status(200).entity(true).build();
	}
	
	@GET
	@Path("/isrunning")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response isRunning() throws JSONException {
		
		JSONObject json = new JSONObject();
		boolean status = false;

		if (process != null && process.isAlive()) 
			status = true;

		if (previousStatus != status) {
			previousStatus = status;
			startTime = System.nanoTime();
		}

		json.put("status", status);
		json.put("time", TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS));

		return Response.status(200).entity(json.toString()).build();
	}
	
	@GET
	@Path("/logtail/{lines}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String[] getLogTail(@PathParam("lines") int lines) {
		String workdir =  RESULT_DIR + File.separator + GUID;
		return PakUtil.tail(new File(workdir + File.separator + LOG_FILE), lines);
	}
	
	@GET
	@Path("/logdownload")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadLog() throws IOException {
		
		String workdir =  RESULT_DIR + File.separator + GUID;

		File log = new File(workdir + File.separator + LOG_FILE);

		if (log.isFile()) {
			ResponseBuilder response = Response.ok().entity((Object) log);
			response.header("Content-Disposition", "attachment; filename=" + log.getName());
			return response.build();
		}
		return Response.status(Response.Status.NOT_FOUND).build();
	}
	
	@GET
	@Path("/getresults")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getResults() throws IOException {
		
		String workdir =  RESULT_DIR + File.separator + GUID;

		List<java.nio.file.Path> lista = Files.find(Paths.get(workdir), 1, 
        		(path, attr) -> String.valueOf(path).endsWith(".tab") ).collect(Collectors.toList());
				
		String[] fajlovi = new String[lista.size()];
		
		int i = 0;
		for (java.nio.file.Path stavka : lista)
			fajlovi[i++] = new File(stavka.toString()).getName();
						
		if ( i!=0 && PakUtil.zipFiles(workdir, fajlovi, RESULT_ZIP) ) {
			File arhiva = new File(workdir + File.separator + RESULT_ZIP);

			if (arhiva.isFile()) {
				ResponseBuilder response = Response.ok().entity((Object) arhiva);
				response.header("Content-Disposition", "attachment; filename=" + arhiva.getName());
				return response.build();
			}
		}
		
		return Response.status(Response.Status.NOT_FOUND).build();
	}	
}
