package rs.mivanovic.pakrunner;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.*;

public class PakUtil {

	public static final String CONFIG_FILE = "config.properties";
	private static final int BUFFER_SIZE = 4096;

	/**
	 * Setuje UNIX dozvole na 755
	 * 
	 * @param filePath
	 * @throws java.io.IOException
	 */
	public static void setPermissions(String filePath) throws IOException {

		Set<PosixFilePermission> perms = new HashSet<>();
		perms.add(PosixFilePermission.OWNER_READ);
		perms.add(PosixFilePermission.OWNER_WRITE);
		perms.add(PosixFilePermission.OWNER_EXECUTE);
		perms.add(PosixFilePermission.GROUP_READ);
		perms.add(PosixFilePermission.GROUP_EXECUTE);
		perms.add(PosixFilePermission.OTHERS_READ);
		perms.add(PosixFilePermission.OTHERS_EXECUTE);

		Files.setPosixFilePermissions(Paths.get(filePath), perms);
	}

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

	/**
	 * Raspakuje celu arhivu u direktorijum
	 * 
	 * @param zipFile
	 * @param outputFolder
	 * @return
	 */
	public static boolean unzip(String zipFile, String outputFolder) {

		try {
			File folder = new File(outputFolder);
			if (!folder.exists()) {
				folder.mkdir();
			}

			ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile));
			ZipEntry entry = zipIn.getNextEntry();
			// iterates over entries in the zip file
			while (entry != null) {
				String filePath = outputFolder + File.separator + entry.getName();
				if (!entry.isDirectory()) {
					// if the entry is a file, extracts it
					extractFile(zipIn, filePath);
					// Setuj dozvole
					setPermissions(filePath);
				} else {
					// if the entry is a directory, make the directory
					File dir = new File(filePath);
					dir.mkdir();
				}
				zipIn.closeEntry();
				entry = zipIn.getNextEntry();
			}
			zipIn.close();

			return true;
		} catch (IOException ex) {
			return false;
		}
	}

	/**
	 * Extracts a zip entry (file entry)
	 *
	 * @param zipIn
	 * @param filePath
	 * @throws IOException
	 */
	private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int read = 0;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();
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
	 * 
	 * @param directory
	 * @throws IOException
	 */
	public static void directorySetExecutable(String directory) throws IOException {

		// Pokupi sve regularne fajlove iz direktorijuma
		List<File> filesInFolder = Files.walk(Paths.get(directory)).filter(Files::isRegularFile)
				.map(java.nio.file.Path::toFile).collect(Collectors.toList());

		// Setuj executable dozvole
		for (File f : filesInFolder)
			f.setExecutable(true);
	}

	/**
	 * 
	 * @param uploadedInputStream
	 * @param uploadedFileLocation
	 */
	public static void saveToFile(InputStream uploadedInputStream, String uploadedFileLocation) throws IOException {

		OutputStream out = null;
		int read = 0;
		byte[] bytes = new byte[BUFFER_SIZE];

		out = new FileOutputStream(new File(uploadedFileLocation));
		while ((read = uploadedInputStream.read(bytes)) != -1) {
			out.write(bytes, 0, read);
		}

		out.flush();
		out.close();
	}

	/**
	 * Nerekurzivno kopiranje direktorijuma
	 * @param sourceLocation
	 * @param targetLocation
	 * @throws IOException
	 */
	public static void copyDirectoryNoRecursive(File sourceLocation, File targetLocation) throws IOException {

		if (sourceLocation.isDirectory()) {

			if (!targetLocation.exists()) 
				targetLocation.mkdir();

			// Uzima samo fajlove bez direktorijuma
			String[] children = sourceLocation.list(new FilenameFilter() {
			    @Override
			    public boolean accept(File dir, String name) {
			        return (new File(dir, name)).isFile();
			    }});

			for (int i = 0; i < children.length; i++)
				copyDirectoryNoRecursive(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
		
		// Klasicno kopiranje fajla
		} else {

			InputStream in = new FileInputStream(sourceLocation);
			OutputStream out = new FileOutputStream(targetLocation);

			// Copy the bits from instream to outstream
			byte[] buf = new byte[BUFFER_SIZE];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		}
	}

}
