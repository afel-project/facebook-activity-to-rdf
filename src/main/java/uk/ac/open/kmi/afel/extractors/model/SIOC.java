package uk.ac.open.kmi.afel.extractors.model;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class SIOC {

	public static final String _NS = "http://rdfs.org/sioc/ns#";

	public static final String _NS_TYPES = "http://rdfs.org/sioc/types#";

	public static final Property content = ResourceFactory.createProperty(_NS
			+ "content");

	public static final Property has_creator = ResourceFactory
			.createProperty(_NS + "has_creator");

	public static final Property id = ResourceFactory
			.createProperty(_NS + "id");

	public static final String NAMESPACE = _NS;

	public static final String NAMESPACE_TYPES = _NS_TYPES;

	public static final Resource MicroblogPost = ResourceFactory
			.createResource(_NS_TYPES + "MicroblogPost");

	public static final Resource Post = ResourceFactory.createResource(_NS
			+ "Post");

	public static final Property site = ResourceFactory.createProperty(_NS
			+ "site");

	public static final Resource UserAccount = ResourceFactory
			.createResource(_NS + "UserAccount");

}
