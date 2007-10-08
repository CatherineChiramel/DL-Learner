/**
 * Copyright (C) 2007, Jens Lehmann
 *
 * This file is part of DL-Learner.
 * 
 * DL-Learner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * DL-Learner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.dllearner.kb;

import java.io.File;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.dllearner.core.ConfigEntry;
import org.dllearner.core.ConfigOption;
import org.dllearner.core.InvalidConfigOptionValueException;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.core.StringConfigOption;
import org.dllearner.core.StringSetConfigOption;
import org.dllearner.core.dl.AtomicConcept;
import org.dllearner.core.dl.Individual;
import org.dllearner.reasoning.JenaOWLDIGConverter;
import org.dllearner.reasoning.OWLAPIDIGConverter;
import org.dllearner.utilities.Datastructures;

/**
 * Represents a SPARQL Endpoint. 
 * TODO: move org.dllearner.modules.sparql to this package and
 * integrate its classes
 * TODO: Is it necessary to create a class DBpediaSparqlEndpoint?
 * 
 * @author Jens Lehmann
 *
 */
public class SparqlEndpoint extends KnowledgeSource {

	private URL url;
	private Set<String> instances;
	private URL ntFile;
	private int numberOfRecursions;

	public static String getName() {
		return "SPARQL Endpoint";
	}

	public static Collection<ConfigOption<?>> createConfigOptions() {
		Collection<ConfigOption<?>> options = new LinkedList<ConfigOption<?>>();
		options.add(new StringConfigOption("url", "URL of SPARQL Endpoint"));
		options.add(new StringSetConfigOption("instances","relevant instances e.g. positive and negative examples in a learning problem"));
		options.add(new StringConfigOption("numberOfRecursions","number of Recursions, the Sparql-Endpoint is asked"));
		return options;
	}

	/*
	 * @see org.dllearner.core.Component#applyConfigEntry(org.dllearner.core.ConfigEntry)
	 */
	@Override
	@SuppressWarnings({"unchecked"})
	public <T> void applyConfigEntry(ConfigEntry<T> entry) throws InvalidConfigOptionValueException {
		String option = entry.getOptionName();
		if (option.equals("url")) {
			String s = (String) entry.getValue();
			try {
				url = new URL(s);
			} catch (MalformedURLException e) {
				throw new InvalidConfigOptionValueException(entry.getOption(), entry.getValue(),"malformed URL " + s);
			} //catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			//}
		} else if(option.equals("instances")) {
			instances = (Set<String>) entry.getValue();
		} else if(option.equals("numberOfRecursions")){
			this.numberOfRecursions=Integer.parseInt((String)entry.getValue());
		}
	}

	/* (non-Javadoc)
	 * @see org.dllearner.core.Component#init()
	 */
	@Override
	public void init() {
		// TODO add code for downloading data from SPARQL endpoint
		String filename=System.currentTimeMillis()+".nt";
		
		Set<String> predList=new HashSet<String>();
		Set<String> objList=new HashSet<String>();
		Set<String> classList=new HashSet<String>();
		String prefix="";
		int filterMode=0;
		
		Individual ind1=new Individual("http://dbpedia.org/resource/Pythagoras");
		Individual ind2=new Individual("http://dbpedia.org/resource/Socrates");
		
		try{
			FileWriter fw=new FileWriter(new File("examples/"+filename),true);
			System.out.println("SparqlModul: Collecting Ontology");
			String[] a=new String[0];
			OntologyCollector oc=new OntologyCollector(instances.toArray(a), numberOfRecursions,
					 filterMode,  Datastructures.setToArray(predList),Datastructures.setToArray( objList),Datastructures.setToArray(classList));
			
			String ont=oc.collectOntology();
			fw.write(ont);
			fw.flush();
						
			System.out.println("SparqlModul: ****Finished");
						
			fw.close();
			this.ntFile=(new File("examples/"+filename)).toURI().toURL();
			//System.exit(0);
			}catch (Exception e) {e.printStackTrace();}	
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dllearner.core.KnowledgeSource#toDIG()
	 */
	@Override
	public String toDIG(URI kbURI) {
		return JenaOWLDIGConverter.getTellsString(ntFile, OntologyFileFormat.N_TRIPLES, kbURI);
	}

	public URL getURL() {
		return url;
	}
}
