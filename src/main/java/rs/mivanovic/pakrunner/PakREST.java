package rs.mivanovic.pakrunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

/**
 *
 * @author Milos Ivanovic RESTfull API http://localhost:8080/pakrest/rest/
 */
@Path("/api")
public class PakREST {

	@Context
	UriInfo uriInfo;

	public static final String GUID = PakUtil.getResource("GUID");     //"d7f88f3e-3754-4132-8c7a-1b41722afd40";
	public static final String ROOT_DIR = PakUtil.getResource("ROOT_DIR");  //"/home/milos/Desktop/Trebisnjica/pakrunner/" + GUID;
	public static final String COMMAND = PakUtil.getResource("COMMAND");  //"./pakv.run";
	public static final String KILL_PROCESSES_COMMAND = PakUtil.getResource("KILL_PROCESSES_COMMAND");  //"./kill_processes.sh";
	public static final String LOG_FILE = PakUtil.getResource("LOG_FILE");  //"pak.log";
	public static final String RESULT_ZIP = PakUtil.getResource("RESULT_ZIP");  //"rezultati.zip";	
	public static final String RESULT_FILES = PakUtil.getResource("RESULT_FILES");   //{ "pakv","pakv.run","PIJEZT.DAT" };

	private static Process process = null;
	private static long startTime = System.nanoTime();
	private static boolean previousStatus = false;
	
	@GET
	@Path("/proba")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response start() throws JSONException {
		JSONObject json = new JSONObject();
		String direktorijum = PakUtil.getResource("ROOT_DIR");
		json.put("ROOT_DIR", direktorijum);

		return Response.status(200).entity(json.toString()).build();
	}

	@GET
	@Path("/start")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response startService() throws JSONException {

		String[] commands = COMMAND.split("\\s+");
		
		// Ako je vec aktivan, prvo ga ubij
		stopService();
		
		try {
			ProcessBuilder pb = new ProcessBuilder(commands);
			File f = new File(ROOT_DIR);
			pb.directory(f);
			
			Files.deleteIfExists((new File(ROOT_DIR + File.separator + LOG_FILE)).toPath());
			pb.redirectOutput(Redirect.appendTo(new File(ROOT_DIR + File.separator + LOG_FILE)));
			
			process = pb.start();
			startTime = System.nanoTime();
		} catch (Exception e) {
			return Response.status(200).entity(false).build();
		}

		return Response.status(200).entity(true).build();
	}
	
	@GET
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

        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
            File f = new File(ROOT_DIR);
            pb.directory(f);
            pb.redirectOutput(Redirect.appendTo(new File(ROOT_DIR+File.separator+LOG_FILE)));
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
		return PakUtil.tail(new File(ROOT_DIR + File.separator + LOG_FILE), lines);
	}
	
	@GET
	@Path("/logdownload")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadLog() throws IOException {

		File log = new File(ROOT_DIR + File.separator + LOG_FILE);

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

		if (PakUtil.zipFiles(ROOT_DIR, RESULT_FILES.split("\\s*,\\s*"), RESULT_ZIP)) {
			File arhiva = new File(ROOT_DIR + File.separator + RESULT_ZIP);

			if (arhiva.isFile()) {
				ResponseBuilder response = Response.ok().entity((Object) arhiva);
				response.header("Content-Disposition", "attachment; filename=" + arhiva.getName());
				return response.build();
			}
		}
		
		return Response.status(Response.Status.NOT_FOUND).build();
	}	
}
