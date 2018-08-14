package rs.mivanovic.pakrunner;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.*;

public class PakUtil {

	public static final String CONFIG_FILE = "config.properties";
	
	/*
	 * Zipuje fajlove 'files' koji se salju kao niz u arhivu 'archiveName'. Vraca da
	 * li je uspelo ili nije.
	 */
	public static boolean zipFiles(String rootDirectory, String[] files, String archiveName) throws IOException {

		List<String> srcFiles = Arrays.asList(files);
		FileOutputStream fos = new FileOutputStream(rootDirectory + File.separator + archiveName);
		ZipOutputStream zipOut = new ZipOutputStream(fos);

		try {
			for (String srcFile : srcFiles) {
				File fileToZip = new File(rootDirectory + File.separator + srcFile);
				FileInputStream fis = new FileInputStream(fileToZip);
				ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
				zipOut.putNextEntry(zipEntry);

				byte[] bytes = new byte[1024];
				int length;
				while ((length = fis.read(bytes)) >= 0) {
					zipOut.write(bytes, 0, length);
				}
				fis.close();
			}
			zipOut.close();
			fos.close();
		} catch (FileNotFoundException e) {
			return false;
		}

		return true;
	}

	/*
	 * Vraca poslednjih 'lines' linija fajla
	 */
	public static String[] tail(File file, int lines) {
		
		RandomAccessFile fileHandler = null;
		try {
			fileHandler = new java.io.RandomAccessFile(file, "r");
			long fileLength = fileHandler.length() - 1;
			StringBuilder sb = new StringBuilder();
			int line = 0;

			for (long filePointer = fileLength; filePointer != -1; filePointer--) {
				fileHandler.seek(filePointer);
				int readByte = fileHandler.readByte();

				if (readByte == '\n' && filePointer < fileLength) {
					line = line + 1;
				}

				if (line >= lines) {
					break;
				}
				if (readByte != '\r') {
					sb.append((char) readByte);
				}
			}

			String lastLine = sb.reverse().toString();
			return lastLine.split("\n");
		} catch (java.io.FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (java.io.IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (fileHandler != null) {
				try {
					fileHandler.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/*
	 * Uzima resurse iz config.properties fajla
	 */
	public static String getResource(String key) {
		Properties prop = new Properties();
		InputStream input = null;
		String value = "";
		try {
			input = PakUtil.class.getClassLoader().getResourceAsStream(CONFIG_FILE);

			if (input == null) {
				return "File Not Found";
			}
			prop.load(input);
			value = prop.getProperty(key, "NotFound");

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return value;
	}
	
	/**
	 * Rekurzivno setuje executable na celom direktorijumu
	 * @param directory
	 * @throws IOException
	 */
	public static void directorySetExecutable(String directory) throws IOException {
		
		// Pokupi sve regularne fajlove iz direktorijuma
		List<File> filesInFolder = Files.walk(Paths.get(directory))
                .filter(Files::isRegularFile)
                .map(java.nio.file.Path::toFile)
                .collect(Collectors.toList());			
		
		// Setuj executable dozvole
		for (File f : filesInFolder)
			f.setExecutable(true);
	}

}
