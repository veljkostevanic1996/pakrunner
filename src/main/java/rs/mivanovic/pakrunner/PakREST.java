package rs.mivanovic.pakrunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
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
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.core.header.FormDataContentDisposition;

import org.apache.commons.io.FileUtils;

/**
 *
 * @author Milos Ivanovic RESTful API http://localhost:8080/pakrunner/rest/api
 */
@Path("/api")
public class PakREST {

	@Context
	UriInfo uriInfo;

	public static final String KILL_PROCESSES_COMMAND = PakUtil.getResource("KILL_PROCESSES_COMMAND");
	public static final String LOG_FILE = PakUtil.getResource("LOG_FILE");
	public static final String RESULT_ZIP = PakUtil.getResource("RESULT_ZIP");
	public static final String MASTER_DIR = PakUtil.getResource("MASTER_DIR");
	public static final String WORKING_DIR_ROOT = PakUtil.getResource("RESULT_DIR");

	// Promenjeno da ne bude final
	private static String GUID = "1111-1111-22222-33333";
	private static String COMMAND = "./pakv.run";

	private static Process process = null;
	private static long startTime = System.nanoTime();
	private static boolean previousStatus = false;

	/**
	 * 
	 * @param input
	 * @return
	 * @throws JSONException
	 */
	@POST
	@Path("/start")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response startService(String input) throws JSONException {

		JSONObject json = new JSONObject();

		try {

			// Ako je vec aktivan, prvo ubij proces
			stopService(GUID);

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(input);

			// Proveri da li je JSON u pravom formatu i setuj varijable
			if (!jsonNode.has("guid"))
				throw new JSONException("JSON format problem.");
			else
				GUID = jsonNode.get("guid").asText();
			
			// Pokrece se proces samo ako je zadat 'command' u pozivu
			if (jsonNode.has("command"))
				COMMAND = jsonNode.get("command").asText();
			else
				COMMAND = "";

			String workdir = WORKING_DIR_ROOT + File.separator + GUID;
			File workingDirectory = new File(workdir);

			// Obrisi direktorijum ako postoji i iskopiraj sadrzaj mastera
			if (Files.exists(workingDirectory.toPath()))
				throw new IOException();

			// Iskopiraj sadrzaj mastera
			FileUtils.copyDirectory(new File(MASTER_DIR), workingDirectory);

			// Setuj executable dozvole
			PakUtil.directorySetExecutable(workdir);

			File logFile = new File(workdir + File.separator + LOG_FILE);
			Files.deleteIfExists(logFile.toPath());
			
			// Startuj proces samo ako je pozvan sa argumentom 'command'
			if (!COMMAND.isEmpty()) {
				String[] commands = COMMAND.split("\\s+");
				ProcessBuilder pb = new ProcessBuilder(commands);
				pb.directory(workingDirectory);
				pb.redirectOutput(Redirect.appendTo(logFile));	
				process = pb.start();
				startTime = System.nanoTime();
			}

		} catch (JSONException e) {
			json.put("status", false);
			json.put("message", "ERROR: JSON format problem.");
			return Response.status(200).entity(json.toString()).build();

		} catch (IOException e) {
			json.put("status", false);
			json.put("message", "I/O Exception in the service.");
			return Response.status(200).entity(json.toString()).build();
		}

		json.put("status", true);
		if (COMMAND.isEmpty())
			json.put("message", "OK - task " + GUID + " created");
		else
			json.put("message", "OK - task " + GUID + " created and started.");

		return Response.status(200).entity(json.toString()).build();

	}

	/**
	 * 
	 * @param input
	 * @return
	 * @throws JSONException
	 */
	@POST
	@Path("/stop")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response stopService(String input) throws JSONException {

		JSONObject json = new JSONObject();

		try {
			// Ubij glavni proces
			if (process != null) {
				try {
					process.destroy();
					startTime = System.nanoTime();
				} catch (Exception e) {
					json.put("status", false);
					json.put("message", "ERROR: Cannot kill process.");

					return Response.status(200).entity(json.toString()).build();
				}
			}

			// Ubij sve procese koji u nazivu radnog direktorijuma imaju GUID
			String command = KILL_PROCESSES_COMMAND + " " + GUID;
			String[] commands = command.split("\\s+");
			String workdir = WORKING_DIR_ROOT + File.separator + GUID;

			ProcessBuilder pb = new ProcessBuilder(commands);
			pb.directory(new File(workdir));
			pb.redirectOutput(Redirect.appendTo(new File(workdir + File.separator + LOG_FILE)));
			pb.start();

		} catch (IOException e) {
			json.put("status", false);
			json.put("message", "ERROR: I/O problem in the service.");
			return Response.status(200).entity(json.toString()).build();
		}

		json.put("status", true);
		json.put("message", "OK");

		return Response.status(200).entity(json.toString()).build();
	}

	/**
	 * Da li proracun sa datim GUID-om trenutno radi?
	 * @param guidInput
	 * @return
	 * @throws JSONException
	 */
	@GET
	@Path("/isrunning/{guid}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response isRunning(@PathParam("guid") String guidInput) throws JSONException {

		JSONObject json = new JSONObject();
		boolean status = false;

		// Ako je proces ziv i poslati guid odgovara globalnom GUID-u
		if (process != null && process.isAlive() && guidInput.equals(GUID))
			status = true;

		if (previousStatus != status) {
			previousStatus = status;
			startTime = System.nanoTime();
		}

		json.put("status", status);
		json.put("time", TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS));

		return Response.status(200).entity(json.toString()).build();
	}

	/**
	 * Vraca poslednjih 'lines' linija loga. Ako je lines==0, vraca sve. 
	 * @param guid
	 * @param lines
	 * @return
	 */
	@GET
	@Path("/logtail/{guid}/{lines}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String[] getLogTail(@PathParam("guid") String guid, @PathParam("lines") int lines) {

		// Ako je lines==0, salji ceo log
		if (lines == 0)
			lines = Integer.MAX_VALUE;

		String workdir = WORKING_DIR_ROOT + File.separator + guid;

		return PakUtil.tail(new File(workdir + File.separator + LOG_FILE), lines);
	}

	/**
	 * 
	 * @param guidInput
	 * @return
	 * @throws IOException
	 */
	@GET
	@Path("/logdownload/{guid}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadLog(@PathParam("guid") String guidInput) throws IOException {

		String workdir = WORKING_DIR_ROOT + File.separator + guidInput;

		File log = new File(workdir + File.separator + LOG_FILE);

		if (log.isFile()) {
			ResponseBuilder response = Response.ok().entity((Object) log);
			response.header("Content-Disposition", "attachment; filename=" + log.getName());
			return response.build();
		}
		return Response.status(Response.Status.NOT_FOUND).build();
	}

	/**
	 * Vraca ZIP sa trazenim fajlovima
	 * @param input
	 * @return
	 */
	@POST
	@Path("/getresults")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getResults(String input) {

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(input);

			// Iscitaj i setuj GUID
			GUID = jsonNode.get("guid").asText();

			// Iscitaj listu trazenih fajlova iz JSON-a
			String filesJSON = jsonNode.get("files").toString();
			List<String> fileNames = objectMapper.readValue(filesJSON, List.class);

			// Konvertuj listu u niz stringova
			String[] fileNamesArray = fileNames.toArray(new String[0]);
			
			String workdir = WORKING_DIR_ROOT + File.separator + GUID;

			if (fileNamesArray.length != 0 && PakUtil.zipFiles(workdir, fileNamesArray, RESULT_ZIP)) {

				File zipFile = new File(workdir + File.separator + RESULT_ZIP);

				if (zipFile.isFile()) {
					ResponseBuilder response = Response.ok().entity((Object) zipFile);
					response.header("Content-Disposition", "attachment; filename=" + zipFile.getName());
					
					return response.build();
				}
			}

		} catch (IOException e) {
			return Response.status(200).entity(false).build();
		}

		return Response.status(Response.Status.NOT_FOUND).build();
	}


	/**
	 * Brisanje posla sa poslatim GUID-om
	 * @param guidInput
	 * @return
	 * @throws JSONException
	 */
	@GET
	@Path("/remove/{guid}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response remove(@PathParam("guid") String guidInput) throws JSONException {
	
		JSONObject json = new JSONObject();
		
		// Ako je nalog za brisanje poslat tekucem GUID-u, prvo stopiraj proracun
		if (guidInput.equals(GUID))
			stopService(GUID);
	
		File dirToRemove = new File(WORKING_DIR_ROOT + File.separator + guidInput);

		try {
			if (Files.exists(dirToRemove.toPath())) {
				FileUtils.deleteDirectory(dirToRemove);
				json.put("status", true);
				json.put("message", "Directory " + guidInput + " removed.");
			} else {
				json.put("status", true);
				json.put("message", "Directory " + guidInput + " does not exist.");				
			}
				
		} catch(IOException e) {
			json.put("status", false);
			json.put("message", "Directory " + guidInput + " not removed.");
		}
	
		return Response.status(200).entity(json.toString()).build();
	}

	
	/**
	 * Brise sve poslove u WORKING_DIR_ROOT
	 * @param 
	 * @return
	 * @throws JSONException
	 */
	@GET
	@Path("/removeall/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response removeAll() throws JSONException {
	
		JSONObject json = new JSONObject();
		
		// Ako je nalog za brisanje svih poslat, prvo stopiraj proracun
		stopService(GUID);
	
		try {
			FileUtils.cleanDirectory(new File(WORKING_DIR_ROOT));
			json.put("status", true);
			json.put("message", "Directory " + WORKING_DIR_ROOT + " cleaned.");				
			
		} catch(IOException e) {
			json.put("status", false);
			json.put("message", "Directory " + WORKING_DIR_ROOT + " not cleaned.");
		}
	
		return Response.status(200).entity(json.toString()).build();
	}
	

	/**
	 * 
	 * @param enabled
	 * @param uploadedInputStream
	 * @param fileDetail
	 * @return
	 */
	@POST
	@Path("/uploadzip")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadZIP(@FormDataParam("file") InputStream uploadedInputStream, 
			@FormDataParam("guid") String guidInput) throws JSONException {
	    
		JSONObject json = new JSONObject();
		File uploadedZIP = new File(WORKING_DIR_ROOT + File.separator + guidInput + File.separator + "tmp.zip");
	    		
		try {
			
		    // Obrisi ako vec postoji
			if(uploadedZIP.exists())
		        uploadedZIP.delete();
			
			// Snimi fajl iz stream-a
			PakUtil.saveToFile(uploadedInputStream, uploadedZIP.getPath());

			// Raspakuj fajl
			PakUtil.unzip(uploadedZIP.getPath(), WORKING_DIR_ROOT + File.separator + guidInput);
			
		    // Obrisi privremeni zip na kraju posla
			if(uploadedZIP.exists())
		        uploadedZIP.delete();
			
			json.put("status", true);
			json.put("message", "File " + uploadedZIP.getPath() + " extracted into " + guidInput);				
			
		} catch(IOException e) {
			json.put("status", false);
			json.put("message", "ERROR: I/O problem.");
		}
	
		return Response.status(200).entity(json.toString()).build();
	}
	
	/**
	 * Kopiranje fajla iz podfoldera u radni folder za dati task GUID
	 * @param input
	 * @return
	 * @throws JSONException 
	 */
	@POST
	@Path("/localcopy")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response localCopy(String input) throws JSONException {

		JSONObject json = new JSONObject();
		String guidInput, sourceRelativePath, destName;

		try {

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(input);

			// Proveri da li je JSON u pravom formatu i setuj varijable
			if (!jsonNode.has("guid") || !jsonNode.has("path") || !jsonNode.has("name") )
				throw new JSONException("JSON format problem.");
			else {
				guidInput = jsonNode.get("guid").asText();
				sourceRelativePath = jsonNode.get("path").asText();
				destName = jsonNode.get("name").asText();
			}

			String workdir = WORKING_DIR_ROOT + File.separator + guidInput;
			File destFile = new File (workdir + File.separator + destName);
			
		    // Obrisi ako vec postoji
			if (destFile.exists())
		        destFile.delete();
			
			// Kopiranje samog fajla
			FileUtils.copyFile( new File(workdir + File.separator + sourceRelativePath), destFile );
		
		} catch (JSONException e) {
			json.put("status", false);
			json.put("message", "ERROR: JSON format problem.");
			return Response.status(200).entity(json.toString()).build();

		} catch (IOException e) {
			json.put("status", false);
			json.put("message", "I/O Exception in the service.");
			return Response.status(200).entity(json.toString()).build();
		}

		json.put("status", true);
		json.put("message", "OK");

		return Response.status(200).entity(json.toString()).build();
	}

	
	/**
	 * Kopiranje fajla iz radnog direktorijuma guidsrc u direktorijum guiddest
	 * @param input
	 * @return
	 * @throws JSONException
	 */
	@POST
	@Path("/copyfiletasktotask")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response copyTaskToTask(String input) throws JSONException {

		JSONObject json = new JSONObject();
		String guidSrc;
		String guidDest; 
		String nameSrc; 
		String nameDest;

		try {

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(input);

			// Proveri da li je JSON u pravom formatu i setuj varijable
			if (!jsonNode.has("guidsrc") || !jsonNode.has("guiddest") || !jsonNode.has("namesrc") || !jsonNode.has("namedest") )
				throw new JSONException("JSON format problem.");
			else {
				guidSrc = jsonNode.get("guidsrc").asText();
				guidDest = jsonNode.get("guiddest").asText();
				nameSrc = jsonNode.get("namesrc").asText();
				nameDest = jsonNode.get("namedest").asText();
			}

			File srcFile = new File (WORKING_DIR_ROOT + File.separator + guidSrc + File.separator + nameSrc);
			File destFile = new File (WORKING_DIR_ROOT + File.separator + guidDest + File.separator + nameDest);
			
		    // Obrisi ako vec postoji
			if (destFile.exists())
		        destFile.delete();
			
			// Kopiranje samog fajla
			FileUtils.copyFile( srcFile, destFile );
		
		} catch (JSONException e) {
			json.put("status", false);
			json.put("message", "ERROR: JSON format problem.");
			return Response.status(200).entity(json.toString()).build();

		} catch (IOException e) {
			json.put("status", false);
			json.put("message", "I/O Exception in the service.");
			return Response.status(200).entity(json.toString()).build();
		}

		json.put("status", true);
		json.put("message", "OK");

		return Response.status(200).entity(json.toString()).build();
	}
	
	
	/**
	 * Brisanje fajla iz radnog direktorijuma taska (radi i za relativne putanje)
	 * @param input
	 * @return
	 * @throws JSONException
	 */
	@POST
	@Path("/removefile")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response removeFile(String input) throws JSONException {

		JSONObject json = new JSONObject();
		String guidInput, fileRelativePath;

		try {

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(input);

			// Proveri da li je JSON u pravom formatu i setuj varijable
			if (!jsonNode.has("guid") || !jsonNode.has("path") )
				throw new JSONException("JSON format problem.");
			else {
				guidInput = jsonNode.get("guid").asText();
				fileRelativePath = jsonNode.get("path").asText();
			}

			String workdir = WORKING_DIR_ROOT + File.separator + guidInput;
			File destFile = new File (workdir + File.separator + fileRelativePath);
			
		    // Obrisi fajl ako postoji
			if (destFile.exists())
		        destFile.delete();
			else {
				json.put("status", true);
				json.put("message", "File " + destFile.getPath() + " does not exist.");
				return Response.status(200).entity(json.toString()).build();				
			}
		
		} catch (JSONException e) {
			json.put("status", false);
			json.put("message", "ERROR: JSON format problem.");
			return Response.status(200).entity(json.toString()).build();

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
	@Path("/renamefile")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response renameFile(String input) throws JSONException {

		JSONObject json = new JSONObject();
		String guidInput;
		String pathOld; 
		String pathNew; 

		try {

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(input);

			// Proveri da li je JSON u pravom formatu i setuj varijable
			if (!jsonNode.has("guid") || !jsonNode.has("pathold") || !jsonNode.has("pathnew") )
				throw new JSONException("JSON format problem.");
			else {
				guidInput = jsonNode.get("guid").asText();
				pathOld = jsonNode.get("pathold").asText();
				pathNew = jsonNode.get("pathnew").asText();
			}

			File oldFile = new File (WORKING_DIR_ROOT + File.separator + guidInput + File.separator + pathOld);
			File newFile = new File (WORKING_DIR_ROOT + File.separator + guidInput + File.separator + pathNew);
			
		    // Obrisi ako vec postoji
			if (oldFile.exists())
		        oldFile.renameTo(newFile);
			else {
				json.put("status", false);
				json.put("message", "ERROR: File " + oldFile.getPath() + " does not exist.");
				return Response.status(200).entity(json.toString()).build();						
			}
			
		} catch (JSONException e) {
			json.put("status", false);
			json.put("message", "ERROR: JSON format problem.");
			return Response.status(200).entity(json.toString()).build();

		} catch (IOException e) {
			json.put("status", false);
			json.put("message", "I/O Exception in the service.");
			return Response.status(200).entity(json.toString()).build();
		}

		json.put("status", true);
		json.put("message", "OK");

		return Response.status(200).entity(json.toString()).build();
	}
	
	/**
	 * Vraca listu fajlova iz radnog direktorijuma guid + path
	 * @param input
	 * @return
	 * @throws JSONException
	 */
	@POST
	@Path("/listfiles/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response listfiles(String input) throws JSONException {
		
		JSONObject json = new JSONObject();
		String guidInput;
		String relativePath;

		try {

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(input);

			// Proveri da li je JSON u pravom formatu i setuj varijable
			if (!jsonNode.has("guid") || !jsonNode.has("path") )
				throw new JSONException("JSON format problem.");
			else {
				guidInput = jsonNode.get("guid").asText();
				relativePath = jsonNode.get("path").asText();
			}

			String dirToList = WORKING_DIR_ROOT + File.separator + guidInput + File.separator + relativePath;

			// Pronadji sve fajlove i direktorijume na zadatoj putanji
			List<java.nio.file.Path> listOfPaths = Files
				.find(Paths.get(dirToList), 1, (path, attr) -> true )
				.collect(Collectors.toList());
		
			ArrayList<String> fileList = new ArrayList<>();
			ArrayList<String> dirList = new ArrayList<>();
		
			// Stavi fajlove i direktorijume u posebne liste
			for (java.nio.file.Path p : listOfPaths) {
				if (p.toFile().isFile())
					fileList.add(p.getFileName().toString());
				else if (p.toFile().isDirectory())
					dirList.add(p.getFileName().toString());
			}
			
			json.put("status", true);
			json.put("message", "OK");
			json.put("task", guidInput);
			json.put("path", relativePath);		
			json.put("directories", dirList);
			json.put("files", fileList);
			
			return Response.status(200).entity(json.toString()).build();
			
		} catch (JSONException e) {
			json.put("status", false);
			json.put("message", "ERROR: JSON format problem.");
			return Response.status(200).entity(json.toString()).build();

		} catch (IOException e) {
			json.put("status", false);
			json.put("message", "I/O Exception in the service.");
			return Response.status(200).entity(json.toString()).build();
		}
	}

	
	/**
	 * Vraca listu taskova
	 * @param input
	 * @return
	 * @throws JSONException
	 */
	@GET
	@Path("/tasklist/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response taskslist() throws JSONException {
		
		JSONObject json = new JSONObject();

		try {

			// Pronadji sve fajlove i direktorijume na zadatoj putanji
			List<java.nio.file.Path> listOfPaths = Files
				.find(Paths.get(WORKING_DIR_ROOT), 1, (path, attr) -> path.toFile().isDirectory() )
				.collect(Collectors.toList());
		
			ArrayList<String> dirList = new ArrayList<>();
		
			// Stavi fajlove i direktorijume u posebne liste
			for (java.nio.file.Path p : listOfPaths)
					dirList.add(p.getFileName().toString());
			
			// Ukloni roditeljski direktorijum koji stavlja na prvo mesto
			if (dirList.size()>1)
				dirList.remove(0);
			
			json.put("status", true);
			json.put("message", "OK");
			json.put("tasks", dirList);
			
			return Response.status(200).entity(json.toString()).build();

		} catch (IOException e) {
			json.put("status", false);
			json.put("message", "I/O Exception in the service.");
			return Response.status(200).entity(json.toString()).build();
		}
	}
	
	
}
