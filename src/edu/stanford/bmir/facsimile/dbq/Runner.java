package edu.stanford.bmir.facsimile.dbq;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import edu.stanford.bmir.facsimile.dbq.configuration.Configuration;
import edu.stanford.bmir.facsimile.dbq.generator.FormGenerator;
import edu.stanford.bmir.facsimile.dbq.question.QuestionParser;
import edu.stanford.bmir.facsimile.dbq.question.QuestionSection;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class Runner {
	private File config;
	private boolean verbose;
	private Configuration conf;
	private List<QuestionSection> questions;
	
	
	/**
	 * Constructor
	 * @param config	Configuration file
	 * @param verbose	Verbose mode
	 */
	public Runner(File config, boolean verbose) {
		this.config = config;
		this.verbose = verbose;
	}
	
	
	/**
	 * Generate form to a local file specified in the configuration file
	 * @throws IOException	IO exception
	 */
	public void generateFormToLocalFile() throws IOException {		
		String output = run();
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(conf.getOutputFilePath())));
		bw.write(output);
		bw.close();
	}
	
	
	/**
	 * Execute form generation procedure
	 * @return String containing HTML code for the form
	 * @throws IOException	IO exception
	 */
	public String run() throws IOException {
		System.out.print("Loading configuration file: " + config.getAbsolutePath() + "... ");
		if(verbose) System.out.println();
		conf = new Configuration(config, verbose);
		conf.loadConfiguration();
		System.out.println("done");

		OWLOntology ont = loadOntology(conf);
		String outputPath = conf.getOutputFilePath();
		if(outputPath != null)
			System.out.println("Output file: " + outputPath);
		
		QuestionParser gen = new QuestionParser(ont, conf, verbose);
		questions = gen.getAllSections();
		FormGenerator form = new FormGenerator(questions);
		String output = form.generateHTMLForm(conf.getOutputFileTitle(), conf.getCSSStyleClass());
		
		System.out.println("finished");
		return output;
	}
	
	
	/**
	 * Get the ordered list of question/sections 
	 * @return List of question sections
	 */
	public List<QuestionSection> getQuestionSections() {
		if(questions == null) {
			try { run(); } 
			catch (IOException e) { e.printStackTrace(); }
			return questions;
		} else
			return questions;
	}
	
	
	/**
	 * Load ontology specified in a configuration
	 * @param conf	Configuration
	 * @return OWL ontology
	 */
	private OWLOntology loadOntology(Configuration conf) {
		String inputFile = conf.getInputOntologyPath();
		if(inputFile == null) {
			System.err.println("\n!! Error: Input ontology file path not specified in configuration file !!\n");
			System.exit(1);
		}
		File f = new File(inputFile);
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
		config.setLoadAnnotationAxioms(false);
		
		System.out.print("Loading ontology: " + f.getAbsolutePath() + "... ");
		Map<IRI, String> map = conf.getInputImportsMap();
		for(IRI i : map.keySet())
			man.getIRIMappers().add(new SimpleIRIMapper(i, IRI.create("file:" + map.get(i))));
		
		OWLOntology ont = null;
		try {
			ont = man.loadOntologyFromOntologyDocument(new FileDocumentSource(f), config);
		} catch (OWLOntologyCreationException e) { e.printStackTrace(); }
		System.out.println("done");
		return ont;
	}
	
	
	/**
	 * Print usage message 
	 */
	private static void printUsage() {
		System.out.println(" Usage:\n\t-config [CONFIGURATION] [OPTIONS]");
		System.out.println();
		System.out.println("	[CONFIGURATION]	An XML configuration file input, output, entity bindings, and section/question ordering");
		System.out.println();
		System.out.println("	[OPTIONS]");
		System.out.println("	-v		verbose mode");
		System.out.println();
	}
	
	
	/**
	 * Main
	 * @param args	Configuration file path, verbose flag
	 * @throws IOException	IO exception
	 */
	public static void main(String[] args) throws IOException {
		boolean verbose = false; File file = null;
		for(int i = 0; i < args.length; i++) {
			String arg = args[i].trim();
			if(arg.equalsIgnoreCase("-config")) {
				if(++i == args.length) {
					System.err.println("\n!! Error: -config must be followed by a path to a configuration file !!\n");
					Runner.printUsage(); System.exit(1);
				}
				if(!args[i].startsWith("-"))
					file = new File(args[i].trim());
			}
			if(arg.equalsIgnoreCase("-v"))
				verbose = true;
		}
		if(file != null)
			new Runner(file, verbose).generateFormToLocalFile();
		else {
			System.err.println("\n!! Error: Could not load configuration file; the path to the configuration must follow the -config flag !!\n");
			Runner.printUsage(); System.exit(1);
		}
	}
}