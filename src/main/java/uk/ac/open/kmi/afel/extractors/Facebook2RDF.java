package uk.ac.open.kmi.afel.extractors;

import static uk.ac.open.kmi.afel.extractors.model.Prefixes.AFEL_RESOURCE;
import static uk.ac.open.kmi.afel.extractors.model.Prefixes.FACEBOOK_ACCOUNT;
import static uk.ac.open.kmi.afel.extractors.model.Prefixes.FACEBOOK_POST;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.open.kmi.afel.extractors.model.SIOC;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.XSD;

import facebook4j.Activity;
import facebook4j.Book;
import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.Group;
import facebook4j.Like;
import facebook4j.Post;
import facebook4j.Reaction;
import facebook4j.User;
import facebook4j.conf.ConfigurationBuilder;

public class Facebook2RDF {

	private Set<String> categoriesOfInterest = new HashSet<String>();
	private String afelUsername;

	private Facebook facebook;
	private Logger log = LoggerFactory.getLogger(Facebook2RDF.class);

	public Facebook2RDF(String consumerKey, String consumerSecret,
			String accessToken) {
		this(consumerKey, consumerSecret, accessToken, null);
	}

	public Facebook2RDF(String consumerKey, String consumerSecret,
			String accessToken, String username) {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthAppId(consumerKey)
				.setOAuthAppSecret(consumerSecret)
				.setOAuthAccessToken(accessToken)
		// .setOAuthAccessTokenSecret(accessSecret)
		;
		try {
			facebook = new FacebookFactory(cb.build()).getInstance();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		this.afelUsername = username;
		populateCategoriesofInterest();
	}

	public Model extract(int limit) {
		Model m = ModelFactory.createDefaultModel();
		Calendar cal = Calendar.getInstance();
		User iam;
		try {
			iam = facebook.getMe();
			log.info("Extracting data for Facebook user named \"{}\" (id={})",
					iam.getName(), iam.getId());
		} catch (FacebookException e) {
			log.error("Could not even retrieve who I am! Cannot go on.");
			throw new RuntimeException(e);
		}

		Resource rMe = m.createResource(FACEBOOK_ACCOUNT + iam.getId());
		rMe.addProperty(RDF.type, SIOC.UserAccount);
		rMe.addProperty(SIOC.id,
				m.createTypedLiteral(iam.getId(), XSD.ID.getURI()));
		rMe.addProperty(SIOC.site, m.createResource("http://facebook.com"));
		// Link to AFEL username if existing XXX should we do this?
		if (afelUsername != null) {
			try {
				Resource rUser = m.createResource(AFEL_RESOURCE + "user/"
						+ urify(afelUsername));
				rUser.addProperty(RDF.type, m.createResource(FOAF.Agent));
				rUser.addProperty(m.createProperty(FOAF.NS + "account"), rMe);
			} catch (EncoderException ex) {
				log.error("Failed to urify AFEL username '{}' - skipping.",
						afelUsername);
			}
		}
		// User likes
		try {
			for (Like item : facebook.getUserLikes()) {
				// Not filtering categories right now
				// if (!categoriesOfInterest.contains(item.getCategory())) {
				try {
					Resource rTopic = m.createResource(AFEL_RESOURCE + "topic/"
							+ urify(item.getName()));
					rTopic.addProperty(FOAF.name,
							m.createLiteral(item.getName()));
					rTopic.addProperty(
							FOAF.page,
							m.createResource("https://facebook.com/"
									+ item.getId()));
					rMe.addProperty(FOAF.topic_interest, rTopic);
				} catch (EncoderException ex) {
					log.error(
							"Failed to urify Facebook topic '{}' - skipping.",
							item.getName());
				}
			}
		} catch (FacebookException e) {
			log.error("Could not get likes list. Facebook4J reported:", e);
		}
		// Old-fashioned flat string interests
		for (String item : iam.getInterestedIn()) {
			try {
				Resource rTopic = m.createResource(AFEL_RESOURCE + "topic/"
						+ urify(item));
				rTopic.addProperty(FOAF.name, m.createLiteral(item));
				rMe.addProperty(FOAF.topic_interest, rTopic);
			} catch (EncoderException ex) {
				log.error("Failed to urify Facebook topic '{}' - skipping.",
						item);
			}
		}
		// Group memberships
		try {
			// Not much to do as it requires FB approval
			for (Group item : facebook.getGroups()) {
				log.info(" * in group '{}'", item.getName());
			}
		} catch (FacebookException e) {
			log.error("Could not get book list. Facebook4J reported:", e);
		}
		// Books read
		try {
			for (Book item : facebook.getBooks()) {
				try {
					Resource rBook = m.createResource(AFEL_RESOURCE
							+ "resource/book/facebook_" + urify(item.getId()));
					rBook.addProperty(DCTerms.title,
							m.createLiteral(item.getName()));
					rBook.addProperty(
							FOAF.page,
							m.createResource("https://facebook.com/"
									+ item.getId()));
					rMe.addProperty(
							m.createProperty("http://vocab.afel-project.eu/activities/consumed"),
							rBook);
				} catch (EncoderException ex) {
					log.error(
							"Failed to urify Facebook book ID '{}' - skipping.",
							item.getId());
				}
			}
		} catch (FacebookException e) {
			log.error("Could not list books read. Facebook4J reported:", e);
		}
		// Activities (not sure where they come from, but they are there)
		try {
			for (Activity item : facebook.getActivities()) {
				try {
					Resource rAct = m.createResource(AFEL_RESOURCE
							+ "resource/activity/facebook_"
							+ urify(item.getName()));
					rAct.addProperty(DCTerms.title,
							m.createLiteral(item.getName()));
					rAct.addProperty(
							FOAF.page,
							m.createResource("https://facebook.com/"
									+ item.getId()));
					rMe.addProperty(
							m.createProperty("http://vocab.afel-project.eu/activities/performing"),
							rAct);
				} catch (EncoderException ex) {
					log.error(
							"Failed to urify Facebook Activity name '{}' - skipping.",
							item.getName());
				}
			}
		} catch (FacebookException e) {
			log.error("Could not list books read. Facebook4J reported:", e);
		}
		// Own posts and a few stats about them
		try {
			for (Post item : facebook.getPosts()) {
				Resource rPost = m.createResource(FACEBOOK_POST + item.getId());
				rPost.addProperty(RDF.type, SIOC.Post);
				rPost.addProperty(RDF.type, SIOC.MicroblogPost);
				if (item.getType() != null && !item.getType().isEmpty())
					rPost.addProperty(
							RDF.type,
							m.createResource("http://vocab.afel-project.eu/facebook/post_type/"
									+ item.getType()));
				rPost.addProperty(SIOC.has_creator, rMe);
				rPost.addProperty(SIOC.site,
						m.createResource("http://facebook.com"));
				cal.setTime(item.getCreatedTime());
				rPost.addProperty(DCTerms.created, m.createTypedLiteral(cal));
				if (item.getMessage() != null && !item.getMessage().isEmpty())
					rPost.addProperty(SIOC.content,
							m.createTypedLiteral(item.getMessage()));
				if (item.getDescription() != null
						&& !item.getDescription().isEmpty())
					rPost.addProperty(DCTerms.description,
							m.createLiteral(item.getDescription()));
				if (item.getName() != null && !item.getName().isEmpty())
					rPost.addProperty(DCTerms.title,
							m.createLiteral(item.getName()));

				// Post reactions - not coming through! Require FB approval?
				for (Reaction r : item.getReactions()) {
					switch (r.getType()) {
					case ANGRY:
					case SAD:
						System.out.println(r.getName());
						break;
					case LIKE:
					case LOVE:
					case THANKFUL:
					case WOW:
						System.out.println(r.getName());
						break;
					default:
					}
				}
				for (Like r : item.getLikes()) {
					Resource rThem = m.createResource(FACEBOOK_ACCOUNT
							+ r.getId());
					// XXX Can we pick the names of the "likers"?
					// if (r.getName() != null && !r.getName().isEmpty())
					// rThem.addProperty(FOAF.name,
					// m.createLiteral(r.getName()));
					rThem.addProperty(RDF.type, SIOC.UserAccount);
					rThem.addProperty(SIOC.id,
							m.createTypedLiteral(r.getId(), XSD.ID.getURI()));
					rThem.addProperty(SIOC.site,
							m.createResource("http://facebook.com"));
					rPost.addProperty(
							m.createProperty("http://vocab.afel-project.eu/social/upvotedBy"),
							rThem);
				}

				// System.out.println("I have posted the " +
				// item.getStatusType()
				// + " '" + item.getName() + "' ("
				// + item.getComments().size() + " comments, "
				// + item.getReactions().size() + " reactions of which "
				// + item.getLikes().size() + " likes)");

			}
		} catch (FacebookException e) {
			log.error("Could not list posts. Facebook4J reported:", e);
		}

		return m;
	}

	protected void populateCategoriesofInterest() {
		categoriesOfInterest.add("Arts & Entertainment");
		categoriesOfInterest.add("Book");
		categoriesOfInterest.add("Field of Study");
		categoriesOfInterest.add("Interest");
	}

	private static URLCodec encoder = new URLCodec();

	private static String urify(String s) throws EncoderException {
		return encoder.encode(s.toLowerCase().replaceAll(" ", "_")
				.replaceAll(",", ""));
	}

}
