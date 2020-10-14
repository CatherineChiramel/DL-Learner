package org.dllearner.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

import org.apache.commons.lang.ArrayUtils;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.kb.OWLFile;
import org.dllearner.learningproblems.PosNegLP;
import org.dllearner.learningproblems.PosNegLPStandard;
import org.dllearner.learningproblems.PosOnlyLP;
import org.dllearner.reasoning.ClosedWorldReasoner;
import org.dllearner.refinementoperators.CustomStartRefinementOperator;
import org.dllearner.refinementoperators.ReasoningBasedRefinementOperator;
import org.dllearner.refinementoperators.RhoDRDown;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLIndividual;

import com.google.common.collect.Sets;

import uk.ac.manchester.cs.owl.owlapi.OWLClassExpressionImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

/**
 * This class should exemplify how to run CELOE programmatically, i.e. by
 * creating all components explicitly in Java. This example runs the same
 * experiment as the examples/father.conf does.
 */
public class CELOESongLearner {
    static File familyExamplesDir = new File("./examples");
    static String uriPrefix = "http://upb.de/Music#";

    public static void main(String[] args) throws ComponentInitException {
        /* Define the knowledge source
         * > ks.type = "OWL File"
         * > ks.fileName = "father.owl"
         *
         */

        Random random = new Random(System.currentTimeMillis());
        OWLFile ks = new OWLFile();
        ks.setFileName(familyExamplesDir.getAbsolutePath() + "/music.owl");
        ks.init();

        /* Set up the reasoner
         * > reasoner.type = "closed world reasoner"
         * > reasoner.sources = { ks }
         */
        ClosedWorldReasoner reasoner = new ClosedWorldReasoner();

        // create { ks }, i.e. a set containing ks
        Set<KnowledgeSource> sources = new HashSet<>();
        sources.add(ks);

        reasoner.setSources(sources);
        reasoner.init();

        PosNegLPStandard lp = new PosNegLPStandard(reasoner);


        HashSet<OWLIndividual> playlistSongs = new HashSet<>();
        List<OWLIndividual> negativeExamples = new ArrayList<>();
        HashSet<OWLIndividual> sampledNegExamples = new HashSet<>();
        BufferedReader csvReader = null;
        Map<String, List<OWLIndividual>> propertyMap = new HashMap<>();

        try {
            csvReader = new BufferedReader(new FileReader(familyExamplesDir.getAbsolutePath() + "/preprocessedSongData2.csv"));
            String row = csvReader.readLine();
            String[] songProperties = row.split(",");
            for(String property: songProperties) {
                propertyMap.putIfAbsent(property, new ArrayList<>());
            }
            while((row = csvReader.readLine()) != null) {
                String[] rowElements = row.split(",");
                for(String property: songProperties) {
                    int index = ArrayUtils.indexOf(songProperties, property);
                    if(! propertyMap.get(property).contains(uriPrefix + property + "_" + rowElements[index])){
                        propertyMap.get(property).add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + property + "_" + rowElements[index])));
                    }
                }
                if(rowElements[2].equals("Alternative 60s")) {
                    playlistSongs.add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + rowElements[18])));
                }
                else {
                    negativeExamples.add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + rowElements[18])));
                }
            }
            csvReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // sampling songs from the dataset for negative examples
        int i=0;
        while(i != 1000) {
            int index = random.nextInt(negativeExamples.size());
            if(!sampledNegExamples.contains(negativeExamples.get(index)) ) {
                sampledNegExamples.add(negativeExamples.get(index));
                i++;
            }

        }

        List nonProperties = new ArrayList();
        nonProperties.add("Playlist");
        nonProperties.add("speechiness");
        nonProperties.add("Song");
        nonProperties.add("song_name");
        // Adding all the idividuals other than songs to negative examples list
        for(String key: propertyMap.keySet()) {
            if(!nonProperties.contains(key)) {
                for(OWLIndividual value: propertyMap.get(key)) {
                    sampledNegExamples.add(value);
                }
            }

        }
//        for( OWLIndividual s: playlistSongs) {
//            System.out.println(s);
//        }

        lp.setPositiveExamples(playlistSongs);
        lp.setNegativeExamples(sampledNegExamples);
        lp.init();

        /* Set up the learning algorithm
         * > alg.type = "celoe"
         * > alg.maxExecutionTimeInSeconds = 1
         */
        CELOE alg = new CELOE(lp, reasoner);
        alg.setMaxExecutionTimeInSeconds(3600);

        alg.setStartClass(new OWLClassImpl(IRI.create(uriPrefix + "Song")));


        RhoDRDown op = new RhoDRDown();

        op.setUseHasValueConstructor(true);
        op.setUseDataHasValueConstructor(true);

        op.setStartClass(new OWLClassImpl(IRI.create(uriPrefix + "Song")));
        op.setReasoner(reasoner);

        op.init();
        alg.setOperator(op);


        // This 'wiring' is not part of the configuration file since it is
        // done automatically when using bin/cli. However it has to be done explicitly,
        // here.
        alg.setLearningProblem(lp);
        alg.setReasoner(reasoner);


        alg.init();

        alg.start();
    }
}
