package com.bagri.samples.book;

import static com.bagri.core.Constants.pn_client_dataFactory;
import static com.bagri.core.Constants.pn_client_storeMode;
import static com.bagri.core.Constants.pn_schema_address;
import static com.bagri.core.Constants.pn_schema_name;
import static com.bagri.core.Constants.pn_schema_password;
import static com.bagri.core.Constants.pn_schema_user;
import static com.bagri.core.Constants.pv_client_storeMode_insert;
import static com.bagri.support.util.FileUtils.def_encoding;
import static com.bagri.support.util.FileUtils.readTextFile;
import static com.bagri.support.util.PropUtils.getOutputProperties;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import com.bagri.client.hazelcast.impl.SchemaRepositoryImpl;
import com.bagri.core.api.DocumentManagement;
import com.bagri.core.api.ResultCursor;
import com.bagri.core.api.SchemaRepository;
import com.bagri.core.api.BagriException;
import com.bagri.core.model.Document;
import com.bagri.core.xquery.api.XQProcessor;
import com.bagri.xqj.BagriXQDataFactory;
import com.bagri.xquery.saxon.XQProcessorClient;
import com.opencsv.CSVParser;
import com.opencsv.CSVReader;

public class BookingApp {
	
	private XQProcessor proc;
	private SchemaRepository xRepo;
	

	public static void main(String[] args) throws BagriException {

        // parse args, get connection params, connect to XDM cluster
		if (args.length < 4) {
			throw new BagriException("wrong number of arguments passed. Expected: schemaAddress schemaName userName password", 0);
		}
		
		Properties props = new Properties();
	    props.setProperty(pn_schema_address, args[0]);
	    props.setProperty(pn_schema_name, args[1]);
	    props.setProperty(pn_schema_user, args[2]);
	    props.setProperty(pn_schema_password, args[3]);
	    
		BookingApp client = new BookingApp(props);
        // print success/failure msg about this 
		System.out.println(">> connected to XDM Schema");

		StringBuffer query = null;
		boolean querying = false;
        String prompt = "> ";
        //String rightPrompt = null;
        try {
            TerminalBuilder builder = TerminalBuilder.builder();
            Completer completer = null;

            //int index = 0;
            //while (args.length > index) {
            //    switch (args[index]) {
            //        case "files":
            //            completer = new FileNameCompleter();
            //            break label;
            //        case "simple":
            //            completer = new StringsCompleter("foo", "bar", "baz");
            //            break label;
            //    }
            //}

            Terminal terminal = builder.build();
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(completer)
                    .build();

    		usage(terminal);
            
            while (true) {
                String line = null;
                try {
                    line = reader.readLine(prompt);
                } catch (UserInterruptException e) {
                    // Ignore
                } catch (EndOfFileException e) {
                    return;
                }
                if (line == null) {
                    continue;
                }

                line = line.trim();
                if (line.length() == 0) {
                	if (querying) {
                		querying = false;
                    	long stamp = System.currentTimeMillis();
                		List<String> result = client.runQuery(query.toString());
                    	stamp = System.currentTimeMillis() - stamp;
                    	terminal.writer().println("> query result: \n");
                    	for (String res: result) {
                    		terminal.writer().println(res);
                    	}
                    	int cnt = result.size(); 
                    	terminal.writer().println("> got " + cnt + " results; time taken: " + stamp + " ms");
                    	terminal.flush();
                    	query = null;
                	}
                	continue;
                }

                if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                    break;
                }
                
                ParsedLine pl = reader.getParser().parse(line, 0);
                if (querying) {
                	query.append('\n');
                	query.append(line);
                } else if ("load".equals(pl.word())) {
                    if (pl.words().size() == 2 || pl.words().size() == 3 || pl.words().size() == 4) {
                    	String[] splits = line.split("\\s");
                    	String fName = splits[1]; 
                    	//String fName = pl.words().get(1);
                    	int threadCount = 1;
                    	String encoding = null;
                    	if (pl.words().size() > 2) {
                    		try {
                    			threadCount = Integer.parseInt(pl.words().get(2));
                    		} catch (NumberFormatException ex) {
                    			//
                    			encoding = pl.words().get(2);
                    		}
                    	}
                    	if (pl.words().size() == 4) {
                			encoding = pl.words().get(3);
                    	}                    	
                    	long stamp = System.currentTimeMillis();
                    	long counts = client.loadFiles(fName, encoding, threadCount);
                    	int err = (int) counts;
                    	int cnt = (int) (counts >> 32);
                    	stamp = System.currentTimeMillis() - stamp;
                    	terminal.writer().println("> loaded " + cnt + " documents; not loaded: " + err + "; time taken: " + stamp + " ms");
                    } else {
                    	terminal.writer().println("> load command expects one, two ot three parameters: fileName (mandatory), threadCount (optional) and encoding (optional)");
                    }
                	terminal.flush();
                } else if ("get".equals(pl.word())) {
                    if (pl.words().size() == 2) {
                    	String uri = pl.words().get(1);
                    	String content = client.getDocument(uri);
                    	if (content != null) {
                    		terminal.writer().println("> " + content);
                    	} else {
                    		terminal.writer().println("> no document found for uri " + uri);
                    	}
                    } else {
                    	terminal.writer().println("> get command expects only one parameter: the document's uri");
                    }
                	terminal.flush();
                } else if ("query".equals(pl.word())) {
                	query = new StringBuffer(line);
                	query.delete(0, 6);
                	querying = true;
                	// todo: turn off history
                } else if ("remove".equals(pl.word())) {
                    if (pl.words().size() == 2) {
                    	String uri = pl.words().get(1);
                    	if (!client.removeDocument(uri)) {
                    		terminal.writer().println("> no document found for uri " + uri);
                    	} else {
                    		terminal.writer().println("> the document " + uri + " was removed");
                    	}
                    } else {
                    	terminal.writer().println("> remove command expects only one parameter: the document's uri");
                    }
                	terminal.flush();
                } else if ("cls".equals(pl.word())) {
                    terminal.puts(Capability.clear_screen);
                    terminal.flush();
                } else if ("help".equals(pl.word())) {
                	usage(terminal);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            client.close();
            System.out.println(">> connection to XDM Schema closed");
        }
		
	}

    private static void usage(Terminal terminal) {
    	terminal.writer().println("CLI commands: command_name <mandatory_param> [optional_param] - description");
    	terminal.writer().println("  get <document uri> - to print document content");
    	terminal.writer().println("  load <file_name> [thread_count] [encoding] - to load document(-s) from file");
    	terminal.writer().println("  remove <document uri> - to remove document from schema");
    	terminal.writer().println("  query - to perform XQuery on loaded documents");
    	terminal.writer().println("     <query code>");
    	terminal.writer().println("  CRLF");
    	terminal.writer().println("  quit - to quit the CLI");
    	terminal.writer().println("  help - to see this usage info");
    	terminal.flush();
    }

	public BookingApp(Properties props) {
		proc = new XQProcessorClient();
		BagriXQDataFactory xqFactory 	= new BagriXQDataFactory();
		xqFactory.setProcessor(proc);
		props.put(pn_client_dataFactory,  xqFactory);
		xRepo = new SchemaRepositoryImpl(props);
	}
	
	public BookingApp(SchemaRepository xRepo) {
		this.xRepo = xRepo;
	}
	
	public void close() { 
		xRepo.close();
	}

	public long loadFiles(String fileName, String encoding, int size) throws IOException, BagriException {
		Path path = Paths.get(fileName);
		if (Files.isDirectory(path)) {
			return processFolder(path, encoding, size);
		}
		return processFile(path, encoding, size);
	}
	
	private long processFolder(Path folder, String encoding, int size) throws IOException, BagriException {
		long result = 0;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
		    for (Path path: stream) {
		        if (Files.isDirectory(path)) {
		            result += processFolder(path, encoding, size);
		        } else {
		            result += processFile(path, encoding, size);
		        }
		    }
		    return result;
		} 
	}
	
	private long processFile(Path path, String encoding, int size) throws IOException, BagriException {
		String fn = path.getFileName().toString();
		int pos = fn.lastIndexOf('.');
		final String name = fn.substring(0, pos) + "_";
		final String ext = fn.substring(pos);
		CSVReader reader = null;
		DocumentManagement dMgr = xRepo.getDocumentManagement();
		Properties props = new Properties();
		props.setProperty(pn_client_storeMode, pv_client_storeMode_insert);
		if (encoding == null) {
			encoding = def_encoding;
		}
		String fileName = path.toString();
		if (".csv".equals(ext)) {
			reader = new CSVReader(new InputStreamReader(new FileInputStream(fileName), encoding));
		} else if (".tsv".equals(ext)) {
			CSVParser parser = new CSVParser('\t', '\0', '\0');
			reader = new CSVReader(new InputStreamReader(new FileInputStream(fileName), encoding), 0, parser); 
		} else {
			String content = readTextFile(fileName, encoding);
			Document doc = dMgr.storeDocumentFromString(fn, content, props);
			return 1;
		}

		final String[] header = reader.readNext();
		String[] line;
		int idx = 0;
		int err = 0;
		
		System.out.println("going to process file: " + fileName);
		if (size > 1) {
			ExecutorService exec = Executors.newFixedThreadPool(size);
			while ((line = reader.readNext()) != null) {
	        	String uri = name + idx + ext;
				Runnable worker = new DocumentStoreThread(idx, header, line, uri, props);
				exec.execute(worker);
				idx++;
			}
			exec.shutdown();
			while (!exec.isTerminated()) {
				//?
			}
		} else {
			while ((line = reader.readNext()) != null) {
	        	String uri = name + idx + ext;
				if (storeDocument(idx + err, header, line, uri, props)) {
		        	idx++;
		        } else {
		        	err++;
		        }
		    }
		}
		
		reader.close();
		long result = (((long) idx) << 32) | (err & 0xffffffffL);
		System.out.println("file " + fileName + " processed; added records: " + idx + ", skipped records: " + err);
		return result;
	}
	
	private boolean storeDocument(int idx, String[] header, String[] data, String uri, Properties props) { //throws XDMException {
        Map<String, Object> map = line2Map(header, data);
        if (map != null) {
        	try {
        		Document doc = xRepo.getDocumentManagement().storeDocumentFromMap(uri, map, props);
        		return doc != null;
        	} catch (BagriException ex) {
        		System.out.println("failed map is: " + map + ". record idx is: " + (idx + 2));
        		return false;
        	}
        }
		System.out.println("was not able convert record to map. record idx is: " + (idx + 2));
        return false;
	}
	
	private Map<String, Object> line2Map(String[] keys, String[] values) {
		if (keys.length != values.length) {
			// throw ex?
			System.out.println("lines do not match! header length: " + keys.length + ", record length: " + values.length);
			System.out.println("keys: " + Arrays.toString(keys));
			System.out.println("values: " + Arrays.toString(values));
			//if (keys.length > values.length) {
			//	System.out.println(keys[values.length - 1] + ": " + values[values.length - 1]);
				return null;
			//}
		}

		HashMap<String, Object> result = new HashMap<>(keys.length);
		for (int i=0; i < keys.length; i++) {
			String key = keys[i].trim();
			if (key.length() > 0) {
				String value = values[i];
				// do it in the right way..
				value = value.replaceAll("[\\u0001\\u0003\\u0005\\u0008\\u000b\\u0010\\u0011\\u0014\\u0019\\u001a\\u001b\\u001c\\u001d\\u001e\\u001f]", "");
				result.put(key, value);
			}
		}
		return result;
	}
	
	public String getDocument(String uri) throws BagriException {
		return xRepo.getDocumentManagement().getDocumentAsString(uri, null);
	}
	
	public List<String> runQuery(String query) throws Exception {
		Properties props = new Properties();
		List<String> result = new ArrayList<String>();
		Properties outProps = getOutputProperties(props);
		try (ResultCursor cursor = xRepo.getQueryManagement().executeQuery(query, null, props)) {
			while (cursor.next()) {
				result.add(proc.convertToString(cursor.getXQItem(), outProps));
			}
		}
		return result;
	}
	
	public boolean removeDocument(String uri) {
		try {
			xRepo.getDocumentManagement().removeDocument(uri);
			return true;
		} catch (BagriException ex) {
			return false;
		}
	}
	

	private class DocumentStoreThread implements Runnable {

		private int idx;
		private String[] data;
		private String[] header;
		private String uri;
		private Properties props;
		
		DocumentStoreThread(int idx, String[] header, String[] data, String uri, Properties props) {
			this.idx = idx;
			this.header = header;
			this.data = data;
			this.uri = uri;
			this.props = props;
		}

		@Override
		public void run() {
			storeDocument(idx, header, data, uri, props);
		}
		
	}
	
}


