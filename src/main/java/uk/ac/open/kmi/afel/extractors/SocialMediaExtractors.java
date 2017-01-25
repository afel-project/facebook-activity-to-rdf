package uk.ac.open.kmi.afel.extractors;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;

public class SocialMediaExtractors {

	private static String appId, appSecret, accessToken, afeluser;
	private static int cap = 10;

	/**
	 * Command-line parser for standalone application.
	 */
	private static class Cli {

		private String[] args = null;
		private Options options = new Options();

		public Cli(String[] args) {
			this.args = args;
			options.addOption("i", "appid", true, "OAuth AppId.");
			options.addOption("s", "appsecret", true, "OAuth AppSecret.");
			options.addOption("T", "token", true, "OAuth access token.");
			options.addOption("S", "secret", true,
					"OAuth access secret (not required for Facebook Apps?).");
			options.addOption("u", "username", true,
					"AFEL Data Platform username.");
			options.addOption("h", "help", false, "Show this help.");
		}

		/**
		 * Prints help.
		 */
		private void help() {
			String syntax = "java [java-opts] -jar [this-jarfile] [options] [extractor]";
			String footer = "The only [extractor] supported right now is \"facebook\"";
			new HelpFormatter().printHelp(syntax, "", options, footer);
			System.exit(0);
		}

		/**
		 * Parses command line arguments and acts upon them.
		 */
		public void parse() {
			CommandLineParser parser = new BasicParser();
			CommandLine cmd = null;
			try {
				cmd = parser.parse(options, args);
				String[] args = cmd.getArgs();
				if (args.length > 0) {
					if ("facebook".equals(args[0])) {
						List<String> errors = new ArrayList<String>();
						if (cmd.hasOption('i')) {
							appId = cmd.getOptionValue('i');
						} else
							errors.add("app ID (--appid, -i)");
						if (cmd.hasOption('s')) {
							appSecret = cmd.getOptionValue('s');
						} else
							errors.add("app secret (--appsecret, -s)");
						if (cmd.hasOption('T')) {
							accessToken = cmd.getOptionValue('T');
						} else
							errors.add("access token (--token, -T)");
						if (!errors.isEmpty()) {
							log.error("Missing OAuth data for Facebook authorisation:");
							for (String e : errors)
								log.error(" * " + e);
							System.exit(1);
						}
					} else {
						log.error("Unsupported extraction task " + args[0]);
						help();
					}
				} else {
					log.error("Please specify the extraction task (supported ones: facebook)");
					help();
				}
				if (cmd.hasOption('u'))
					afeluser = cmd.getOptionValue('u');
				if (cmd.hasOption('h'))
					help();
			} catch (UnrecognizedOptionException e) {
				System.err.println(e.getMessage());
				help();
			} catch (ParseException e) {
				log.error("Failed to parse comand line properties", e);
				help();

			}
		}

	}

	private static Logger log = LoggerFactory
			.getLogger(SocialMediaExtractors.class);

	/**
	 * 
	 * @param args
	 *            <ul>
	 *            <li>0: appId
	 *            <li>1: appSecret
	 *            <li>2: auth token
	 *            <li>3: query
	 *            <li>4: limit (int)
	 *            </ul>
	 */
	public static void main(String[] args) {
		new Cli(args).parse();
		// try {
		Facebook2RDF f2r = new Facebook2RDF(appId, appSecret, accessToken,
				afeluser);
		Model m = f2r.extract(cap);
		String filename = (afeluser == null ? "anonymous" : afeluser)
				+ "_facebook" + ".nt";
		OutputStream out;
		try {
			out = new FileOutputStream(filename);
			RDFDataMgr.write(out, m, RDFFormat.NTRIPLES);
			out.close();
			log.info("DONE - {} statements written.", m.size());
		} catch (FileNotFoundException e) {
			log.error("Failed to create or reuse file " + filename, e);
		} catch (IOException e) {
			log.error("Failed to write to file " + filename, e);
		}

	}

}
