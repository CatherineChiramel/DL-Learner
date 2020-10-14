package org.dllearner.examples;

import org.dllearner.core.ComponentInitException;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.kb.OWLFile;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class QueryKG {
    static File familyExamplesDir = new File("./examples");
    static String uriPrefix = "http://upb.de/Music#";
    public static void main(String[] args) throws ComponentInitException {
        OWLFile ks = new OWLFile();
        ks.setFileName(familyExamplesDir.getAbsolutePath() + "/music.owl");
        ks.init();
        Set<KnowledgeSource> sources = new HashSet<>();
        sources.add(ks);
        SPARQLReasoner sparqlReasoner = new SPARQLReasoner();
        sparqlReasoner.setSources(sources);
        sparqlReasoner.init();
        OWLClassExpression description = null;
        sparqlReasoner.getIndividuals(description);
    }
}
