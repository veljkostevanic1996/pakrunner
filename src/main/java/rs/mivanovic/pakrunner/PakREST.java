package rs.mivanovic.pakrunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

import org.apache.commons.io.FileUtils;

/**
 *
 * @author Milos Ivanovic RESTful API http://localhost:8080/pakrunner/rest/api
 */
@Path("/api")
public class PakREST {

	@Context
	UriInfo uriInfo;
	
	public static final String ROOT_DIR = PakUtil.getResource("ROOT_DIR");
	public static final String KILL_PROCESSES_COMMAND = PakUtil.getResource("KILL_PROCESSES_COMMAND");
	public static final String LOG_FILE = PakUtil.getResource("LOG_FILE");
	public static final String RESULT_ZIP = PakUtil.getResource("RESULT_ZIP");	
	public static final String RESULT_FILES = PakUtil.getResource("RESULT_FILES");
	
	public static final String MASTER_DIR = PakUtil.getResource("MASTER_DIR");
	public static final String WORKING_DIR_ROOT = PakUtil.getResource("RESULT_DIR");

	// Promenjeno da ne bude final
	private static String GUID = "1111-1111-22222-33333";
	private static String COMMAND = "./pakv.run";	

	private static Process process = null;
	private static long startTime = System.nanoTime();
	private static boolean previousStatus = false;
		
	
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
		
		JSONObject json = new JSONObject();
		
		// Ako je vec aktivan, prvo ubij proces
		stopService(GUID);
		
	    try {
		    if ( !input.isEmpty() ) {
		    	ObjectMapper objectMapper = new ObjectMapper();
		    	JsonNode jsonNode = objectMapper.readTree(input);

		    	// Setuj globalni GUID na poslati
		    	GUID = jsonNode.get("guid").asText();
		    	// Setuj globalnu komandu na poslatu
		    	COMMAND = jsonNode.get("command").asText();
		    }		    
		} catch (IOException e) {
			json.put("status", false);
			json.put("message", "I/O Exception in the service.");
			return Response.status(200).entity(json.toString()).build();
		}
	    
		String[] commands = COMMAND.split("\\s+");
		
		try {
			ProcessBuilder pb = new ProcessBuilder(commands);
			String workdir =  WORKING_DIR_ROOT + File.separator + GUID;
			File workingDirectory = new File(workdir);
			pb.directory(workingDirectory);
			
			// Obrisi dir ako postoji i iskopiraj sadrzaj mastera
			FileUtils.deleteDirectory(workingDirectory);
			FileUtils.copyDirectory(new File(MASTER_DIR), workingDirectory);
			
			// Pokupi sve regularne fajlove iz direktorijuma
			List<File> filesInFolder = Files.walk(Paths.get(workdir))
                    .filter(Files::isRegularFile)
                    .map(java.nio.file.Path::toFile)
                    .collect(Collectors.toList());			
			
			// Setuj executable dozvole
			for (File lf : filesInFolder)
				lf.setExecutable(true);
							
			Files.deleteIfExists((new File(workdir + File.separator + LOG_FILE)).toPath());
			pb.redirectOutput(Redirect.appendTo(new File(workdir + File.separator + LOG_FILE)));
			
			process = pb.start();
			startTime = System.nanoTime();
		} catch (IOException e) {
			json.put("status", false);
			json.put("message", "I/O Exception in the service.");
			return Response.status(200).entity(json.toString()).build();
		}
		
		json.put("status", true);
		json.put("message", "OK");

		return Response.status(200).entity(json.toString()).build();
	}
	
	@POST
	@Path("/stop")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response stopService(String input) throws JSONException {
		
		JSONObject json = new JSONObject();
			    
		// Ubij glavni proces
		if (process != null) {
			try {
				process.destroy();
				startTime = System.nanoTime();
			} catch (Exception e) {
	    		json.put("status", false);
	    		json.put("message", "Internal server error");        	        	

				return Response.status(200).entity(json.toString()).build();
			}
		}
		
        // Ubij sve procese koji u nazivu radnog direktorijuma imaju GUID
		String command = KILL_PROCESSES_COMMAND + " " + GUID;
		System.out.println(command);
        String[] commands = command.split("\\s+");
        String workdir =  WORKING_DIR_ROOT + File.separator + GUID;

        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
            File f = new File(workdir);
            pb.directory(f);
            pb.redirectOutput(Redirect.appendTo(new File(workdir + File.separator + LOG_FILE)));
            pb.start();
	
        } catch (Exception e) {
    		json.put("status", false);
    		json.put("message", "Internal server error.1");
    		return Response.status(200).entity(json.toString()).build();
        }
        
		json.put("status", true);
		json.put("message", "OK");
		
		return Response.status(200).entity(json.toString()).build();
	}
	
	
	
	@GET
	@Path("/isrunning/{guid}")	
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response isRunning(@PathParam("guid") String guidInput) throws JSONException {
		
		JSONObject json = new JSONObject();
		boolean status = false;
		
		// Ako je proces ziv i poslati guid odgovara globalnom GUID-u
	    if ( process != null && process.isAlive() && guidInput.equals(GUID) ) 
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
	@Path("/logtail/{guid}/{lines}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String[] getLogTail(@PathParam("guid") String guid, @PathParam("lines") int lines) {
		
		// Ako je lines==0, salji ceo log
		if (lines==0) 
			lines = Integer.MAX_VALUE;
		
		String workdir =  WORKING_DIR_ROOT + File.separator + guid;
		
		return PakUtil.tail(new File(workdir + File.separator + LOG_FILE), lines);
	}
	
	@GET
	@Path("/logdownload/{guid}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadLog(@PathParam("guid") String guidInput) throws IOException {
		
		String workdir =  WORKING_DIR_ROOT + File.separator + guidInput;

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
		
		String workdir =  WORKING_DIR_ROOT + File.separator + GUID;

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
